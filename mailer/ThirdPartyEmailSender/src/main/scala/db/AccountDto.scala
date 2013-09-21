package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging

object AccountDto extends Logging {
  def getUsernameAndPasswordForId(id: Long): (String, String) = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select username, password
          from account
          where id = """ + id + """;"""

    logger.info("AccountDto.getUsernameAndPasswordForId():" + query)

    try {
      val firstRow = SQL(query)().head

      (
        firstRow[String]("username"),
        firstRow[String]("password")
        )
    }
    finally {
      c.close()
    }
  }
}
