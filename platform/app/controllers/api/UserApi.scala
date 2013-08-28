package controllers.api

import models.User
import services.JsonUtil
import play.api.mvc.{Action, Controller}
import db.UserDto
import play.api.Logger

object UserApi extends Controller {
  def create = Action(parse.json) {
    implicit request =>

      val user = JsonUtil.deserialize[User](request.body.toString)
      UserDto.create(user) match {
        case Some(id) => Ok(id.toString)
        case None => InternalServerError("Creation of a user did not return an ID!")
      }
  }

  def update = Action(parse.json) {
    implicit request =>

      val user = JsonUtil.deserialize[User](request.body.toString)
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

      val filters = if (filtersMap.size == 0)
        None
      else
        Some(filtersMap)

      val matchingUsers = UserDto.get(filters)

      if (matchingUsers.isEmpty)
        NoContent
      else
        Ok(JsonUtil.serialize(matchingUsers))
  }
}
