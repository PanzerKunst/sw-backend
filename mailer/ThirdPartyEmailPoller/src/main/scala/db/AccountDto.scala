package db

import anorm._
import scalikejdbc.ConnectionPool
import com.typesafe.scalalogging.slf4j.Logging
import java.math.BigInteger
import models.{Email, Account}
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import javax.security.auth.login.AccountNotFoundException

object AccountDto extends Logging {
  def get(filters: Option[Map[String, String]]): List[Account] = {
    implicit val c = ConnectionPool.borrow()

    val query = """
          select id, first_name, last_name, username, public_key
          from account """ + DbUtil.generateWhereClause(filters) + ";"

    logger.info("AccountDto.get():" + query)

    try {
      SQL(query)().map(row =>
        Account(
          id = row[BigInteger]("id").longValue,
          firstName = row[String]("first_name"),
          lastName = row[String]("last_name"),
          username = row[String]("username"),
          password = null,
          publicKey = row[String]("public_key")
        )
      ).toList
    }
    finally {
      c.close()
    }
  }

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

  /*def create(email: ClientSideEmail): Option[Long] = {
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
                       insert into email(subject, text_content, html_content, content_type, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status)
          values(""" + subjectForQuery + """,
          {textContent},
          {htmlContent},
                                         """" + email.contentType + """",
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
  }*/
}
