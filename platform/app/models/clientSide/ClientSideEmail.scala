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
  var smtpReplyTo: Option[String] = None

  @JsonProperty
  var smtpSender: Option[String] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var fromAccountId: Option[Long] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var creationTimestamp: Long = _

  @JsonProperty
  var status: String = _


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
           _to: List[String],
           _cc: List[String],
           _bcc: List[String],
           _smtpReferences: List[Long]) = {
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
    this.fromAccountId = email.fromAccountId
    this.creationTimestamp = email.creationTimestamp
    this.status = email.status

    // Without the underscore on the 4 following variable, Jackson deserialization fucks up
    this.to = _to
    this.cc = _cc
    this.bcc = _bcc
    this.smtpReferences = _smtpReferences
  }

  def validate: Option[String] = {
    if (this.status != Email.STATUS_DRAFT &&
      this.status != Email.STATUS_ARCHIVED &&
      this.status != Email.STATUS_READ &&
      this.status != Email.STATUS_SENT &&
      this.status != Email.STATUS_UNREAD) {

      Some(ClientSideEmail.ERROR_MSG_INCORRECT_STATUS)
    }
    else if (this.to.length == 0 && this.cc.length == 0 && this.bcc.length == 0) {
      Some(ClientSideEmail.ERROR_MSG_TO_CC_BCC_ALL_EMPTY)
    }
    else {
      None
    }
  }
}

object ClientSideEmail {
  val ERROR_MSG_TO_CC_BCC_ALL_EMPTY = "'to', 'cc' and 'bcc' cannot all be empty"
  val ERROR_MSG_INCORRECT_STATUS = "Incorrect status"

  implicit val writes = new Writes[ClientSideEmail] {
    def writes(email: ClientSideEmail): JsValue = {
      Json.obj(
        "id" -> email.id,
        "subject" -> email.subject,
        "body" -> email.body,
        "contentType" -> email.contentType,
        "smtpMessageId" -> email.smtpMessageId,
        "smtpFrom" -> email.smtpFrom,
        "smtpTo" -> email.smtpTo,
        "smtpCc" -> email.smtpCc,
        "smtpBcc" -> email.smtpBcc,
        "smtpReplyTo" -> email.smtpReplyTo,
        "smtpSender" -> email.smtpSender,
        "fromAccountId" -> email.fromAccountId,
        "creationTimestamp" -> email.creationTimestamp,
        "status" -> email.status,
        "to" -> email.to,
        "cc" -> email.cc,
        "bcc" -> email.bcc,
        "smtpReferences" -> email.smtpReferences
      )
    }
  }
}