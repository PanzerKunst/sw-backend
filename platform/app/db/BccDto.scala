package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object BccDto {
  def create(emailId: Long, address: String): Option[Long] = {
    DB.withConnection {
      implicit c =>
        val query = """
                       insert into bcc(email_id, address)
          values(""" + emailId + """,
          """" + address + """");"""

        Logger.info("BccDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
