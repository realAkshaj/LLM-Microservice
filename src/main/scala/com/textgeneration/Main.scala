package com.textgeneration

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import spray.json.DefaultJsonProtocol
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.utils.Options
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

object Main extends SprayJsonSupport with DefaultJsonProtocol {
  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  final case class Query(text: String)
  final case class ConversationResponse(lambdaResponse: String, ollamaResponse: String, nextQuery: String)
  final case class LambdaRequest(query: String)
  final case class LambdaResponse(response: String, model: String)
  final case class Config(
                           ollamaHost: String = "http://localhost:11434",
                           ollamaModel: String = "llama3.2:1b",
                           requestTimeout: Int = 500,
                           lambdaUrl: String = "https://lthllcut0i.execute-api.us-east-1.amazonaws.com/generate",
                           maxTurns: Int = 5
                         )

  implicit val queryFormat: RootJsonFormat[Query] = jsonFormat1(Query)
  implicit val conversationResponseFormat: RootJsonFormat[ConversationResponse] = jsonFormat3(ConversationResponse)
  implicit val lambdaRequestFormat: RootJsonFormat[LambdaRequest] = jsonFormat1(LambdaRequest)
  implicit val lambdaResponseFormat: RootJsonFormat[LambdaResponse] = jsonFormat2(LambdaResponse)

  private def callLambda(query: Query, config: Config, httpClient: org.apache.http.impl.client.CloseableHttpClient)(implicit ec: ExecutionContext): Future[String] = Future {
    logger.info(s"Sending query to Lambda: ${query.text}")
    val post = new HttpPost(config.lambdaUrl)
    post.setHeader("Content-Type", "application/json")
    post.setEntity(new StringEntity(LambdaRequest(query.text).toJson.compactPrint))

    val response = httpClient.execute(post)
    try {
      val responseBody = EntityUtils.toString(response.getEntity)
      if (response.getStatusLine.getStatusCode != 200) {
        throw new Exception(s"Lambda error: $responseBody")
      }
      val lambdaResponse = responseBody.parseJson.convertTo[LambdaResponse].response
      if (lambdaResponse.trim.isEmpty) "I need more context to provide a meaningful response." else lambdaResponse
    } finally {
      response.close()
    }
  }

  private def callOllama(context: String, config: Config, ollamaAPI: OllamaAPI)(implicit ec: ExecutionContext): Future[String] = Future {
    val ollamaQuery = s"""Given this conversation context: "$context", provide a thoughtful response that continues the discussion."""
    val options = new Options(new java.util.HashMap[String, Object]() {
      put("num_predict", Integer.valueOf(150))
      put("temperature", java.lang.Float.valueOf(0.7f))
    })

    val response = ollamaAPI.generate(config.ollamaModel, ollamaQuery, false, options).getResponse.trim
    if (response.isEmpty) "Could you provide more details about what you'd like to discuss?" else response
  }

  def processConversation(initialQuery: Query, config: Config, ollamaAPI: OllamaAPI, httpClient: org.apache.http.impl.client.CloseableHttpClient)(implicit ec: ExecutionContext): Future[List[ConversationResponse]] = {
    ConversationLogger.resetConversation()

    def loop(currentQuery: String, turnNumber: Int, acc: List[ConversationResponse], context: String): Future[List[ConversationResponse]] = {
      if (turnNumber >= config.maxTurns) {
        Future.successful(acc.reverse)
      } else {
        for {
          lambdaResponse <- callLambda(Query(currentQuery), config, httpClient)
          ollamaResponse <- callOllama(s"$context $lambdaResponse", config, ollamaAPI)
          nextResponses <- {
            val updatedContext = s"$context $lambdaResponse $ollamaResponse"
            val nextQuery = s"Regarding our discussion about $currentQuery: $ollamaResponse"
            ConversationLogger.logConversation(currentQuery, lambdaResponse, ollamaResponse)
            loop(nextQuery, turnNumber + 1, ConversationResponse(lambdaResponse, ollamaResponse, nextQuery) :: acc, updatedContext)
          }
        } yield nextResponses
      }
    }

    loop(initialQuery.text, 0, List.empty, initialQuery.text)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem(Behaviors.empty, "ConversationSystem")
    implicit val executionContext = system.executionContext

    val config = Config()
    val ollamaAPI = new OllamaAPI(config.ollamaHost)
    ollamaAPI.setRequestTimeoutSeconds(config.requestTimeout)
    val httpClient = HttpClients.createDefault()

    val route = cors(CorsSettings.defaultSettings) {
      path("conversation") {
        post {
          entity(as[Query]) { query =>
            onComplete(processConversation(query, config, ollamaAPI, httpClient)) {
              case Success(responses) => complete(HttpEntity(ContentTypes.`application/json`, responses.toJson.prettyPrint))
              case Failure(ex) =>
                logger.error(s"Conversation error: ${ex.getMessage}", ex)
                complete(StatusCodes.InternalServerError, JsObject("error" -> JsString(ex.getMessage)).prettyPrint)
            }
          }
        }
      }
    }

    val serverBinding = Http().newServerAt("localhost", 8080).bind(route)
    logger.info("Server online at http://localhost:8080/")
    logger.info("Press RETURN to stop...")

    scala.io.StdIn.readLine()

    serverBinding
      .flatMap(_.unbind())
      .onComplete(_ => {
        httpClient.close()
        system.terminate()
      })

    try {
      Await.result(system.whenTerminated, 30.seconds)
    } catch {
      case ex: Exception =>
        logger.error("Shutdown error", ex)
        system.terminate()
    }
  }
}