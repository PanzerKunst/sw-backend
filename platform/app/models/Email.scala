package models

import java.util.Date
import util.Random
import play.Play

case class Email(id: Long,
                 subject: Option[String] = None,
                 body: Option[String] = None,
                 contentType: String,
                 smtpMessageId: String,
                 smtpFrom: String,
                 smtpTo: Option[String] = None,
                 smtpCc: Option[String] = None,
                 smtpBcc: Option[String] = None,
                 smtpReplyTo: Option[String] = None,
                 smtpSender: Option[String] = None,
                 fromAccountId: Option[Long] = None,
                 creationTimestamp: Long,
                 status: String)


object Email {

  // Format: timestamp.rand@domain
  def generateSmtpMessageId(): String = {
    new Date().getTime + "." + new Random().nextInt() + "@" + Play.application().configuration().getString("email.domain")
  }

  val STATUS_DRAFT = "DRAFT"
  val STATUS_SENT = "SENT"
  val STATUS_UNREAD = "UNREAD"
  val STATUS_READ = "READ"
  val STATUS_ARCHIVED = "ARCHIVED"
}