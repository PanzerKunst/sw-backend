package controllers

import play.api.mvc._
import services.AuthHelper
import play.mvc.Http
import play.api.libs.json.Json

object Authenticator extends Controller {
  def generateToken = Action {
    implicit request =>
      AuthHelper.checkTokenRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
          Ok(Json.toJson(AuthHelper.generateToken(request.headers)))
      }
  }

  def revokeToken = Action {
    implicit request =>
      AuthHelper.checkRequestAuthentication(request.method, request.uri, request.headers) match {
        case Http.Status.BAD_REQUEST => BadRequest
        case Http.Status.UNAUTHORIZED => Unauthorized
        case _ =>
          AuthHelper.revokeToken(request.headers)
          Ok
      }
  }
}
