package com.textgeneration.util

import spray.json._
import com.textgeneration.client.BedrockResponse

object JsonFormats extends DefaultJsonProtocol {
  implicit val bedrockResponseFormat: RootJsonFormat[BedrockResponse] = jsonFormat2(BedrockResponse)
}