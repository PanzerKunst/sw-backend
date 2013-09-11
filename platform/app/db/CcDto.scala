package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object CcDto {
  def create(emailId: Long, address: String): Option[Long] = {
    DB.withConnection {
      implicit c =>
        val query = """
                       insert into cc(email_id, address)
          values(""" + emailId + """,
          """" + address + """");"""

        Logger.info("CcDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
