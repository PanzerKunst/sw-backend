package models.clientSide

import models.Email
import org.codehaus.jackson.annotate.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import play.api.libs.json.{Json, JsValue, Writes}

class ClientSideEmail {
  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var id: Option[Long] = None

  @JsonProperty
  var subject: Option[String] = None

  @JsonProperty
  var body: Option[String] = None

  @JsonProperty
  var contentType: String = _

  @JsonProperty
  var smtpMessageId: Option[String] = None

  @JsonProperty
  var smtpFrom: String = _

  @JsonProperty
  var smtpTo: Option[String] = None

  @JsonProperty
  var smtpCc: Option[String] = None

  @JsonProperty
  var smtpBcc: Option[String] = None

  @JsonProperty
  var smtpReplyTo: Option[String] = _

  @JsonProperty
  var smtpSender: Option[String] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var fromUserId: Option[Long] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var creationTimestamp: Long = _


  @JsonProperty
  var to: List[String] = List()

  @JsonProperty
  var cc: List[String] = List()

  @JsonProperty
  var bcc: List[String] = List()

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var smtpReferences: List[Long] = List()

  def this(email: Email,
           to: List[String],
           cc: List[String],
           bcc: List[String],
           smtpReferences: List[Long]) = {
    this()

    this.id = Some(email.id)
    this.subject = email.subject
    this.body = email.body
    this.contentType = email.contentType
    this.smtpMessageId = Some(email.smtpMessageId)
    this.smtpFrom = email.smtpFrom
    this.smtpTo = email.smtpTo
    this.smtpCc = email.smtpCc
    this.smtpBcc = email.smtpBcc
    this.smtpReplyTo = email.smtpReplyTo
    this.smtpSender = email.smtpSender
    this.fromUserId = email.fromUserId
    this.creationTimestamp = email.creationTimestamp

    this.to = to
    this.cc = cc
    this.bcc = bcc
    this.smtpReferences = smtpReferences
  }

  /* TODO: remove? def toEmail: Email = {
    models.Email(this.id.get,
      this.subject,
      this.body,
      this.contentType,
      this.smtpMessageId.get,
      this.smtpFrom,
      this.smtpTo,
      this.smtpCc,
      this.smtpBcc,
      this.smtpReplyTo,
      this.smtpSender,
      this.fromUserId,
      this.creationTimestamp
    )
  } */

  def validate: Option[String] = {
    if (this.to.length == 0 && this.cc.length == 0 && this.bcc.length == 0) {
      Some(ClientSideEmail.ERROR_MSG_TO_CC_BCC_ALL_EMPTY)
    }
    else {
      None
    }
  }
}

object ClientSideEmail {
  val ERROR_MSG_TO_CC_BCC_ALL_EMPTY = "'to', 'cc' and 'bcc' cannot all be empty"

  implicit val writes = new Writes[ClientSideEmail] {
    def writes(user: ClientSideEmail): JsValue = {
      Json.obj(
        "id" -> user.id,
        "subject" -> user.subject,
        "body" -> user.body,
        "contentType" -> user.contentType,
        "smtpMessageId" -> user.smtpMessageId,
        "smtpFrom" -> user.smtpFrom,
        "smtpTo" -> user.smtpTo,
        "smtpCc" -> user.smtpCc,
        "smtpBcc" -> user.smtpBcc,
        "smtpReplyTo" -> user.smtpReplyTo,
        "smtpSender" -> user.smtpSender,
        "fromUserId" -> user.fromUserId,
        "creationTimestamp" -> user.creationTimestamp,
        "to" -> user.to,
        "cc" -> user.cc,
        "bcc" -> user.bcc,
        "smtpReferences" -> user.smtpReferences
      )
    }
  }
}