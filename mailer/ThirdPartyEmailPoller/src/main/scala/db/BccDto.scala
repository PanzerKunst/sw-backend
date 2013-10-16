package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import models.InternetAddress

object BccDto extends Logging {
  def getForEmailId(emailId: Long): List[InternetAddress] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select address, name
          from bcc
          where email_id = """ + emailId + """;"""

    logger.info("BccDto.getForEmailId():" + query)

    try {
      SQL(query)().map(row =>
        InternetAddress(
          email = row[String]("address"),
          name = row[Option[String]]("name")
        )
      ).toList
    }
    finally {
      c.close()
    }
  }
}
