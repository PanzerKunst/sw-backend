package controllers.api

import models.User
import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db.UserDto
import play.api.libs.json.Json

object UserApi extends Controller {
  val HTTP_STATUS_CODE_USERNAME_ALREADY_TAKEN = 520

  def create = Action(parse.json) {
    implicit request =>

      val user = JsonUtil.deserialize[User](request.body.toString())
      try {
        UserDto.create(user) match {
          case Some(id) => Ok(id.toString)
          case None => InternalServerError("Creation of a user did not return an ID!")
        }
      }
      catch {
        case uate: UsernameAlreadyTakenException => Status(HTTP_STATUS_CODE_USERNAME_ALREADY_TAKEN)
        case e: Exception => InternalServerError(e.getMessage)
      }
  }

  def update = Action(parse.json) {
    implicit request =>

      val user = JsonUtil.deserialize[User](request.body.toString())
      UserDto.update(user)
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
        val matchingUsers = UserDto.get(Some(filtersMap))

        if (matchingUsers.isEmpty)
          NoContent
        else
          Ok(Json.toJson(matchingUsers))
      }
  }

  def getOfId(id: Int) = Action {
    implicit request =>

      val filters = Some(Map("id" -> id.toString))

      val matchingUsers = UserDto.get(filters)

      if (matchingUsers.isEmpty)
        NoContent
      else
        Ok(Json.toJson(matchingUsers.head))
  }
}

class UsernameAlreadyTakenException extends Exception