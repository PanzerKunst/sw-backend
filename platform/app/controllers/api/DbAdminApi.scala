package controllers.api

import db.DbAdmin
import play.api.mvc.{Action, Controller}
import play.Play

object DbAdminApi extends Controller {
  def createTables = Action {
    implicit request =>

      if (request.queryString.contains("key") &&
        request.queryString.get("key").get.head == Play.application().configuration().getString("application.secret")) {
        DbAdmin.createTables()
        DbAdmin.initData()
        Ok
      }
      else
        Forbidden
  }

  def dropTables = Action {
    implicit request =>

      if (request.queryString.contains("key") &&
        request.queryString.get("key").get.head == Play.application().configuration().getString("application.secret")) {
        DbAdmin.dropTables()
        Ok
      }
      else
        Forbidden
  }

}
