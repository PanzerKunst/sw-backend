package controllers.api

import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db._
import models.clientSide.ClientSideEmail
import play.api.libs.json.Json
import javax.security.auth.login.AccountNotFoundException
import scala.Some
import models.Email

object EmailApi extends Controller {
  val HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY = 520
  val HTTP_STATUS_ACCOUNT_NOT_FOUND = 521
  val HTTP_STATUS_INCORRECT_STATUS = 522

  def create = Action(parse.json) {
    implicit request =>

      val clientSideEmail = JsonUtil.deserialize[ClientSideEmail](request.body.toString())

      clientSideEmail.validate match {
        case Some(errorMsg) =>
          if (errorMsg == ClientSideEmail.ERROR_MSG_TO_CC_BCC_ALL_EMPTY)
            Status(HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY)
          else
            Status(HTTP_STATUS_INCORRECT_STATUS)
        case None =>
          try {
            EmailDto.create(clientSideEmail) match {
              case Some(id) =>
                for (address <- clientSideEmail.to)
                  ToDto.create(id, address)

                for (address <- clientSideEmail.cc)
                  CcDto.create(id, address)

                for (address <- clientSideEmail.bcc)
                  BccDto.create(id, address)

                for (emailId <- clientSideEmail.smtpReferences)
                  SmtpReferencesDto.create(id, emailId)

                Ok(id.toString)
              case None => InternalServerError("Creation of an email did not return an ID!")
            }
          }
          catch {
            case anfe: AccountNotFoundException => Status(HTTP_STATUS_ACCOUNT_NOT_FOUND)
            case e: Exception => InternalServerError(e.getMessage)
          }
      }
  }

  def get = Action {
    implicit request =>

      try {
        if (request.queryString.contains("accountId") && request.queryString.contains("status")) {
          val accountId = request.queryString.get("accountId").get.head
          val statuses = request.queryString.get("status").get.head.split(',').toList

          val matchingEmails = if (statuses.length == 1 && statuses.head == Email.STATUS_SENT) {
            val filters = Some(Map(
              "from_account_id" -> accountId,
              "status" -> Email.STATUS_SENT
            ))

            EmailDto.get(filters)
          }
          else if (!statuses.contains(Email.STATUS_SENT)){
            val accountFilters = Some(Map("id" -> accountId))
            val username = AccountDto.get(accountFilters).head.username

            EmailDto.getEmailsToAccount(username, statuses)
          }
          else {
            throw new IncorrectEmailRequestException()
          }

          if (matchingEmails.isEmpty)
            NoContent
          else {
            val clientSideEmails = for (email <- matchingEmails) yield new ClientSideEmail(
              email,
              ToDto.get(Some(Map("email_id" -> email.id.toString))),
              CcDto.get(Some(Map("email_id" -> email.id.toString))),
              BccDto.get(Some(Map("email_id" -> email.id.toString))),
              SmtpReferencesDto.get(Some(Map("email_id" -> email.id.toString)))
            )
            Ok(Json.toJson(clientSideEmails))
          }
        }
        else {
          throw new IncorrectEmailRequestException()
        }
      }
      catch {
        case iere: IncorrectEmailRequestException => Forbidden
        case e: Exception => InternalServerError(e.getMessage)
      }
  }

  def getOfId(id: Int) = Action {
    implicit request =>

      val filters = Some(Map("id" -> id.toString))

      val matchingEmails = EmailDto.get(filters)

      if (matchingEmails.isEmpty)
        NoContent
      else {
        val clientSideEmail = new ClientSideEmail(
          matchingEmails.head,
          ToDto.get(Some(Map("email_id" -> matchingEmails.head.id.toString))),
          CcDto.get(Some(Map("email_id" -> matchingEmails.head.id.toString))),
          BccDto.get(Some(Map("email_id" -> matchingEmails.head.id.toString))),
          SmtpReferencesDto.get(Some(Map("email_id" -> matchingEmails.head.id.toString)))
        )
        Ok(Json.toJson(clientSideEmail))
      }
  }
}

class IncorrectEmailRequestException extends Exception