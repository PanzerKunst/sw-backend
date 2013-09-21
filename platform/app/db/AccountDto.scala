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
          select id, first_name, last_name, username, public_key
          from account """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("AccountDto.get():" + query)

        SQL(query)().map(row =>
          Account(
            id = Some(row[BigInteger]("id").longValue),
            firstName = row[String]("first_name"),
            lastName = row[String]("last_name"),
            username = row[String]("username"),
            password = null,
            publicKey = row[String]("public_key")
          )
        ).toList
    }
  }

  def create(account: Account): Option[Long] = {
    DB.withConnection {
      implicit c =>

        val query = """
          insert into account(first_name, last_name, username, password, public_key)
          values("""" + DbUtil.backslashQuotes(account.firstName) + """",
            """" + DbUtil.backslashQuotes(account.lastName) + """",
            """" + DbUtil.backslashQuotes(account.username) + """",
            """" + DbUtil.backslashQuotes(account.password) + """",
            {publicKey});"""

        Logger.info("AccountDto.create():" + query)

        try {
          SQL(query).on("publicKey" -> account.publicKey)
            .executeInsert()
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

        val query = """
          update account set
          first_name = """" + DbUtil.backslashQuotes(account.firstName) + """",
          last_name = """" + DbUtil.backslashQuotes(account.lastName) + """",
          password = """" + DbUtil.backslashQuotes(account.password) + """",
          public_key = {publicKey}
          where id = """ + account.id + """;"""

        Logger.info("AccountDto.update():" + query)

        SQL(query).on("publicKey" -> account.publicKey).executeUpdate()
    }
  }
}
