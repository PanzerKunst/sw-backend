package models

import javax.mail.internet.InternetAddress
import scala.concurrent.duration.FiniteDuration
import db._
import com.raulraja.services.SmtpConfig

class ApacheCommonsEmail {
  var subject: Option[String] = None
  var textContent: Option[String] = None
  var htmlContent: Option[String] = None
  var contentType: String = _
  var from: InternetAddress = _
  var replyList: List[InternetAddress] = List()
  var toList: List[InternetAddress] = List()
  var ccList: List[InternetAddress] = List()
  var bccList: List[InternetAddress] = List()
  var headers: Map[String, String] = Map()
  var smtpConfig: SmtpConfig = _
  var retryOn: FiniteDuration = _
  var deliveryAttempts: Int = 0

  def this(email: Email, smtpConfig: SmtpConfig, retryOn: FiniteDuration) = {
    this()

    subject = email.subject
    textContent = email.textContent
    htmlContent = email.htmlContent
    contentType = email.contentType
    from = email.from
    replyList = ReplyToDto.getForEmailId(email.id)
    toList = ToDto.getForEmailId(email.id)
    ccList = CcDto.getForEmailId(email.id)
    bccList = BccDto.getForEmailId(email.id)
    headers = generateHeaders(email)
    this.smtpConfig = smtpConfig
    this.retryOn = retryOn
  }

  def generateHeaders(email: Email): Map[String, String] = {
    var result: Map[String, String] = Map()

    if (email.sender.isDefined) {
      result += (
        "Sender" -> InternetAddressUtils.toString(email.sender.get)
        )
    }

    if (!toList.isEmpty) {
      result += (
        "To" -> generateRecipientLine(email, toList)
        )
    }

    if (!ccList.isEmpty) {
      result += (
        "Cc" -> generateRecipientLine(email, ccList)
        )
    }

    if (!bccList.isEmpty) {
      result += (
        "Bcc" -> generateRecipientLine(email, bccList)
        )
    }

    val references = ReferencesDto.getForEmailId(email.id)
    if (!references.isEmpty) {
      result += (
        "References" -> generateReferencesLine(email, references)
        )
    }

    result
  }

  private def generateRecipientLine(email: Email, internetAddresses: List[InternetAddress]): String = {
    var result = ""

    if (!internetAddresses.isEmpty) {
      result += internetAddresses.head
    }

    for (i <- 1 to internetAddresses.length - 1) {
      result += ", " + internetAddresses.apply(i)
    }

    result
  }

  private def generateReferencesLine(email: Email, references: List[String]): String = {
    var result = ""

    if (!references.isEmpty) {
      result += references.head
    }

    for (i <- 1 to references.length - 1) {
      result += " " + references.apply(i)
    }

    result
  }
}
