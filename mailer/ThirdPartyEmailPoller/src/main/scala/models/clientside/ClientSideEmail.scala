package models.clientside

import models.{InternetAddress, Email}

class ClientSideEmail {
  var id: Option[Long] = None
  var subject: Option[String] = None
  var textContent: Option[String] = None
  var htmlContent: Option[String] = None
  var messageId: Option[String] = None
  var from: InternetAddress = _
  var sender: Option[InternetAddress] = None
  var fromAccountId: Option[Long] = None
  var creationTimestamp: Long = _
  var status: String = _

  var to: List[InternetAddress] = List()
  var cc: List[InternetAddress] = List()
  var bcc: List[InternetAddress] = List()
  var replyTo: List[InternetAddress] = List()
  var references: List[String] = List()

  def this(email: Email,
           to: List[InternetAddress],
           cc: List[InternetAddress],
           bcc: List[InternetAddress],
           replyTo: List[InternetAddress],
           references: List[String]) = {
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

    this.to = to
    this.cc = cc
    this.bcc = bcc
    this.replyTo = replyTo
    this.references = references
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
}