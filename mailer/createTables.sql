CREATE DATABASE postfix;

GRANT SELECT ON postfix.* TO postfix@localhost IDENTIFIED BY '34ig1su4';
GRANT SELECT, INSERT, DELETE, UPDATE ON postfix.* TO postfixadmin@localhost IDENTIFIED BY '34ig1su4';

USE postfix;

CREATE TABLE admin (
  `username` varchar(255) NOT NULL default '',
  `password` varchar(255) NOT NULL default '',
  `created` datetime NOT NULL default '0000-00-00 00:00:00',
  `modified` datetime NOT NULL default '0000-00-00 00:00:00',
  `active` tinyint(1) NOT NULL default '1',
  PRIMARY KEY  (`username`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Virtual Admins';

CREATE TABLE alias (
  `address` varchar(255) NOT NULL default '',
  `goto` text NOT NULL,
  `domain` varchar(255) NOT NULL default '',
  `created` datetime NOT NULL default '0000-00-00 00:00:00',
  `modified` datetime NOT NULL default '0000-00-00 00:00:00',
  `active` tinyint(1) NOT NULL default '1',
  PRIMARY KEY  (`address`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Virtual Aliases';

CREATE TABLE domain (
  `domain` varchar(255) NOT NULL default '',
  `description` varchar(255) NOT NULL default '',
  `aliases` int(10) NOT NULL default '0',
  `mailboxes` int(10) NOT NULL default '0',
  `maxquota` bigint(20) NOT NULL default '0',
  `quota` bigint(20) NOT NULL default '0',
  `transport` varchar(255) default NULL,
  `backupmx` tinyint(1) NOT NULL default '0',
  `created` datetime NOT NULL default '0000-00-00 00:00:00',
  `modified` datetime NOT NULL default '0000-00-00 00:00:00',
  `active` tinyint(1) NOT NULL default '1',
  PRIMARY KEY  (`domain`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Virtual Domains';

CREATE TABLE domain_admins (
  `username` varchar(255) NOT NULL default '',
  `domain` varchar(255) NOT NULL default '',
  `created` datetime NOT NULL default '0000-00-00 00:00:00',
  `active` tinyint(1) NOT NULL default '1',
  KEY username (`username`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Domain Admins';

CREATE TABLE log (
  `timestamp` datetime NOT NULL default '0000-00-00 00:00:00',
  `username` varchar(255) NOT NULL default '',
  `domain` varchar(255) NOT NULL default '',
  `action` varchar(255) NOT NULL default '',
  `data` varchar(255) NOT NULL default '',
  KEY timestamp (`timestamp`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Log';

CREATE TABLE mailbox (
  `account_id` bigint NOT NULL,
  `username` varchar(255) NOT NULL default '',
  `password` varchar(255) NOT NULL default '',
  `name` varchar(255) NOT NULL default '',
  `maildir` varchar(255) NOT NULL default '',
  `quota` bigint(20) NOT NULL default '0',
  `domain` varchar(255) NOT NULL default '',
  `created` datetime NOT NULL default '0000-00-00 00:00:00',
  `modified` datetime NOT NULL default '0000-00-00 00:00:00',
  `active` tinyint(1) NOT NULL default '1',
  PRIMARY KEY  (`account_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Virtual Mailboxes';

CREATE TABLE vacation ( 
	email varchar(255) NOT NULL , 
	subject varchar(255) NOT NULL, 
	body text NOT NULL, 
	cache text NOT NULL, 
	domain varchar(255) NOT NULL , 
	created datetime NOT NULL default '0000-00-00 00:00:00', 
	active tinyint(4) NOT NULL default '1', 
	PRIMARY KEY (email), 
	KEY email (email) 
) ENGINE=INNODB DEFAULT CHARSET=utf8 COMMENT='Postfix Admin - Virtual Vacation';
