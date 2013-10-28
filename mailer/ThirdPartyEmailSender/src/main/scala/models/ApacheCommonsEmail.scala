package models

import javax.mail.internet.InternetAddress
import scala.concurrent.duration.FiniteDuration
import db._
import com.raulraja.services.SmtpConfig
import main.SenderEmailService

class ApacheCommonsEmail {
  var subject: Option[String] = None
  var textContent: Option[String] = None
  var htmlContent: Option[String] = None
  var from: InternetAddress = _
  var replyList: List[InternetAddress] = List()
  var toList: List[InternetAddress] = List()
  var ccList: List[InternetAddress] = List()
  var bccList: List[InternetAddress] = List()
  var headers: Map[String, String] = Map()
  var smtpConfig: SmtpConfig = _
  var retryOn: FiniteDuration = _
  var deliveryAttempts: Int = 0

  private var numberOfAccountsAmongRecipients: Int = 0

  def this(email: Email, smtpConfig: SmtpConfig, retryOn: FiniteDuration) = {
    this()

    subject = email.subject
    from = email.from

    replyList = ReplyToDto.getForEmailId(email.id).filter(internetAddress => !isThirdPartyAddress(internetAddress.getAddress))

    for (internetAddress <- ToDto.getForEmailId(email.id)) {
      if (isThirdPartyAddress(internetAddress.getAddress))
        toList = toList :+ internetAddress
      else
        numberOfAccountsAmongRecipients = numberOfAccountsAmongRecipients + 1
    }

    for (internetAddress <- CcDto.getForEmailId(email.id)) {
      if (isThirdPartyAddress(internetAddress.getAddress))
        ccList = ccList :+ internetAddress
      else
        numberOfAccountsAmongRecipients = numberOfAccountsAmongRecipients + 1
    }

    bccList = BccDto.getForEmailId(email.id).filter(internetAddress => !isThirdPartyAddress(internetAddress.getAddress))

    textContent = textContentWithNotificationOfAccounts(email.textContent)
    htmlContent = htmlContentWithNotificationOfAccounts(email.htmlContent)

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

    val references = ReferencesDto.getForEmailId(email.id)
    if (!references.isEmpty) {
      result += (
        "References" -> generateReferencesLine(email, references)
        )
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

  private def isThirdPartyAddress(address: String): Boolean = {
    val addressDomain = address.substring(address.indexOf("@") + 1)
    addressDomain != SenderEmailService.MAILER_DOMAIN
  }

  private def textContentWithNotificationOfAccounts(textContent: Option[String]): Option[String] = {
    textContent match {
      case None => None
      case Some(content) => notificationOfAccountsMessage match {
        case Some(notificationOfAccountsMessage) => Some(content + "\n" + notificationOfAccountsMessage)
        case None => Some(content)
      }
    }
  }

  private def htmlContentWithNotificationOfAccounts(htmlContent: Option[String]): Option[String] = {
    htmlContent match {
      case None => None
      case Some(content) => notificationOfAccountsMessage match {
        case Some(notificationOfAccountsMessage) => Some(content + "<p style='font-style: italic;'>" + notificationOfAccountsMessage + "</p>")
        case None => Some(content)
      }
    }
  }

  private def notificationOfAccountsMessage: Option[String] = {
    numberOfAccountsAmongRecipients match {
      case 0 => None
      case 1 => Some("Note: one @" + SenderEmailService.MAILER_DOMAIN + " user was also recipient of this message. This user received an encrypted copy of this message, and for security reasons his or her address is not in the list of recipients.")
      case _ => Some("Note: " + numberOfAccountsAmongRecipients + " @" + SenderEmailService.MAILER_DOMAIN + " users were also recipients of this message. These " + numberOfAccountsAmongRecipients + " users received an encrypted copy of this message, and for security reasons their address is not in the list of recipients.")
    }
  }
}
