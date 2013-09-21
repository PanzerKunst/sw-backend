package main

import com.typesafe.scalalogging.slf4j.Logging
import com.raulraja.services.{EmailService, SmtpConfig}
import models.{ApacheCommonsEmail, Email}
import scala.concurrent.duration._

object SenderEmailService extends Logging {
  val MAILER_HOST = "mailer.reportingfromtheborderland.net"
  val MAILER_DOMAIN = "reportingfromtheborderland.net"

  def sendEmail(email: Email, accountUsername: String, accountPassword: String) {
    val smtpConfig = SmtpConfig(
      host = MAILER_HOST,
      tls = true,
      user = accountUsername,
      password = accountPassword
    )

    EmailService.send(new ApacheCommonsEmail(
      email,
      smtpConfig,
      1 minute
    ))
  }
}
