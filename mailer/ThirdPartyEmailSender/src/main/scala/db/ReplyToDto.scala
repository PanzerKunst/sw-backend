package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import javax.mail.internet.InternetAddress

object ReplyToDto extends Logging {
  def getForEmailId(emailId: Long): List[InternetAddress] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select address, name
          from reply_to
          where email_id = """ + emailId + """;"""

    logger.info("ReplyToDto.getForEmailId():" + query)

    try {
      SQL(query)().map(row =>
        new InternetAddress(
          row[String]("address"),
          row[Option[String]]("name") match {
            case Some(name) => name
            case None => null
          }
        )
      ).toList
    }
    finally {
      c.close()
    }
  }
}
