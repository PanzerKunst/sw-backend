package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current
import models.InternetAddress

object BccDto {
  def get(filters: Option[Map[String, String]]): List[InternetAddress] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select address, name
          from bcc """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("BccDto.get():" + query)

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
          insert into bcc(email_id, address, name)
          values(""" + emailId + """,
          """" + internetAddress.email + """",
          """ + nameForQuery + """);"""

        Logger.info("BccDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
