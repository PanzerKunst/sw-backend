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
                 fromUserId: Option[Long] = None,
                 creationTimestamp: Long)


object Email {

  // Format: timestamp.rand@domain
  def generateSmtpMessageId(): String = {
    new Date().getTime + "." + new Random().nextInt() + "@" + Play.application().configuration().getString("email.domain")
  }
}