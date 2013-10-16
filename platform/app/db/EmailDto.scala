package db

import anorm._
import models.{InternetAddress, Email}
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import models.clientside.ClientSideEmail
import java.math.BigInteger
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import javax.security.auth.login.AccountNotFoundException
import play.Play

object EmailDto {
  def get(filters: Option[Map[String, String]]): List[Email] = {
    DB.withConnection {
      implicit c =>

        val query = """
          SELECT id, subject, text_content, html_content, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status
          FROM email """ + DbUtil.generateWhereClause(filters) + """
          order by creation_timestamp desc;"""

        Logger.info("EmailDto.get():" + query)

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
            status = row[String]("status")
          )
        ).toList
    }
  }

  def getEmailsFromAccount(accountId: Long, statuses: List[String]): List[Email] = {
    DB.withConnection {
      implicit c =>

        var statusesForQuery = "\"" + DbUtil.backslashQuotes(statuses.apply(0)) + "\""
        for (i <- 1 to statuses.length - 1) {
          statusesForQuery += ", \"" + DbUtil.backslashQuotes(statuses.apply(i)) + "\""
        }

        val query = """
          SELECT id, subject, text_content, html_content, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status
          FROM email
          WHERE from_account_id = """ + accountId + """
          AND status in (""" + statusesForQuery + """)
          order by creation_timestamp desc;"""

        Logger.info("EmailDto.getEmailsFromAccount():" + query)

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
            status = row[String]("status")
          )
        ).toList
    }
  }

  def getEmailsToAccount(accountId: Long, statuses: List[String]): List[Email] = {
    DB.withConnection {
      implicit c =>

        val accountFilters = Some(Map("id" -> accountId.toString))
        val username = AccountDto.get(accountFilters).head.username

        var statusesForQuery = "\"" + DbUtil.backslashQuotes(statuses.apply(0)) + "\""
        for (i <- 1 to statuses.length - 1) {
          statusesForQuery += ", \"" + DbUtil.backslashQuotes(statuses.apply(i)) + "\""
        }

        val queryToGetEmailIds = """
          SELECT e.id,
          `to`.address to_address,
          cc.address cc_address,
          bcc.address bcc_address
            FROM email e
          LEFT JOIN `to` ON `to`.email_id = e.id
          LEFT JOIN cc ON cc.email_id = e.id
          LEFT JOIN bcc ON bcc.email_id = e.id
          WHERE (`to`.address = """" + username + "@" + Play.application().configuration().getString("email.domain") + """"
            OR cc.address = """" + username + "@" + Play.application().configuration().getString("email.domain") + """"
            OR bcc.address = """" + username + "@" + Play.application().configuration().getString("email.domain") + """")
          and e.status IN(""" + statusesForQuery + """);"""

        Logger.info("EmailDto.getEmailsToAccount().queryToGetEmailIds:" + queryToGetEmailIds)

        val emailIds = SQL(queryToGetEmailIds)().map(row =>
          row[BigInteger]("id").longValue()
        ).toList

        if (emailIds.isEmpty) {
          List()
        }
        else {
          var idsForQuery = emailIds.apply(0).toString
          for (i <- 1 to emailIds.length - 1) {
            idsForQuery += ", " + emailIds.apply(i)
          }

          val query = """
          SELECT id, subject, text_content, html_content, message_id, from_address, from_name, sender_address, sender_name, from_account_id, creation_timestamp, status
          FROM email where id in (""" + idsForQuery + """)
          order by creation_timestamp desc;"""

          Logger.info("EmailDto.getEmailsToAccount().query:" + query)

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
              status = row[String]("status")
            )
          ).toList
        }
    }
  }

  def create(email: ClientSideEmail): Option[Long] = {
    DB.withConnection {
      implicit c =>

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

        Logger.info("EmailDto.create():" + query)

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
    }
  }
}
