package db

import play.api.db.DB
import anorm._
import play.api.Play.current

object DbAdmin {
  def createTables() {
    createTableUser()
  }

  def dropTables() {
    dropTableUser()
  }

  def initData() {
  }

  private def createTableUser() {
    DB.withConnection {
      implicit c =>

        val query = """
        CREATE TABLE `user` (
          `id` int unsigned NOT NULL AUTO_INCREMENT,
          `first_name` varchar(45) NOT NULL,
          `last_name` varchar(45) NOT NULL,
          `username` varchar(45) NOT NULL,
          `public_key` varchar(10240) NOT NULL,
          PRIMARY KEY (`id`,`username`) USING BTREE
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
        """

        SQL(query).executeUpdate()
    }
  }

  private def dropTableUser() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists user;").executeUpdate()
    }
  }
}
