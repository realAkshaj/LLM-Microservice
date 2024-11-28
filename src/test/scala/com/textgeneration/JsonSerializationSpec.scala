package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._
import scala.util.Random

class JsonSerializationSpec extends AnyFlatSpec with Matchers {

  // Import only the necessary formats from Main
  import Main.{conversationResponseFormat}

  // Test single ConversationResponse serialization
  "JSON serialization" should "handle single response" in {
    val response = Main.ConversationResponse(
      lambdaResponse = s"Lambda${Random.nextInt(100)}",
      ollamaResponse = s"Ollama${Random.nextInt(100)}",
      nextQuery = s"Query${Random.nextInt(100)}"
    )

    val json = response.toJson.toString
    val parsed = json.parseJson.convertTo[Main.ConversationResponse]

    parsed shouldBe response
  }

  it should "properly escape special characters" in {
    val specialResponse = Main.ConversationResponse(
      lambdaResponse = """Hello "world" with \n newline""",
      ollamaResponse = """Test with \t tab""",
      nextQuery = "Normal query"
    )

    val json = specialResponse.toJson.toString
    val parsed = json.parseJson.convertTo[Main.ConversationResponse]

    parsed shouldBe specialResponse
    json should include ("""\\n""")
    json should include ("""\\t""")
  }

  it should "handle empty and null-like values" in {
    val emptyResponse = Main.ConversationResponse("", "", "")
    val json = emptyResponse.toJson.toString
    val parsed = json.parseJson.convertTo[Main.ConversationResponse]

    parsed shouldBe emptyResponse
    parsed.lambdaResponse shouldBe empty
    parsed.ollamaResponse shouldBe empty
    parsed.nextQuery shouldBe empty
  }

  it should "validate all fields are present" in {
    val response = Main.ConversationResponse(
      "lambda response",
      "ollama response",
      "next query"
    )

    val json = response.toJson.toString
    json should include ("lambdaResponse")
    json should include ("ollamaResponse")
    json should include ("nextQuery")
  }
}