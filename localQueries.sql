USE steelwrapped;

DROP TABLE smpt_in_reply_to;
DROP TABLE smpt_references;
DROP TABLE bcc;
DROP TABLE cc;
DROP TABLE `to`;
DROP TABLE email;
DROP TABLE USER;

CREATE TABLE USER (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(64) NOT NULL,
    last_name VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    public_key VARCHAR(10240) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX unique_username (username)
  ) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE email (
            id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
            SUBJECT VARCHAR(128),
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

CREATE TABLE `to` (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email_id BIGINT UNSIGNED NOT NULL,
  address VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_email_to FOREIGN KEY (email_id) REFERENCES email (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE cc (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email_id BIGINT UNSIGNED NOT NULL,
  address VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_email_cc FOREIGN KEY (email_id) REFERENCES email (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE bcc (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email_id BIGINT UNSIGNED NOT NULL,
  address VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_email_bcc FOREIGN KEY (email_id) REFERENCES email (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE smpt_references (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email_id BIGINT UNSIGNED NOT NULL,
  references_email_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_email_smtp_references FOREIGN KEY (email_id) REFERENCES email (id),
  CONSTRAINT fk_references_email_smtp_references FOREIGN KEY (references_email_id) REFERENCES email (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

CREATE TABLE smpt_in_reply_to (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  email_id BIGINT UNSIGNED NOT NULL,
  reply_to_email_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_email_smtp_in_reply_to FOREIGN KEY (email_id) REFERENCES email (id),
  CONSTRAINT fk_reply_to_smtp_in_reply_to FOREIGN KEY (reply_to_email_id) REFERENCES email (id)
) ENGINE=INNODB DEFAULT CHARSET=utf8;


SELECT * FROM USER;
SELECT * FROM email;

DELETE FROM USER;