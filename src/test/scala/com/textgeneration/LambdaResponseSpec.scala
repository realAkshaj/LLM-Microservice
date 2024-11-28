package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import spray.json._

class LambdaResponseSpec extends AnyFlatSpec with Matchers {
  import Main.{Query, LambdaRequest, LambdaResponse}
  import Main.{queryFormat, lambdaRequestFormat, lambdaResponseFormat}

  "Lambda Response Processing" should "correctly parse lambda response" in {
    val jsonResponse = """{"response": "Test response", "model": "amazon.titan-text-lite-v1"}"""
    val parsed = jsonResponse.parseJson.convertTo[LambdaResponse]

    parsed.response shouldBe "Test response"
    parsed.model shouldBe "amazon.titan-text-lite-v1"
  }

  it should "convert LambdaRequest to JSON" in {
    val request = LambdaRequest("How do cats show affection?")
    val json = request.toJson.toString

    json should include ("How do cats show affection?")
    json should include ("query")
  }
}