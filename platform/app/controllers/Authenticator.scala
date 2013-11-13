package controllers

import play.api.mvc._
import play.mvc.Http
import play.api.libs.json.Json
import services.auth.helper.AuthHelper
import services.GlobalServices

object Authenticator extends Controller {
  def generateToken = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkTokenRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else
        Ok(Json.toJson(GlobalServices.authHelper.generateToken(request.headers)))
  }

  def revokeToken = Action {
    implicit request =>
      val authenticationResult = GlobalServices.authHelper.checkRequestAuthentication(request.method, request.uri, request.headers)

      if (authenticationResult.httpReturnCode == Http.Status.BAD_REQUEST)
        BadRequest
      else if (authenticationResult.httpReturnCode == Http.Status.UNAUTHORIZED)
        Unauthorized(authenticationResult.errorMessage.get)
      else {
        GlobalServices.authHelper.revokeToken(request.headers)
        Ok
      }
  }
}
