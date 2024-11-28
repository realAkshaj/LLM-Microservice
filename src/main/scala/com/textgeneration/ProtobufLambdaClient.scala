package com.textgeneration

import com.conversation.grpc.bedrock_service.{GenerateRequest, GenerateResponse, GenerationConfig}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.util.EntityUtils
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import java.util.Base64

class ProtobufLambdaClient(httpClient: CloseableHttpClient, lambdaUrl: String) {
  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  def generate(query: String): String = {
    val request = GenerateRequest(
      query = query,
      config = Some(GenerationConfig(
        maxTokens = 150,
        temperature = 0.7f
      ))
    )

    logger.info(s"Sending protobuf request for query: $query")

    val post = new HttpPost(lambdaUrl)
    post.setHeader("Content-Type", "application/x-protobuf")
    val serializedRequest = Base64.getEncoder.encodeToString(request.toByteArray)
    post.setEntity(new ByteArrayEntity(serializedRequest.getBytes))

    val response = httpClient.execute(post)
    try {
      val responseBody = EntityUtils.toString(response.getEntity)
      val bytes = Base64.getDecoder.decode(responseBody)
      val protoResponse = GenerateResponse.parseFrom(bytes)
      logger.info(s"Received response: ${protoResponse.response}")
      protoResponse.response
    } finally {
      response.close()
    }
  }
}