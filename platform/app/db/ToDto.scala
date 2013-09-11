package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object ToDto {
  def create(emailId: Long, address: String): Option[Long] = {
    DB.withConnection {
      implicit c =>
        val query = """
                       insert into `to`(email_id, address)
          values(""" + emailId + """,
          """" + address + """");"""

        Logger.info("ToDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
