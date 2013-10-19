package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import models.InternetAddress

object ToDto extends Logging {
  def getForEmailId(emailId: Long): List[InternetAddress] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select address, name
          from `to`
          where email_id = """ + emailId + """;"""

    logger.info("ToDto.getForEmailId():" + query)

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

  def create(emailId: Long, internetAddress: InternetAddress): Option[Long] = {
    implicit val c = ConnectionPool.borrow()

    var nameForQuery = "NULL"
    if (internetAddress.name.isDefined && internetAddress.name.get != "")
      nameForQuery = "\"" + DbUtil.backslashQuotes(internetAddress.name.get) + "\""

    val query = """
          insert into `to`(email_id, address, name)
          values(""" + emailId + """,
          """" + internetAddress.email + """",
          """ + nameForQuery + """);"""

    logger.info("ToDto.create():" + query)

    try {
      SQL(query).executeInsert()
    }
    finally {
      c.close()
    }
  }
}
