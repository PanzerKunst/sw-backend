package db

import anorm._
import models.Email
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import models.clientSide.ClientSideEmail
import java.math.BigInteger

object EmailDto {
  def get(filters: Option[Map[String, String]]): List[Email] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select id, subject, body, content_type, smtp_message_id, smtp_from, smtp_to, smtp_cc, smtp_bcc, smtp_reply_to, smtp_sender, from_user_id, creation_timestamp
          from email """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("EmailDto.get():" + query)

        SQL(query)().map(row =>

          Email(
            id = row[BigInteger]("id").longValue(),
            subject = row[Option[String]]("subject"),
            body = row[Option[String]]("body"),
            contentType = row[String]("content_type"),
            smtpMessageId = row[String]("smtp_message_id"),
            smtpFrom = row[String]("smtp_from"),
            smtpTo = row[Option[String]]("smtp_to"),
            smtpCc = row[Option[String]]("smtp_cc"),
            smtpBcc = row[Option[String]]("smtp_bcc"),
            smtpReplyTo = row[Option[String]]("smtp_reply_to"),
            smtpSender = row[Option[String]]("smtp_sender"),
            fromUserId = row[Option[Long]]("from_user_id"),
            creationTimestamp = row[Long]("creation_timestamp")
          )
        ).toList
    }
  }

  def create(email: ClientSideEmail): Option[Long] = {
    DB.withConnection {
      implicit c =>

        var subjectForQuery = "NULL"
        if (email.subject.isDefined && email.subject.get != "")
          subjectForQuery = "\"" + DbUtil.backslashQuotes(email.subject.get) + "\""

        var bodyForQuery = "NULL"
        if (email.body.isDefined && email.body.get != "")
          bodyForQuery = email.body.get

        var smtpToForQuery = "NULL"
        if (email.smtpTo.isDefined && email.smtpTo.get != "")
          smtpToForQuery = "\"" + DbUtil.backslashQuotes(email.smtpTo.get) + "\""

        var smtpCcForQuery = "NULL"
        if (email.smtpCc.isDefined && email.smtpCc.get != "")
          smtpCcForQuery = "\"" + DbUtil.backslashQuotes(email.smtpCc.get) + "\""

        var smtpBccForQuery = "NULL"
        if (email.smtpBcc.isDefined && email.smtpBcc.get != "")
          smtpBccForQuery = "\"" + DbUtil.backslashQuotes(email.smtpBcc.get) + "\""

        var smtpReplyToForQuery = "NULL"
        if (email.smtpReplyTo.isDefined && email.smtpReplyTo.get != "")
          smtpReplyToForQuery = "\"" + DbUtil.backslashQuotes(email.smtpReplyTo.get) + "\""

        var smtpSenderForQuery = "NULL"
        if (email.smtpSender.isDefined && email.smtpSender.get != "")
          smtpSenderForQuery = "\"" + DbUtil.backslashQuotes(email.smtpSender.get) + "\""

        val query = """
                       insert into email(subject, body, content_type, smtp_message_id, smtp_from, smtp_to, smtp_cc, smtp_bcc, smtp_reply_to, smtp_sender, from_user_id, creation_timestamp)
          values(""" + subjectForQuery + """,
          {body},
          """" + email.contentType + """",
          """" + Email.generateSmtpMessageId() + """",
          """" + email.smtpFrom + """",
          """ + smtpToForQuery + """,
          """ + smtpCcForQuery + """,
          """ + smtpBccForQuery + """,
          """ + smtpReplyToForQuery + """,
          """ + smtpSenderForQuery + """,
          """ + email.fromUserId.getOrElse("NULL") + """,
          """ + email.creationTimestamp + """);"""

        Logger.info("EmailDto.create():" + query)

        // We need to use "on" otherwise the new lines inside the "bodyForQuery" are removed
        SQL(query).on("body" -> bodyForQuery)
          .executeInsert()
    }
  }
}
