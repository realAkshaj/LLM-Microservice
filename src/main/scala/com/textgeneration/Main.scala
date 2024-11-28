package com.textgeneration

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.headers.HttpOriginRange
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.settings.ServerSettings.timeoutsShortcut
import akka.http.scaladsl.settings.{RoutingSettings, ServerSettings}
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
import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._

object Main extends SprayJsonSupport with DefaultJsonProtocol {
  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  // Case classes
  final case class Query(text: String)
  final case class ConversationResponse(lambdaResponse: String, ollamaResponse: String, nextQuery: String)
  final case class LambdaRequest(query: String)
  final case class LambdaResponse(response: String, model: String)

  // JSON formats
  implicit val queryFormat: RootJsonFormat[Query] = jsonFormat1(Query)
  implicit val conversationResponseFormat: RootJsonFormat[ConversationResponse] = jsonFormat3(ConversationResponse)
  implicit val lambdaRequestFormat: RootJsonFormat[LambdaRequest] = jsonFormat1(LambdaRequest)
  implicit val lambdaResponseFormat: RootJsonFormat[LambdaResponse] = jsonFormat2(LambdaResponse)

  private def checkModelAvailability(ollamaAPI: OllamaAPI)(implicit ec: ExecutionContext): Future[Boolean] = {
    def attemptCheck(remainingAttempts: Int): Future[Boolean] = {
      if (remainingAttempts <= 0) {
        logger.error("Model availability check failed after all attempts")
        Future.successful(false)
      } else {
        Future {
          Try {
            val options = new Options(new java.util.HashMap[String, Object]())
            ollamaAPI.generate("llama3.2:1b", "test", false, options)
            logger.info("Ollama model check successful")
            true
          }
        }.flatMap {
          case Success(true) => Future.successful(true)
          case _ =>
            logger.warn(s"Model check attempt failed, remaining attempts: ${remainingAttempts - 1}")
            Thread.sleep(10000)
            attemptCheck(remainingAttempts - 1)
        }
      }
    }

    attemptCheck(3)
  }

  private def callLambda(
                          query: Query,
                          lambdaUrl: String,
                          httpClient: org.apache.http.impl.client.CloseableHttpClient
                        )(implicit ec: ExecutionContext): Future[String] = Future {
    logger.info(s"Sending query to Lambda: ${query.text}")
    val post = new HttpPost(lambdaUrl)
    post.setHeader("Content-Type", "application/json")
    post.setEntity(new StringEntity(LambdaRequest(query.text).toJson.compactPrint))

    Try {
      val response = httpClient.execute(post)
      try {
        val responseBody = EntityUtils.toString(response.getEntity)
        if (response.getStatusLine.getStatusCode != 200) {
          throw new Exception(s"Lambda error (${response.getStatusLine.getStatusCode}): $responseBody")
        }
        responseBody.parseJson.convertTo[LambdaResponse].response
      } finally {
        response.close()
      }
    }.getOrElse {
      logger.error("Failed to execute Lambda request")
      "Error communicating with Lambda service"
    }
  }

  private def callOllama(
                          context: String,
                          ollamaAPI: OllamaAPI
                        )(implicit ec: ExecutionContext): Future[String] = Future {
    val ollamaQuery = s"""Given this conversation context: "$context", provide a thoughtful response that continues the discussion."""
    logger.info(s"Sending query to Ollama: $ollamaQuery")

    val options = new Options(new java.util.HashMap[String, Object]() {
      put("num_predict", Integer.valueOf(150))
      put("temperature", java.lang.Float.valueOf(0.7f))
    })

    Try(ollamaAPI.generate("llama3.2:1b", ollamaQuery, false, options))
      .map(_.getResponse)
      .recover { case e =>
        logger.error(s"Ollama API error: ${e.getMessage}")
        s"Error processing request: ${e.getMessage}"
      }
      .getOrElse {
        logger.error("Null response from Ollama API")
        "No response received from Ollama"
      }
  }

  def processConversation(
                           initialQuery: Query,
                           lambdaUrl: String,
                           ollamaAPI: OllamaAPI,
                           httpClient: org.apache.http.impl.client.CloseableHttpClient
                         )(implicit ec: ExecutionContext): Future[List[ConversationResponse]] = {
    def loop(
              currentQuery: String,
              turnNumber: Int,
              acc: List[ConversationResponse],
              context: String
            ): Future[List[ConversationResponse]] = {
      if (turnNumber >= 5) {
        Future.successful(acc.reverse)
      } else {
        (for {
          lambdaResponse <- callLambda(Query(currentQuery), lambdaUrl, httpClient)
            .recover { case e =>
              logger.error(s"Lambda call failed: ${e.getMessage}")
              "Error getting response from Lambda"
            }
          ollamaResponse <- callOllama(s"$context $lambdaResponse", ollamaAPI)
            .recover { case e =>
              logger.error(s"Ollama call failed: ${e.getMessage}")
              "Error getting response from Ollama"
            }
          nextResponses <- {
            val updatedContext = s"$context $lambdaResponse $ollamaResponse"
            val nextQuery = s"Regarding our discussion about $currentQuery: $ollamaResponse"
            Try(ConversationLogger.logConversation(currentQuery, lambdaResponse, ollamaResponse))
              .recover { case e => logger.error(s"Logging failed: ${e.getMessage}") }

            loop(nextQuery, turnNumber + 1,
              ConversationResponse(lambdaResponse, ollamaResponse, nextQuery) :: acc,
              updatedContext)
          }
        } yield nextResponses).recoverWith { case e =>
          logger.error(s"Conversation processing failed: ${e.getMessage}")
          Future.successful(acc.reverse)
        }
      }
    }

    loop(initialQuery.text, 0, List.empty, initialQuery.text)
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem[Nothing](Behaviors.empty, "ConversationSystem")
    implicit val executionContext = system.executionContext
    implicit val classicSystem = system.classicSystem
    implicit val materializer = akka.stream.Materializer(classicSystem)
    implicit val routingSettings = RoutingSettings(classicSystem)

    val ollamaHost = sys.env.getOrElse("OLLAMA_HOST", "http://localhost:11434")
    val ollamaAPI = new OllamaAPI(ollamaHost)
    ollamaAPI.setRequestTimeoutSeconds(1000)
    val httpClient = HttpClients.createDefault()
    val lambdaUrl = "https://lthllcut0i.execute-api.us-east-1.amazonaws.com/generate"

    val homeDir = System.getProperty("user.home")
    val conversationsDir = new java.io.File(s"$homeDir/conversations")
    if (!conversationsDir.exists()) {
      conversationsDir.mkdirs()
    }

    val corsSettings = CorsSettings.defaultSettings.withAllowGenericHttpRequests(true)

    val modelCheckResult = Await.result(checkModelAvailability(ollamaAPI), 5.minutes) match {
      case true => logger.info("Ollama model is ready")
      case false => logger.warn("Failed to verify Ollama model, but continuing startup")
    }

    val route = cors(corsSettings) {
      withRequestTimeout(5.minutes) {
        pathPrefix("conversation") {
          post {
            entity(as[Query]) { query =>
              extractActorSystem { implicit system =>
                onComplete(processConversation(query, lambdaUrl, ollamaAPI, httpClient)) {
                  case Success(responses) =>
                    complete(HttpEntity(
                      ContentTypes.`application/json`,
                      responses.toJson.prettyPrint
                    ))
                  case Failure(ex) =>
                    logger.error(s"Error processing conversation: ${ex.getMessage}", ex)
                    complete(
                      StatusCodes.InternalServerError,
                      JsObject(
                        "error" -> JsString(s"Failed to process conversation: ${ex.getMessage}"),
                        "details" -> JsString(ex.getStackTrace.mkString("\n"))
                      ).prettyPrint
                    )
                }
              }
            }
          }
        }
      }
    }

    val serverSettings = ServerSettings(system).withTimeouts(
      ServerSettings(system).timeouts
        .withRequestTimeout(5.minutes)
        .withIdleTimeout(10.minutes)
    )

    val bindingFuture = Http()(classicSystem)
      .newServerAt("0.0.0.0", 8080)
      .withSettings(serverSettings)
      .bindFlow(route)

    bindingFuture.onComplete {
      case Success(_) =>
        logger.info(s"Server online at http://0.0.0.0:8080/")
        logger.info(s"Logs will be written to ${conversationsDir.getAbsolutePath}")
      case Failure(ex) =>
        logger.error(s"Failed to bind to port 8080: ${ex.getMessage}")
        system.terminate()
    }

    sys.addShutdownHook {
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => {
          system.terminate()
          httpClient.close()
        })
    }
  }
}