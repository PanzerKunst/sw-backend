package db

import anorm._
import com.typesafe.scalalogging.slf4j.Logging
import scalikejdbc.ConnectionPool

object ReferencesDto extends Logging {
  def getForEmailId(emailId: Long): List[String] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select references_message_id
          from `references`
          where email_id = """ + emailId + """;"""

    logger.info("ReferencesDto.getForEmailId():" + query)

    try {
      SQL(query)().map(row =>
        row[String]("references_message_id")
      ).toList
    }
    finally {
      c.close()
    }
  }
}
