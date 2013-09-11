package controllers.api

import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db.EmailDto
import models.clientSide.ClientSideEmail
import play.api.libs.json.Json

object EmailApi extends Controller {
  val HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY = 520

  def create = Action(parse.json) {
    implicit request =>

      val clientSideEmail = JsonUtil.deserialize[ClientSideEmail](request.body.toString())

      clientSideEmail.validate match {
        case Some(errorMsg) =>
          Status(HTTP_STATUS_CODE_TO_CC_BCC_ALL_EMPTY)
        case None =>
          EmailDto.create(clientSideEmail) match {
            case Some(id) => Ok(id.toString)
            case None => InternalServerError("Creation of an email did not return an ID!")
          }
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
          List(),
          List(),
          List(),
          List()
        )
        Ok(Json.toJson(clientSideEmail))
      }
  }
}
