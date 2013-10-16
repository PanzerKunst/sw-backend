package models.clientside

import models.{InternetAddress, Email}
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
  var textContent: Option[String] = None

  @JsonProperty
  var htmlContent: Option[String] = None

  @JsonProperty
  var messageId: Option[String] = None

  @JsonProperty
  var from: InternetAddress = _

  @JsonProperty
  var sender: Option[InternetAddress] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var fromAccountId: Option[Long] = None

  @JsonProperty
  @JsonDeserialize(contentAs = classOf[java.lang.Long])
  var creationTimestamp: Long = _

  @JsonProperty
  var status: String = _


  @JsonProperty
  var to: List[InternetAddress] = List()

  @JsonProperty
  var cc: List[InternetAddress] = List()

  @JsonProperty
  var bcc: List[InternetAddress] = List()

  @JsonProperty
  var replyTo: List[InternetAddress] = List()

  @JsonProperty
  var references: List[String] = List()

  def this(email: Email,
           _to: List[InternetAddress],
           _cc: List[InternetAddress],
           _bcc: List[InternetAddress],
           _replyTo: List[InternetAddress],
           _references: List[String]) = {
    this()

    this.id = Some(email.id)
    this.subject = email.subject
    this.textContent = email.textContent
    this.htmlContent = email.htmlContent
    this.messageId = Some(email.messageId)
    this.from = email.from
    this.sender = email.sender
    this.fromAccountId = email.fromAccountId
    this.creationTimestamp = email.creationTimestamp
    this.status = email.status

    // Without the underscore on the 4 following variable, Jackson deserialization fucks up
    this.to = _to
    this.cc = _cc
    this.bcc = _bcc
    this.replyTo = _replyTo
    this.references = _references
  }

  def validate: Option[String] = {
    if (this.from == null ||
      this.replyTo == null ||
      !this.fromAccountId.isDefined ||
      this.creationTimestamp == 0.toLong ||
      this.status == null ||
      this.to == null ||
      this.cc == null ||
      this.bcc == null ||
      this.references == null) {

      Some(ClientSideEmail.ERROR_MSG_MISSING_REQUIRED_FIELDS)
    }
    else if (this.status != Email.STATUS_DRAFT &&
      this.status != Email.STATUS_TO_SEND) {

      Some(ClientSideEmail.ERROR_MSG_INCORRECT_STATUS)
    }
    else if (this.to.isEmpty && this.cc.isEmpty && this.bcc.isEmpty) {
      Some(ClientSideEmail.ERROR_MSG_TO_CC_BCC_ALL_EMPTY)
    }
    else {
      None
    }
  }
}

object ClientSideEmail {
  val ERROR_MSG_MISSING_REQUIRED_FIELDS = "Some required fields are missing"
  val ERROR_MSG_TO_CC_BCC_ALL_EMPTY = "'to', 'cc' and 'bcc' cannot all be empty"
  val ERROR_MSG_INCORRECT_STATUS = "Incorrect status"

  implicit val writes = new Writes[ClientSideEmail] {
    def writes(email: ClientSideEmail): JsValue = {
      Json.obj(
        "id" -> email.id,
        "subject" -> email.subject,
        "textContent" -> email.textContent,
        "htmlContent" -> email.htmlContent,
        "messageId" -> email.messageId,
        "from" -> email.from,
        "replyTo" -> email.replyTo,
        "sender" -> email.sender,
        "fromAccountId" -> email.fromAccountId,
        "creationTimestamp" -> email.creationTimestamp,
        "status" -> email.status,
        "to" -> email.to,
        "cc" -> email.cc,
        "bcc" -> email.bcc,
        "references" -> email.references
      )
    }
  }
}