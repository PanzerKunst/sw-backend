package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current
import java.math.BigInteger

object SmtpReferencesDto {
  def get(filters: Option[Map[String, String]]): List[Long] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select references_email_id
          from smtp_references """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("SmtpReferencesDto.get():" + query)

        SQL(query)().map(row =>
          row[BigInteger]("references_email_id").longValue()
        ).toList
    }
  }

  def create(emailId: Long, referencesEmailId: Long): Option[Long] = {
    DB.withConnection {
      implicit c =>
        val query = """
                       insert into smtp_references(email_id, references_email_id)
          values(""" + emailId + """,
          """ + referencesEmailId + """);"""

        Logger.info("SmtpReferencesDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
