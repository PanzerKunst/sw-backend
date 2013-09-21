package db

import anorm._
import models.Account
import play.api.db.DB
import play.api.Play.current
import play.api.Logger
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import controllers.api.UsernameAlreadyTakenException
import java.math.BigInteger
import play.Play


object PostfixAccountDto {
  def create(account: Account , accountId: Long): Option[Long] = {
    DB.withConnection("mailer") {
      implicit c =>

        val query = """
          insert into mailbox(username, password, name, maildir, domain, account_id)
          values("""" + DbUtil.backslashQuotes(account.username) + """",
            encrypt("""" + DbUtil.backslashQuotes(account.password) + """"),
            """" + DbUtil.backslashQuotes(account.firstName + " " + account.lastName) + """",
            """" + DbUtil.backslashQuotes(account.username + "/") + """",
            """" + DbUtil.backslashQuotes(Play.application().configuration().getString("email.domain")) + """",
            """ + accountId + """);"""

        Logger.info("PostfixAccountDto.create():" + query)

        SQL(query).executeInsert()
    }
  }
}
