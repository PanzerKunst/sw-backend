package db

import anorm._
import models.Account
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.api.UsernameAlreadyTakenException
import java.math.BigInteger


object AccountDto {
  def get(filters: Option[Map[String, String]]): List[Account] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select id, first_name, last_name, username, public_key, private_key
          from account """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("AccountDto.get():" + query)

        SQL(query)().map(row =>
          Account(
            id = Some(row[BigInteger]("id").longValue),
            firstName = row[String]("first_name"),
            lastName = row[String]("last_name"),
            username = row[String]("username"),
            password = null,
            publicKey = row[String]("public_key"),
            privateKey = row[Option[String]]("private_key")
          )
        ).toList
    }
  }

  def create(account: Account): Option[Long] = {
    DB.withConnection {
      implicit c =>

        val query = """
          insert into account(first_name, last_name, username, password, public_key, private_key)
            values("""" + DbUtil.backslashQuotes(account.firstName) + """",
            """" + DbUtil.backslashQuotes(account.lastName) + """",
            """" + DbUtil.backslashQuotes(account.username) + """",
            """" + DbUtil.backslashQuotes(account.password) + """",
            {publicKey},
            {privateKey});"""

        Logger.info("AccountDto.create():" + query)

        try {
          SQL(query).on(
            "publicKey" -> account.publicKey,
            "privateKey" -> account.privateKey.getOrElse(null)
          ).executeInsert()
        }
        catch {
          case msicve: MySQLIntegrityConstraintViolationException =>
            """Duplicate\sentry.+for\skey\s'unique_username'""".r.findFirstIn(msicve.getMessage) match {
              case Some(foo) => throw new UsernameAlreadyTakenException
              case None => throw msicve
            }
          case e: Exception =>
            throw e
        }
    }
  }

  def update(account: Account) {
    DB.withConnection {
      implicit c =>

        var query = """
          update account set
          first_name = """" + DbUtil.backslashQuotes(account.firstName) + """",
          last_name = """" + DbUtil.backslashQuotes(account.lastName) + """","""

        if (account.password != null) {
          query += """
            password = """" + DbUtil.backslashQuotes(account.password) + """","""
        }

        query += """
          public_key = {publicKey},
          private_key = {privateKey}
          where id = """ + account.id.get + """;"""

        Logger.info("AccountDto.update():" + query)

        SQL(query).on(
          "publicKey" -> account.publicKey,
          "privateKey" -> account.privateKey.getOrElse(null)
        ).executeUpdate()
    }
  }

  def getPasswordForUsername(username: String): Option[String] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select password
          from account where username = '""" + username + "';"

        val rows = SQL(query)()

        if (!rows.isEmpty) {
          val firstRow = rows.head
          Some(firstRow[String]("password"))
        }
        else
          None
    }
  }

  def getAllUsernames: List[String] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select distinct username from account;"""

        Logger.info("AccountDto.getAllUsernames():" + query)

        SQL(query)().map(row =>
          row[String]("username")
        ).toList
    }
  }

  def getOfId(id: Long): Option[Account] = {
    val filters = Some(Map("id" -> id.toString))
    get(filters).headOption
  }

  def getOfUsername(username: String): Option[Account] = {
    val filters = Some(Map("username" -> username))
    get(filters).headOption
  }
}
