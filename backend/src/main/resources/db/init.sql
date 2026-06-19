CREATE DATABASE IF NOT EXISTS agreement_db
  DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE agreement_db;

DROP TABLE IF EXISTS agreements;

CREATE TABLE agreements (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  language   VARCHAR(10)  NOT NULL COMMENT 'language code, e.g. zh-CN, ja, th',
  title      VARCHAR(255) DEFAULT NULL,
  content    LONGTEXT     NOT NULL COMMENT 'rich text in HTML format',
  created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_language (language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='signed agreements';
