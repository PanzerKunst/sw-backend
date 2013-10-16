package main

import com.typesafe.scalalogging.slf4j.Logging
import akka.actor.{Actor, Props}
import scala.io.Source
import java.io.File
import models.{InternetAddress, Email}
import java.text.SimpleDateFormat
import models.clientside.ClientSideEmail

object EmailsToPollTasker extends Logging {
  val THIRD_PARTY_EMAILS_ROOT_DIR = "/Users/blackbird/Desktop/vmail" //TODO "/mnt/vmail"

  val actor = Poller.system.actorOf(Props(new Actor {
    def receive = {
      case _ =>
        for (accountDir <- new File(THIRD_PARTY_EMAILS_ROOT_DIR).listFiles) {
          for (newEmailFile <- new File(accountDir.getAbsolutePath + "/new").listFiles) {
            val source = Source.fromFile(newEmailFile.getAbsolutePath)

            val clientSideEmail = new ClientSideEmail()

            var i = 0
            val lines = source.getLines()
            var line: String = null
            var contentType: String = null
            var charset: Option[String] = None
            var boundary: Option[String] = None

            while (lines.hasNext && line != "") {
              line = lines.next()

              logger.info("line: " + line)

              if (line.toLowerCase.startsWith(Email.HEADER_SUBJECT)) {
                clientSideEmail.subject = Some(headerLineValue(line))
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_CONTENT_TYPE)) {
                contentType = contentTypeFromLineValue(headerLineValue(line))
                charset = subHeaderFromLineValue(headerLineValue(line), "charset")
                boundary = subHeaderFromLineValue(headerLineValue(line), "boundary")
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_MESSAGE_ID)) {
                clientSideEmail.messageId = Some(headerLineValue(line))
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_FROM)) {
                clientSideEmail.from = internetAddressFromLineValue(headerLineValue(line))
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_SENDER)) {
                clientSideEmail.sender = Some(internetAddressFromLineValue(headerLineValue(line)))
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_DATE)) {
                clientSideEmail.creationTimestamp = timestampFromLineValue(headerLineValue(line))
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_TO)) {
                clientSideEmail.to = internetAddressListFromFile(newEmailFile.getAbsolutePath, i)
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_CC)) {
                clientSideEmail.cc = internetAddressListFromFile(newEmailFile.getAbsolutePath, i)
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_BCC)) {
                clientSideEmail.bcc = internetAddressListFromFile(newEmailFile.getAbsolutePath, i)
              }
              else if (line.toLowerCase.startsWith(Email.HEADER_REFERENCES)) {
                clientSideEmail.references = referencesFromFile(newEmailFile.getAbsolutePath, i)
              }

              // The header "To:" can sometimes be empty
              else if (line.toLowerCase.startsWith(Email.HEADER_DELIVERED_TO)) {
                clientSideEmail.to = internetAddressListFromFile(newEmailFile.getAbsolutePath, i)
              }

              i = i + 1
            }

            // Empty line -> body starts

            if (contentType == Email.CONTENT_TYPE_TEXT) {
              clientSideEmail.textContent = buildSimpleContentFromLines(lines)
            }
            else if (contentType == Email.CONTENT_TYPE_HTML) {
              clientSideEmail.htmlContent = buildSimpleContentFromLines(lines)
            }
            else if (contentType == Email.CONTENT_TYPE_HTML_WITH_TEXT_FALLBACK && boundary.isDefined) {
              clientSideEmail.textContent = contentFromMultipart(newEmailFile.getAbsolutePath, i, Email.CONTENT_TYPE_TEXT, boundary.get)
              clientSideEmail.htmlContent = contentFromMultipart(newEmailFile.getAbsolutePath, i, Email.CONTENT_TYPE_HTML, boundary.get)
            }

            source.close()
          }
        }
    }
  }))

  private def headerLineValue(line: String): String = {
    val trimmedLine = line.substring(line.indexOf(":") + 1).trim

    if (trimmedLine.startsWith("<") && trimmedLine.endsWith(">")) {
      trimmedLine.substring(1, trimmedLine.length - 1)
    } else {
      trimmedLine
    }
  }

  private def internetAddressFromLineValue(lineValue: String): InternetAddress = {
    val emailStartIndex = lineValue.indexOf("<")
    val emailEndIndex = lineValue.indexOf(">")

    if (emailStartIndex == -1) {
      InternetAddress(
        email = lineValue.trim,
        name = None
      )
    }
    else {
      val lineWithoutEmail = lineValue.substring(0, emailStartIndex) + lineValue.substring(emailEndIndex + 1, lineValue.length)
      InternetAddress(
        email = lineValue.substring(emailStartIndex + 1, emailEndIndex).trim,
        name = Some(lineWithoutEmail.replaceAll(",", "").trim) // The line may have a comma separator at the end
      )
    }
  }

  private def messageIdFromLineValue(lineValue: String): String = {
    val emailStartIndex = lineValue.indexOf("<")
    val emailEndIndex = lineValue.indexOf(">")

    if (emailStartIndex == -1) {
      lineValue
    }
    else {
      lineValue.substring(0, emailStartIndex) + lineValue.substring(emailEndIndex + 1, lineValue.length)
    }
  }

  private def contentTypeFromLineValue(lineValue: String): String = {
    val separatorIndex = lineValue.indexOf(";")

    if (separatorIndex == -1) {
      lineValue.trim
    }
    else {
      lineValue.substring(0, separatorIndex).trim
    }
  }

  private def subHeaderFromLineValue(lineValue: String, subHeaderName: String): Option[String] = {
    val charsetIndex = lineValue.indexOf(subHeaderName + "=")

    if (charsetIndex == -1) {
      None
    }
    else {
      Some(lineValue.substring(charsetIndex + subHeaderName.length + 1).trim) // +1 because of "="
    }
  }

  private def internetAddressListFromFile(filePath: String, firstLineIndex: Int): List[InternetAddress] = {
    val source = Source.fromFile(filePath)

    val lines = source.getLines()
    var line: String = null

    for (i <- 0 to firstLineIndex) {
      line = lines.next()
    }

    var result = List(internetAddressFromLineValue(headerLineValue(line)))

    // read next lines
    var isLineInteresting = true
    while (lines.hasNext && isLineInteresting) {
      line = lines.next()

      if ("^\\s.+".r.findFirstIn(line).isDefined) {
        result = result :+ internetAddressFromLineValue(line)
      }
      else {
        isLineInteresting = false
      }
    }

    source.close()

    result
  }

  private def referencesFromFile(filePath: String, firstLineIndex: Int): List[String] = {
    val source = Source.fromFile(filePath)

    val lines = source.getLines()
    var line: String = null

    for (i <- 0 to firstLineIndex) {
      line = lines.next()
    }

    var result = List(messageIdFromLineValue(headerLineValue(line)))

    while (lines.hasNext && "^\\s".r.findFirstIn(line).isDefined) {
      line = lines.next()
      result = result :+ messageIdFromLineValue(line)
    }

    source.close()

    result
  }

  private def contentFromMultipart(filePath: String, firstLineIndex: Int, contentType: String, boundary: String): Option[String] = {
    val source = Source.fromFile(filePath)

    val lines = source.getLines()
    var line: String = null

    for (i <- 0 to firstLineIndex) {
      line = lines.next()
    }

    while (lines.hasNext && line != "--" + boundary) {
      line = lines.next()
    }

    // Reading the line just after the boundary
    line = lines.next()

    val readContentType = contentTypeFromLineValue(headerLineValue(line))
    val charset = subHeaderFromLineValue(headerLineValue(line), "charset")

    var content = ""

    if (readContentType == contentType) {
      while (lines.hasNext && line != "--" + boundary) {
        line = lines.next()

        if (line != "--" + boundary) {
          content = content + line + "\\n"
        }
      }
    }

    source.close()

    if (content != "") {
      Some(content)
    }
    else {
      None
    }
  }

  private def buildSimpleContentFromLines(lines: Iterator[String]): Option[String] = {
    var content = ""

    while (lines.hasNext) {
      content = content + lines.next() + "\\n"
    }

    if (content != "") {
      Some(content)
    }
    else {
      None
    }
  }

  private def timestampFromLineValue(lineValue: String): Long = {
    new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z").parse(lineValue).getTime
  }
}
