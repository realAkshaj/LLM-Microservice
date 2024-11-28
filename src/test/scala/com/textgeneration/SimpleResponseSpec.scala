package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SimpleResponseSpec extends AnyFlatSpec with Matchers {

  "Response generation" should "create valid responses" in {
    val response = Main.ConversationResponse(
      lambdaResponse = "Test lambda",
      ollamaResponse = "Test ollama",
      nextQuery = "Test query"
    )

    response.lambdaResponse should not be empty
    response.ollamaResponse should not be empty
    response.nextQuery should not be empty
  }

  it should "maintain data consistency" in {
    val testData = "Test data"
    val response = Main.ConversationResponse(testData, testData, testData)

    response.lambdaResponse shouldBe response.ollamaResponse
    response.ollamaResponse shouldBe response.nextQuery
  }

  it should "handle length validation" in {
    val shortText = "Short"
    val response = Main.ConversationResponse(shortText, shortText, shortText)

    response.lambdaResponse.length shouldBe shortText.length
    response.ollamaResponse.length shouldBe shortText.length
    response.nextQuery.length shouldBe shortText.length
  }
}
