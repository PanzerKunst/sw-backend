package controllers.api

import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db._
import models.clientSide.ClientSideEmail
import play.api.libs.json.Json
import javax.security.auth.login.AccountNotFoundException
import scala.Some

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

  def getOfId(id: Int) = Action {
    implicit request =>

      val filters = Some(Map("e.id" -> id.toString))

      val matchingEmails = EmailDto.get(filters)

      if (matchingEmails.isEmpty)
        NoContent
      else {
        val clientSideEmail = new ClientSideEmail(
          matchingEmails.head,
          List(),
          List(),
          List(),
          List()
        )
        Ok(Json.toJson(clientSideEmail))
      }
  }
}
