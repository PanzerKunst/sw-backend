package controllers.api

import models.Account
import services.{AuthHelper, JsonUtil}
import play.api.mvc.{Action, Controller}
import db.{PostfixAccountDto, AccountDto}
import play.api.libs.json.Json
import play.mvc.Http

object AccountApi extends Controller {
  val HTTP_STATUS_CODE_USERNAME_ALREADY_TAKEN = 520

  def create = Action(parse.json) {
    implicit request =>
      AuthHelper.checkRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
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
  }

  def update(id: Long) = Action(parse.json) {
    implicit request =>
      AuthHelper.checkRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
          val account = JsonUtil.deserialize[Account](request.body.toString())

          // The ID passed as URL parameter takes precedence
          val accountWithEnforcedId = account.copy(id = Some(id))

          AccountDto.update(accountWithEnforcedId)
          Ok
      }
  }

  def get = Action {
    implicit request =>
      AuthHelper.checkRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
          var filtersMap = Map[String, String]()

          if (request.queryString.contains("username")) {
            val username = request.queryString.get("username").get.head
            filtersMap += ("username" -> username)
          }

          if (filtersMap.size == 0)
            Forbidden
          else {
            val matchingAccounts = AccountDto.get(Some(filtersMap))

            if (matchingAccounts.isEmpty)
              NoContent
            else
              Ok(Json.toJson(matchingAccounts))
          }
      }
  }

  def getOfId(id: Long) = Action {
    implicit request =>
      AuthHelper.checkRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
          val filters = Some(Map("id" -> id.toString))

          val matchingAccounts = AccountDto.get(filters)

          if (matchingAccounts.isEmpty)
            NoContent
          else
            Ok(Json.toJson(matchingAccounts.head))
      }
  }
}

class UsernameAlreadyTakenException extends Exception