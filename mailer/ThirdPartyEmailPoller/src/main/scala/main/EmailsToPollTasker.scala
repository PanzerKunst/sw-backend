package main

import scala.io.Source
import java.io.{BufferedReader, InputStreamReader, File}
import models.Email
import java.text.SimpleDateFormat
import models.clientside.ClientSideEmail
import db._
import scala.StringBuilder
import models.InternetAddress
import scala.Some
import java.util.TimerTask
import com.typesafe.config.ConfigFactory

object EmailsToPollTasker extends TimerTask {
  val thirdPartyEmailsRootDir = ConfigFactory.load().getString("thirdPartyEmailsRootDir")

  var isRunning = false

  def run() {
    if (!isRunning) {
      isRunning = true

      val directoriesInsideThirdPartyEmailsRootDir = new File(thirdPartyEmailsRootDir).listFiles filter (file => file.isDirectory)

      for (accountDir <- directoriesInsideThirdPartyEmailsRootDir) {
        val dirForAccountNewEmail = accountDir.getAbsolutePath + "/new/"

        val filesInDirForAccountNewEmail = new File(dirForAccountNewEmail).listFiles

        if (!filesInDirForAccountNewEmail.isEmpty) {
          addPermissionsToReadAndMoveEmailFilesInsideDir(dirForAccountNewEmail)

          for (newEmailFile <- filesInDirForAccountNewEmail) {
            val clientSideEmail = clientSideEmailFromFile(newEmailFile)

            EmailDto.create(clientSideEmail) match {
              case Some(id) =>
                for (internetAddress <- clientSideEmail.to)
                  ToDto.create(id, internetAddress)

                for (internetAddress <- clientSideEmail.cc)
                  CcDto.create(id, internetAddress)

                for (internetAddress <- clientSideEmail.bcc)
                  BccDto.create(id, internetAddress)

                for (messageId <- clientSideEmail.references)
                  ReferencesDto.create(id, messageId)

              case None => throw new Exception("Creation of an email did not return an ID!")
            }

            markIncomingEmailAsRead(newEmailFile, accountDir.getAbsolutePath)
          }

          resetPermissionsToMovedEmailFilesInsideDir(accountDir.getAbsolutePath + "/cur/")
        }
      }

      isRunning = false
    }
  }

  private def clientSideEmailFromFile(newEmailFile: File): ClientSideEmail = {
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
    else if (contentType == Email.CONTENT_TYPE_MULTIPART_ALTERNATIVE && boundary.isDefined) {
      clientSideEmail.textContent = contentFromMultipart(newEmailFile.getAbsolutePath, i, Email.CONTENT_TYPE_TEXT, boundary.get)
      clientSideEmail.htmlContent = contentFromMultipart(newEmailFile.getAbsolutePath, i, Email.CONTENT_TYPE_HTML, boundary.get)
    }

    clientSideEmail.status = Email.STATUS_UNREAD

    source.close()

    clientSideEmail
  }

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
      lineValue.substring(emailStartIndex+1, emailEndIndex)
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

    // read next lines
    var isLineInteresting = true
    while (lines.hasNext && isLineInteresting) {
      line = lines.next()

      if ("^\\s.+".r.findFirstIn(line).isDefined) {
        result = result :+ messageIdFromLineValue(line)
      }
      else {
        isLineInteresting = false
      }
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

    var readContentType: String = null
    var charset: Option[String] = None

    while (readContentType != contentType) {
      while (lines.hasNext && line != "--" + boundary) {
        line = lines.next()
      }

      // Reading the line just after the boundary
      line = lines.next()

      readContentType = contentTypeFromLineValue(headerLineValue(line))
      charset = subHeaderFromLineValue(headerLineValue(line), "charset")
    }

    // We need to read until the next blank line, this is where the content will start
    while (!line.trim.isEmpty) {
      line = lines.next()
    }

    val stringBuilder = new StringBuilder

    while (lines.hasNext && !line.contains(boundary)) {
      line = lines.next()

      if (!line.contains(boundary)) {
        stringBuilder.append(line).append('\n')
      }
    }

    source.close()

    if (!stringBuilder.isEmpty) {
      Some(stringBuilder.toString())
    }
    else {
      None
    }
  }

  private def buildSimpleContentFromLines(lines: Iterator[String]): Option[String] = {
    val stringBuilder = new StringBuilder

    while (lines.hasNext) {
      stringBuilder.append(lines.next()).append('\n')
    }

    if (!stringBuilder.isEmpty) {
      Some(stringBuilder.toString())
    }
    else {
      None
    }
  }

  private def timestampFromLineValue(lineValue: String): Long = {
    new SimpleDateFormat("E, d MMM yyyy HH:mm:ss Z").parse(lineValue).getTime / 1000 // We divide by 1000 to convert ms to seconds
  }

  private def markIncomingEmailAsRead(newEmailFile: File, accountDirAbsolutePath: String) {
    // Need to invoke a shell to run a piped command, or one containing the '*' wildcard. This command is used on the Mac laptop for testing
    //runProcess(new ProcessBuilder("/bin/sh", "-c", "echo tigrou | sudo -S mv " + accountDirAbsolutePath + "/new/" + newEmailFile.getName + " " + accountDirAbsolutePath + "/cur/"))

    // On the server we don't need to supply the password
    runProcess(new ProcessBuilder("sudo", "mv", accountDirAbsolutePath + "/new/" + newEmailFile.getName, accountDirAbsolutePath + "/cur/"))
  }

  private def addPermissionsToReadAndMoveEmailFilesInsideDir(dirAbsolutePath: String) {
    setPermissionsToFilesInsideDir(dirAbsolutePath, "666")
  }

  private def resetPermissionsToMovedEmailFilesInsideDir(dirAbsolutePath: String) {
    setPermissionsToFilesInsideDir(dirAbsolutePath, "600")
  }

  private def setPermissionsToFilesInsideDir(dirAbsolutePath: String, ugoPermissions: String) {
    // Need to invoke a shell to run a piped command, or one containing the '*' wildcard. This command is used on the Mac laptop for testing
    //runProcess(new ProcessBuilder("/bin/sh", "-c", "echo tigrou | sudo -S chmod " + ugoPermissions + " " + dirAbsolutePath + "*"))

    // On the server we don't need to supply the password
    runProcess(new ProcessBuilder("/bin/sh", "-c", "sudo chmod " + ugoPermissions + " " + dirAbsolutePath + "*"))
  }

  private def runProcess(processBuilder: ProcessBuilder) {
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()

    printProcessOutput(process)

    if (process.waitFor() != 0)
      throw new OsCommandException
  }

  private def printProcessOutput(process: Process) {
    val br = new BufferedReader(new InputStreamReader(process.getInputStream))
    val builder = new StringBuilder()
    var line: String = br.readLine()

    while (line != null) {
      builder.append(line)
      builder.append(System.getProperty("line.separator"))

      line = br.readLine()
    }

    System.out.println(builder.toString())
  }
}

class OsCommandException extends Exception
