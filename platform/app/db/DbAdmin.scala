package db

import play.api.db.DB
import anorm._
import play.api.Play.current

object DbAdmin {
  def createTables() {
    createTableUser()
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
    dropTableUser()
  }

  def initData() {
  }

  private def createTableUser() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE USER (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            first_name VARCHAR(64) NOT NULL,
            last_name VARCHAR(64) NOT NULL,
            username VARCHAR(64) NOT NULL,
            public_key VARCHAR(10240) NOT NULL,
            PRIMARY KEY (id),
            UNIQUE INDEX unique_username (username)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

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
            from_user_id BIGINT UNSIGNED,
            creation_timestamp INT UNSIGNED NOT NULL,
            PRIMARY KEY (id),
            UNIQUE INDEX unique_smtp_message_id (smtp_message_id),
            CONSTRAINT fk_user FOREIGN KEY (from_user_id) REFERENCES USER (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

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

        SQL(query).executeUpdate()
    }
  }

  private def createTableSmtpReferences() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE smpt_references (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            references_email_id BIGINT UNSIGNED NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_smtp_references FOREIGN KEY (email_id) REFERENCES email (id),
            CONSTRAINT fk_references_email_smtp_references FOREIGN KEY (references_email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
        """

        SQL(query).executeUpdate()
    }
  }

  private def createTableSmtpInReplyTo() {
    DB.withConnection {
      implicit c =>

        val query = """
          CREATE TABLE smpt_in_reply_to (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            email_id BIGINT UNSIGNED NOT NULL,
            reply_to_email_id BIGINT UNSIGNED NOT NULL,
            PRIMARY KEY (id),
            CONSTRAINT fk_email_smtp_in_reply_to FOREIGN KEY (email_id) REFERENCES email (id),
            CONSTRAINT fk_reply_to_smtp_in_reply_to FOREIGN KEY (reply_to_email_id) REFERENCES email (id)
          ) ENGINE=INNODB DEFAULT CHARSET=utf8;
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

  private def dropTableEmail() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists email;").executeUpdate()
    }
  }

  private def dropTableTo() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists to;").executeUpdate()
    }
  }

  private def dropTableCc() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists cc;").executeUpdate()
    }
  }

  private def dropTableBcc() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists bcc;").executeUpdate()
    }
  }

  private def dropTableSmtpReferences() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists smtp_references;").executeUpdate()
    }
  }

  private def dropTableSmtpInReplyTo() {
    DB.withConnection {
      implicit c =>
        SQL("drop table if exists smtp_in_reply_to;").executeUpdate()
    }
  }
}
