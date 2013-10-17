package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import models.{InternetAddress, Email}
import java.math.BigInteger
import javax.security.auth.login.AccountNotFoundException
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import models.clientside.ClientSideEmail

object EmailDto extends Logging {
  def getEmailsToSend: List[Email] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
        select id, subject, text_content, html_content, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status
        from email
        where status = """" + Email.STATUS_TO_SEND + """";"""

    // Commented out because was spamming too much
    // logger.info("EmailDto.getEmailsToSend():" + query)

    try {
      SQL(query)().map(row =>
        Email(
          id = row[BigInteger]("id").longValue(),
          subject = row[Option[String]]("subject"),
          textContent = row[Option[String]]("text_content"),
          htmlContent = row[Option[String]]("html_content"),
          messageId = row[String]("message_id"),
          from = InternetAddress(
            email = row[String]("from_address"),
            name = row[Option[String]]("from_name")
          ),
          sender = row[Option[String]]("sender_address") match {
            case Some(address) => Some(InternetAddress(
              email = address,
              name = row[Option[String]]("sender_name")
            ))
            case None => None
          },
          fromAccountId = row[Option[BigInteger]]("from_account_id") match {
            case Some(fromAccountId) => Some(fromAccountId.longValue())
            case None => None
          },
          creationTimestamp = row[Long]("creation_timestamp"),
          status = Email.STATUS_TO_SEND
        )
      ).toList
    }
    finally {
      c.close()
    }
  }

  def create(email: ClientSideEmail): Option[Long] = {
    implicit val c = ConnectionPool.borrow()

    var subjectForQuery = "NULL"
    if (email.subject.isDefined && email.subject.get != "")
      subjectForQuery = "\"" + DbUtil.backslashQuotes(email.subject.get) + "\""

    var fromNameForQuery = "NULL"
    if (email.from.name.isDefined && email.from.name.get != "")
      fromNameForQuery = "\"" + DbUtil.backslashQuotes(email.from.name.get) + "\""

    var senderAddressForQuery = "NULL"
    var senderNameForQuery = "NULL"
    if (email.sender.isDefined) {
      val sender = email.sender.get
      senderAddressForQuery = "\"" + DbUtil.backslashQuotes(sender.email) + "\""

      if (sender.name.isDefined) {
        senderNameForQuery = "\"" + DbUtil.backslashQuotes(sender.name.get) + "\""
      }
    }

    val query = """
                       insert into email(subject, text_content, html_content, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status)
          values(""" + subjectForQuery + """,
          {textContent},
          {htmlContent},
          """" + Email.generateMessageId() + """",
          """" + email.from.email + """",
          """ + fromNameForQuery + """,
          """ + senderAddressForQuery + """,
          """ + senderNameForQuery + """,
          """ + email.fromAccountId.getOrElse("NULL") + """,
          """ + email.creationTimestamp + """,
          """" + email.status + """");"""

    logger.info("EmailDto.create():" + query)

    try {
      // We need to use "on" otherwise the new lines inside the "textContentForQuery" and "htmlContentForQuery" are removed
      SQL(query).on(
        "textContent" -> email.textContent,
        "htmlContent" -> email.htmlContent
      ).executeInsert()
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
    finally {
      c.close()
    }
  }
}
