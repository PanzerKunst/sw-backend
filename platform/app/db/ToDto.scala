package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object ToDto {
  def get(filters: Option[Map[String, String]]): List[String] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select address
          from `to` """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("ToDto.get():" + query)

        SQL(query)().map(row =>
          row[String]("address")
        ).toList
    }
  }

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
