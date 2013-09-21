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
        select id, subject, textContent, htmlContent, content_type, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status
        from email
        where status = """" + Email.STATUS_TO_SEND + """";"""

    logger.info("EmailDto.getEmailsToSend():" + query)

    try {
      SQL(query)().map(row =>
        Email(
          id = row[BigInteger]("id").longValue(),
          subject = row[Option[String]]("subject"),
          textContent = row[Option[String]]("textContent"),
          htmlContent = row[Option[String]]("htmlContent"),
          contentType = row[String]("content_type"),
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
}
