package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import models.Email
import java.math.BigInteger
import javax.mail.internet.InternetAddress

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
          from = new InternetAddress(
            row[String]("from_address"),
            row[Option[String]]("from_name") match {
              case Some(fromName) => fromName
              case None => null
            }
          ),
          sender = row[Option[String]]("sender_address") match {
            case Some(address) => Some(new InternetAddress(
              address,
              row[Option[String]]("sender_name") match {
                case Some(senderName) => senderName
                case None => null
              }
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

  def update(email: Email) {
    implicit val c = ConnectionPool.borrow()

    var query = """
      update email set
      text_content={textContent},
      html_content={htmlContent},
      message_id="""" + email.messageId + """",
      from_address="""" + email.from.getAddress + """",
      creation_timestamp=""" + email.creationTimestamp + """,
      status="""" + email.status + """""""

    email.subject match {
      case Some(subject) => query += """, subject="""" + subject + """""""
      case None => query += """, subject=NULL"""
    }

    if (email.from.getPersonal != null)
      query += """, from_name="""" + email.from.getPersonal + """""""
    else
      query += """, from_name=NULL"""

    email.sender match {
      case Some(sender) =>
        query += """, sender_address="""" + sender.getAddress + """""""
        if (sender.getPersonal != null)
          query += """, sender_name="""" + sender.getPersonal + """""""
        else
          query += """, sender_name=NULL"""
      case None => query += """, sender_address=NULL, sender_name=NULL"""
    }

    email.fromAccountId match {
      case Some(accountId) => query += """, from_account_id=""" + accountId
      case None => query += """, from_accound_id=NULL"""
    }

    query += """
      where id=""" + email.id + """;"""

    logger.info("EmailDto.update():" + query)

    try {
      SQL(query).on(
        "textContent" -> email.textContent,
        "htmlContent" -> email.htmlContent
      ).executeUpdate()
    }
    finally {
      c.close()
    }
  }
}
