package db

import anorm._
import models.User
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.api.UsernameAlreadyTakenException
import java.math.BigInteger


object UserDto {
  def get(filters: Option[Map[String, String]]): List[User] = {
    DB.withConnection {
      implicit c =>

        val query = """
          select id, first_name, last_name, username, public_key
          from user """ + DbUtil.generateWhereClause(filters) + ";"

        Logger.info("UserDto.get():" + query)

        SQL(query)().map(row =>
          User(
            id = Some(row[BigInteger]("id").longValue),
            firstName = row[String]("first_name"),
            lastName = row[String]("last_name"),
            username = row[String]("username"),
            publicKey = row[String]("public_key")
          )
        ).toList
    }
  }

  def create(user: User): Option[Long] = {
    DB.withConnection {
      implicit c =>

        val query = """
                       insert into user(first_name, last_name, username, public_key)
          values("""" + DbUtil.backslashQuotes(user.firstName) + """",
          """" + DbUtil.backslashQuotes(user.lastName) + """",
          """" + DbUtil.backslashQuotes(user.username) + """",
          {publicKey});"""

        Logger.info("UserDto.create():" + query)

        try {
          SQL(query).on("publicKey" -> user.publicKey)
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

  def update(user: User) {
    DB.withConnection {
      implicit c =>

        val query = """
                       update user set
          first_name = """" + DbUtil.backslashQuotes(user.firstName) + """",
          last_name = """" + DbUtil.backslashQuotes(user.lastName) + """",
          public_key = """" + DbUtil.backslashQuotes(user.publicKey) + """";"""

        Logger.info("UserDto.update():" + query)

        SQL(query).executeUpdate()
    }
  }
}
