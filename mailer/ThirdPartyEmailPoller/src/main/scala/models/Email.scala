package models

import scala.util.Random
import java.util.Date


case class Email(id: Long,
                 subject: Option[String],
                 textContent: Option[String],
                 htmlContent: Option[String],
                 messageId: String,
                 from: InternetAddress,
                 sender: Option[InternetAddress],
                 fromAccountId: Option[Long],
                 creationTimestamp: Long,
                 status: String)

object Email {

  // Format: timestamp.rand@domain
  def generateMessageId(): String = {
    new Date().getTime + "." + new Random().nextInt() + "@" + DOMAIN
  }

  val STATUS_DRAFT = "DRAFT"
  val STATUS_TO_SEND = "TO_SEND"
  val STATUS_SENT = "SENT"
  val STATUS_UNREAD = "UNREAD"
  val STATUS_READ = "READ"
  val STATUS_ARCHIVED = "ARCHIVED"

  val CONTENT_TYPE_TEXT = "text/plain"
  val CONTENT_TYPE_HTML = "text/html"
  val CONTENT_TYPE_MULTIPART_ALTERNATIVE = "multipart/alternative"

  val HEADER_SUBJECT = "subject"
  val HEADER_CONTENT_TYPE = "content-type"
  val HEADER_MESSAGE_ID = "message-id"
  val HEADER_FROM = "from"
  val HEADER_SENDER = "sender"
  val HEADER_DATE = "date"
  val HEADER_TO = "to"
  val HEADER_CC = "cc"
  val HEADER_BCC = "bcc"
  val HEADER_REFERENCES = "references"

  // Secondary headers
  val HEADER_DELIVERED_TO = "delivered-to"

  val DOMAIN = "reportingfromtheborderland.net"
}
