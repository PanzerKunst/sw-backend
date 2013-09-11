package db

import play.api.db.DB
import anorm._
import play.api.Play.current
import play.api.Logger

object DbAdmin {
  def createTables() {
    createTableAccount()
    createTableEmail()
    createTableTo()
    createTableCc()
    createTableBcc()
    createTableSmtpReferences()
    createTableSmtpInReplyTo()
  }

  def dropTables() {
    dropTableSmtpInReplyTo()
    dropTableSmtpReferences()
    dropTableBcc()
    dropTableCc()
    dropTableTo()
    dropTableEmail()
    dropTableAccount()
  }

  def initData() {
  }

  private def createTableAccount() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE account (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            first_name VARCHAR(64) NOT NULL,
            last_name VARCHAR(64) NOT NULL,
            username VARCHAR(64) NOT NULL,
            public_key VARCHAR(10240) NOT NULL,
            PRIMARY KEY (id),
            UNIQUE INDEX unique_username (username)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        Logger.info("DbAdmin.createTableAccount():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableEmail() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE email (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            subject VARCHAR(128),
            body TEXT,
            content_type VARCHAR(128) NOT NULL,
            smtp_message_id VARCHAR(128) NOT NULL,
            smtp_from VARCHAR(128) NOT NULL,
            smtp_to VARCHAR(5120),
            smtp_cc VARCHAR(5120),
            smtp_bcc VARCHAR(5120),
            smtp_reply_to VARCHAR(64),
            smtp_sender VARCHAR(128),
            from_account_id BIGINT UNSIGNED,
            creation_timestamp INT UNSIGNED NOT NULL,
            status VARCHAR(16), /* DRAFT, SENT, UNREAD, READ, ARCHIVED */
            PRIMARY KEY (id),
            UNIQUE INDEX unique_smtp_message_id (smtp_message_id),
            CONSTRAINT fk_account FOREIGN KEY (from_account_id) REFERENCES account (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
                    """

        Logger.info("DbAdmin.createTableEmail():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableTo() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE `to` (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            address VARCHAR(64) NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_to FOREIGN KEY (email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
                    """

        Logger.info("DbAdmin.createTableTo():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableCc() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE cc (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            address VARCHAR(64) NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_cc FOREIGN KEY (email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        Logger.info("DbAdmin.createTableCc():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableBcc() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE bcc (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            address VARCHAR(64) NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_bcc FOREIGN KEY (email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        Logger.info("DbAdmin.createTableBcc():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableSmtpReferences() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE smtp_references (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            references_email_id BIGINT UNSIGNED NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_smtp_references FOREIGN KEY (email_id) REFERENCES email (id),
            CONSTRAINT fk_references_email_smtp_references FOREIGN KEY (references_email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        Logger.info("DbAdmin.createTableSmtpReferences():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def createTableSmtpInReplyTo() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE smtp_in_reply_to (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            reply_to_email_id BIGINT UNSIGNED NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_smtp_in_reply_to FOREIGN KEY (email_id) REFERENCES email (id),
            CONSTRAINT fk_reply_to_smtp_in_reply_to FOREIGN KEY (reply_to_email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        Logger.info("DbAdmin.createTableSmtpInReplyTo():" + query)

        SQL(query).executeUpdate()
    }
  }

  private def dropTableAccount() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists account;"
        Logger.info("DbAdmin.dropTableAccount(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableEmail() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists email;"
        Logger.info("DbAdmin.dropTableEmail(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableTo() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists `to`;"
        Logger.info("DbAdmin.dropTableTo(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableCc() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists cc;"
        Logger.info("DbAdmin.dropTableCc(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableBcc() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists bcc;"
        Logger.info("DbAdmin.dropTableBcc(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableSmtpReferences() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists smtp_references;"
        Logger.info("DbAdmin.dropTableSmtpReferences(): " + query)
        SQL(query).executeUpdate()
    }
  }

  private def dropTableSmtpInReplyTo() {
    DB.withConnection {
      implicit c =>
        val query = "drop table if exists smtp_in_reply_to;"
        Logger.info("DbAdmin.SmtpInReplyTo(): " + query)
        SQL(query).executeUpdate()
    }
  }
}
