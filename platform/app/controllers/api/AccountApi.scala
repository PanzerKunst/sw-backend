package controllers.api

import models.Account
import services.{GlobalServices, JsonUtil}
import play.api.mvc.{Action, Controller}
import db.{PostfixAccountDto, AccountDto}
import play.api.libs.json.Json
import play.mvc.Http

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
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        // Let's start by retrieving the account in DB for that ID
        AccountDto.getOfId(id) match {
          case None => BadRequest("No account found for ID " + id)
          case Some(accountInDb) =>
            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            if (clientIdentifier != accountInDb.username)
              Forbidden("Account with ID '" + id + "' is not yours")
            else {
              val accountInRequest = JsonUtil.deserialize[Account](request.body.toString())

              if (accountInDb.username != accountInRequest.username) {
                Forbidden("You're not allowed to change username")
              }
              else if (id != accountInRequest.id.get) {
                Forbidden("The ID in the URL doesn't match the one in the JSON")
              }
              else {
                AccountDto.update(accountInRequest)
                Ok(Json.toJson(accountInRequest))
              }
            }
        }
      }
  }

  def get = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        var filtersMap = Map[String, String]()

        if (request.queryString.contains("username")) {
          val username = request.queryString.get("username").get.head
          filtersMap += ("username" -> username)
        }

        if (filtersMap.size == 0)
          Forbidden("You need to specify 'username' as a query string")
        else {
          val matchingAccounts = AccountDto.get(Some(filtersMap))

          if (matchingAccounts.isEmpty)
            NoContent
          else {
            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            // We hide privateKey if the caller is not the same account
            val accountsToReturn = for (account <- matchingAccounts) yield {
              if (clientIdentifier != account.username)
                account.copy(privateKey = None)
              else
                account
            }

            Ok(Json.toJson(accountsToReturn))
          }
        }
      }
  }

  def getOfId(id: Long) = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else
        AccountDto.getOfId(id) match {
          case None => NoContent
          case Some(account) =>
            val authorizationHeader = GlobalServices.authHelper.getAuthorizationHeader(request.headers).get
            val clientIdentifier = GlobalServices.authHelper.getSubHeader(authorizationHeader, "oauth_consumer_key").get

            // We hide privateKey if the caller is not the same account
            val accountToReturn = if (clientIdentifier != account.username)
              account.copy(privateKey = None)
            else
              account

            Ok(Json.toJson(accountToReturn))
        }
  }
}

class UsernameAlreadyTakenException extends Exception