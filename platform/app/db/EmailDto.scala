package db

import anorm._
import models.Email
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import models.clientSide.ClientSideEmail
import java.math.BigInteger
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import javax.security.auth.login.AccountNotFoundException

object EmailDto {
  def get(filters: Option[Map[String, String]]): List[Email] = {
    DB.withConnection {
      implicit c =>

        val query = """
          SELECT e.*,
          `to`.address AS to_address,
          cc.address AS cc_address,
          bcc.address AS bcc_address,
          r.references_email_id
          FROM email e
          LEFT JOIN `to` ON `to`.email_id = e.id
          LEFT JOIN cc ON cc.email_id = e.id
          LEFT JOIN bcc ON bcc.email_id = e.id
          LEFT JOIN smtp_references r ON r.email_id = e.id """ + DbUtil.generateWhereClause(filters) + ";"

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
            fromAccountId = row[Option[BigInteger]]("from_account_id") match {
              case Some(fromAccountId) => Some(fromAccountId.longValue())
              case None => None
            },
            creationTimestamp = row[Long]("creation_timestamp"),
            status = row[String]("status")
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
                       insert into email(subject, body, content_type, smtp_message_id, smtp_from, smtp_to, smtp_cc, smtp_bcc, smtp_reply_to, smtp_sender, from_account_id, creation_timestamp, status)
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
          """ + email.fromAccountId.getOrElse("NULL") + """,
          """ + email.creationTimestamp + """,
          """" + email.status + """");"""

        Logger.info("EmailDto.create():" + query)

        try {
          // We need to use "on" otherwise the new lines inside the "bodyForQuery" are removed
          SQL(query).on("body" -> bodyForQuery)
            .executeInsert()
        }
        catch {
          case msicve: MySQLIntegrityConstraintViolationException =>
            """CONSTRAINT\s`fk_account`\sFOREIGN\sKEY\s\(`from_account_id`\)\sREFERENCES\s`account`\s\(`id`\)""".r.findFirstIn(msicve.getMessage) match {
              case Some(foo) => throw new AccountNotFoundException
              case None => throw msicve
            }
          case e: Exception =>
            throw e
        }
    }
  }
}
