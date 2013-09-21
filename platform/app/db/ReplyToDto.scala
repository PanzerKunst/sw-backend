package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current
import models.InternetAddress

object ReplyToDto {
  def get(filters: Option[Map[String, String]]): List[InternetAddress] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select address, name
          from reply_to """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("ReplyToDto.get():" + query)

        SQL(query)().map(row =>
          new InternetAddress(
            row[String]("address"),
            row[Option[String]]("name")
          )
        ).toList
    }
  }

  def create(emailId: Long, internetAddress: InternetAddress): Option[Long] = {
    DB.withConnection {
      implicit c =>

        var nameForQuery = "NULL"
        if (internetAddress.name.isDefined && internetAddress.name.get != "")
          nameForQuery = "\"" + DbUtil.backslashQuotes(internetAddress.name.get) + "\""

        val query = """
          insert into cc(email_id, address, name)
          values(""" + emailId + """,
          """" + internetAddress.email + """",
          """ + nameForQuery + """);"""

        Logger.info("CcDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
