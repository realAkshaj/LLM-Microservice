package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

class ConversationProcessingSpec extends AnyFlatSpec with Matchers {
  import Main.ConversationResponse

  "Conversation Processing" should "create valid conversation response" in {
    val response = ConversationResponse(
      lambdaResponse = "A slow blink from a cat is like a cat kiss.",
      ollamaResponse = "That makes sense! It's a nonverbal way to show trust.",
      nextQuery = "Do you have any comments on the cat's nonverbal communication?"
    )

    response.lambdaResponse should include ("cat")
    response.ollamaResponse should include ("trust")
    response.nextQuery should include ("nonverbal")
  }

  it should "handle empty responses gracefully" in {
    val response = ConversationResponse("", "", "")

    response.lambdaResponse shouldBe empty
    response.ollamaResponse shouldBe empty
    response.nextQuery shouldBe empty
  }
}