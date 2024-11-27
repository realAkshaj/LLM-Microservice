package com.textgeneration

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger
import java.io.{File, PrintWriter, FileWriter}
import spray.json._
import DefaultJsonProtocol._

object ConversationLogger {
  private val logger = Logger(LoggerFactory.getLogger(this.getClass))
  private var conversationFile: Option[File] = None

  private def createConversationFile(): File = {
    if (conversationFile.isEmpty) {
      val dir = new File("conversations")
      if (!dir.exists()) dir.mkdirs()
      val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
      conversationFile = Some(new File(dir, s"conversation_$timestamp.txt"))
    }
    conversationFile.get
  }

  def logConversation(query: String, lambdaResponse: String, ollamaResponse: String): Unit = {
    val file = createConversationFile()
    val writer = new PrintWriter(new FileWriter(file, true))
    try {
      val content =
        s"""Turn ${getTurnCount(file)}:
           |Initial Query:
           |$query
           |
           |Bedrock Response:
           |$lambdaResponse
           |
           |Ollama Response:
           |$ollamaResponse
           |
           |Next Query Generated:
           |Do you have any comments on: $ollamaResponse
           |
           |----------------------------------------
           |""".stripMargin

      writer.write(content)
      writer.flush()
    } finally {
      writer.close()
    }
  }

  private def getTurnCount(file: File): Int = {
    if (!file.exists()) return 1
    scala.io.Source.fromFile(file).getLines().count(_.startsWith("Turn ")) + 1
  }

  def resetConversation(): Unit = {
    conversationFile = None
  }
}