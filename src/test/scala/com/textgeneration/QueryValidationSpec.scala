package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.Random

class QueryValidationSpec extends AnyFlatSpec with Matchers {

  class QueryValidator {
    def validateQuery(query: String): Boolean = {
      query != null &&
        query.trim.nonEmpty &&
        query.length <= 1000 &&
        !query.matches(".*[<>].*")
    }

    def sanitizeQuery(query: String): String = {
      Option(query).map(_.trim)
        .filter(_.nonEmpty)
        .map(_.replaceAll("[<>]", ""))
        .getOrElse("")
    }

    def generateNextQuery(context: String, response: String): String = {
      val templates = List(
        s"Regarding '$context': $response",
        s"Based on the response '$response', can you elaborate?",
        s"Following up on '$response'",
        s"Given your response about '$context'"
      )
      templates(Random.nextInt(templates.length))
    }
  }

  "Query validation" should "reject invalid queries" in {
    val validator = new QueryValidator()

    validator.validateQuery(null) shouldBe false
    validator.validateQuery("") shouldBe false
    validator.validateQuery("   ") shouldBe false
    validator.validateQuery("<script>alert(1)</script>") shouldBe false
    validator.validateQuery("a" * 1001) shouldBe false
  }

  it should "accept valid queries" in {
    val validator = new QueryValidator()

    validator.validateQuery("Hello") shouldBe true
    validator.validateQuery("How are you?") shouldBe true
    validator.validateQuery("This is a normal question!") shouldBe true
    validator.validateQuery("a" * 1000) shouldBe true
  }

  it should "generate appropriate follow-up queries" in {
    val validator = new QueryValidator()
    val context = "initial question"
    val response = "interesting response"

    val nextQuery = validator.generateNextQuery(context, response)
    nextQuery should (
      include(context) or
        include(response)
      )
  }

  it should "properly sanitize queries" in {
    val validator = new QueryValidator()

    validator.sanitizeQuery("<script>alert(1)</script>") shouldBe "scriptalert(1)/script"
    validator.sanitizeQuery("   Hello   ") shouldBe "Hello"
    validator.sanitizeQuery(null) shouldBe ""
  }
}