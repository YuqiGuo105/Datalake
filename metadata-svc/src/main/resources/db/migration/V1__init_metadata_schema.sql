CREATE TABLE datasets (
                          id            VARCHAR(36)   PRIMARY KEY,
                          name          VARCHAR(128)  NOT NULL UNIQUE,
                          storage_uri   VARCHAR(1024) NOT NULL,
                          format        VARCHAR(16)   NOT NULL,
                          project       VARCHAR(64),
                          created_at    DATETIME(6)   NOT NULL,
                          created_by    VARCHAR(64),
                          last_commit_id VARCHAR(36)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE dataset_columns (
                                 id             BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 dataset_id     VARCHAR(36) NOT NULL,
                                 version        INT         NOT NULL,
                                 column_name    VARCHAR(128),
                                 data_type      VARCHAR(64),
                                 is_partition   TINYINT(1),
                                 is_primary_key TINYINT(1),
                                 comment        VARCHAR(256),
                                 CONSTRAINT fk_dataset FOREIGN KEY (dataset_id) REFERENCES datasets(id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

ALTER TABLE dataset_columns
    ADD UNIQUE KEY uk_ds_ver_col (dataset_id, version, column_name);

CREATE TABLE dataset_commits (
                                 id          VARCHAR(36) PRIMARY KEY,
                                 dataset_id  VARCHAR(36) NOT NULL,
                                 version     INT,
                                 commit_time DATETIME(6),
                                 author      VARCHAR(64),
                                 message     VARCHAR(256),
                                 extra_meta  JSON,                     -- MySQL 原生 JSON
                                 CONSTRAINT fk_commit_dataset FOREIGN KEY (dataset_id) REFERENCES datasets(id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
