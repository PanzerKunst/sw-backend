package db

import play.api.db.DB
import play.api.Logger
import anorm._
import play.api.Play.current

object SmtpReferencesDto {
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
