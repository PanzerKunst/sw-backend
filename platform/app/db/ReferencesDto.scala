package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object ReferencesDto {
  def get(filters: Option[Map[String, String]]): List[String] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select references_message_id
          from `references` """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("ReferencesDto.get():" + query)

        SQL(query)().map(row =>
          row[String]("references_message_id")
        ).toList
    }
  }

  def create(emailId: Long, referencesSmtpMessageId: String): Option[Long] = {
    DB.withConnection {
      implicit c =>
        val query = """
                       insert into `references`(email_id, references_message_id)
          values(""" + emailId + """,
                """" + DbUtil.backslashQuotes(referencesSmtpMessageId) + """");"""

        Logger.info("ReferencesDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
