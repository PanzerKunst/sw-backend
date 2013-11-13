package main

import db._
import models.Email
import java.util.TimerTask

object EmailsToSendTasker extends TimerTask {
  var isRunning = false

  def run() {
    if (!isRunning) {
      isRunning = true

      for (email <- EmailDto.getEmailsToSend) {
        val (username, password) = AccountDto.getUsernameAndPasswordForId(email.fromAccountId.get)
        SenderEmailService.sendEmail(email, username, password)
        EmailDto.update(email.copy(status = Email.STATUS_SENT))
      }

      isRunning = false
    }
  }
}
