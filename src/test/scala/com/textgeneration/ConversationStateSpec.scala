package com.textgeneration

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.collection.mutable

class ConversationStateSpec extends AnyFlatSpec with Matchers {

  class MockConversationState {
    private val conversations = mutable.Map[String, List[Main.ConversationResponse]]()
    private var currentId = 0

    def startConversation(query: Main.Query): String = {
      currentId += 1
      val id = s"conv-$currentId"
      conversations(id) = List()
      id
    }

    def addResponse(id: String, response: Main.ConversationResponse): Unit = {
      conversations(id) = response :: conversations.getOrElse(id, List())
    }

    def getConversation(id: String): List[Main.ConversationResponse] = {
      conversations.getOrElse(id, List())
    }
  }

  "Conversation state" should "maintain separate conversations" in {
    val state = new MockConversationState()

    val conv1 = state.startConversation(Main.Query("First conversation"))
    val conv2 = state.startConversation(Main.Query("Second conversation"))

    state.addResponse(conv1, Main.ConversationResponse("L1", "O1", "N1"))
    state.addResponse(conv2, Main.ConversationResponse("L2", "O2", "N2"))

    state.getConversation(conv1) should have length 1
    state.getConversation(conv2) should have length 1
    state.getConversation(conv1).head.lambdaResponse shouldBe "L1"
    state.getConversation(conv2).head.lambdaResponse shouldBe "L2"
  }

  it should "maintain conversation order" in {
    val state = new MockConversationState()
    val convId = state.startConversation(Main.Query("Test"))

    val responses = (1 to 5).map { i =>
      Main.ConversationResponse(s"L$i", s"O$i", s"N$i")
    }

    responses.foreach(state.addResponse(convId, _))

    val conversation = state.getConversation(convId)
    conversation should have length 5
    conversation.reverse.zip(responses).foreach { case (actual, expected) =>
      actual shouldBe expected
    }
  }
}