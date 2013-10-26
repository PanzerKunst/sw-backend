package controllers.api

import models.Account
import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db.{PostfixAccountDto, AccountDto}
import play.api.libs.json.Json

object AccountApi extends Controller {
  val HTTP_STATUS_CODE_USERNAME_ALREADY_TAKEN = 520

  def create = Action(parse.json) {
    implicit request =>

      val account = JsonUtil.deserialize[Account](request.body.toString())
      try {
        AccountDto.create(account) match {
          case Some(id) =>
            PostfixAccountDto.create(account, id)
            Created(id.toString)
          case None => InternalServerError("Creation of an account did not return an ID!")
        }
      }
      catch {
        case uate: UsernameAlreadyTakenException => Status(HTTP_STATUS_CODE_USERNAME_ALREADY_TAKEN)
        case e: Exception => InternalServerError(e.getMessage)
      }
  }

  def update(id: Long) = Action(parse.json) {
    implicit request =>

      val account = JsonUtil.deserialize[Account](request.body.toString())

      // The ID passed as URL parameter takes precedence
      val accountWithEnforcedId = account.copy(id = Some(id))

      AccountDto.update(accountWithEnforcedId)
      Ok
  }

  def get = Action {
    implicit request =>

      var filtersMap = Map[String, String]()

      if (request.queryString.contains("username")) {
        val username = request.queryString.get("username").get.head
        filtersMap += ("username" -> username)
      }

      if (filtersMap.size == 0)
        Forbidden
      else {
        val matchingUsers = AccountDto.get(Some(filtersMap))

        if (matchingUsers.isEmpty)
          NoContent
        else
          Ok(Json.toJson(matchingUsers))
      }
  }

  def getOfId(id: Long) = Action {
    implicit request =>

      val filters = Some(Map("id" -> id.toString))

      val matchingUsers = AccountDto.get(filters)

      if (matchingUsers.isEmpty)
        NoContent
      else
        Ok(Json.toJson(matchingUsers.head))
  }
}

class UsernameAlreadyTakenException extends Exception