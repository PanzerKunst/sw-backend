package db

import anorm._
import models.User
import play.api.db.DB
import play.api.Play.current
import play.api.Logger


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
            id = Some(row[Long]("id")),
            firstName = Some(row[String]("first_name")),
            lastName = Some(row[String]("last_name")),
            username = Some(row[String]("username")),
            publicKey = Some(row[String]("public_key"))
          )
        ).toList
    }
  }

  def create(user: User): Option[Long] = {
    DB.withConnection {
      implicit c =>

        val query = """
                       insert into user(first_name, last_name, username, public_key)
          values("""" + DbUtil.backslashQuotes(user.firstName.get) + """", """" +
          DbUtil.backslashQuotes(user.lastName.get) + """", """" +
          DbUtil.backslashQuotes(user.username.get) + """", {publicKey});"""

        Logger.info("UserDto.create():" + query)

        SQL(query).on("publicKey" -> user.publicKey.get)
          .executeInsert()
    }
  }

  def update(user: User) {
    DB.withConnection {
      implicit c =>

        val query = """
                       update user set
          first_name = """" + DbUtil.backslashQuotes(user.firstName.get) + """",
          last_name = """" + DbUtil.backslashQuotes(user.lastName.get) + """",
          public_key = """" + DbUtil.backslashQuotes(user.publicKey.get) + """";"""

        Logger.info("UserDto.update():" + query)

        SQL(query).executeUpdate()
    }
  }
}
