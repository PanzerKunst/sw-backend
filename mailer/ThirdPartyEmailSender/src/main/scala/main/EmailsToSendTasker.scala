package main

import com.typesafe.scalalogging.slf4j.Logging
import akka.actor.{Actor, Props}
import db._
import models.{InternetAddressUtils, Email}
import javax.mail.internet.InternetAddress

object EmailsToSendTasker extends Logging {
  val actor = Sender.system.actorOf(Props(new Actor {
    def receive = {
      case _ =>
        for (email <- EmailDto.getEmailsToSend) {
          val (username, password) = AccountDto.getUsernameAndPasswordForId(email.fromAccountId.get)
          SenderEmailService.sendEmail(email, username, password)
          EmailDto.update(email.copy(status = Email.STATUS_SENT))
        }
    }
  }))
}
