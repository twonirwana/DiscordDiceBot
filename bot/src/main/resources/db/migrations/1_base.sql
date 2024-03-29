CREATE TABLE IF NOT EXISTS MESSAGE_DATA
(
    CONFIG_ID       UUID      NOT NULL,
    CHANNEL_ID      BIGINT    NOT NULL,
    MESSAGE_ID      BIGINT    NOT NULL,
    COMMAND_ID      VARCHAR   NOT NULL,
    CONFIG_CLASS_ID VARCHAR   NOT NULL,
    CONFIG          VARCHAR   NOT NULL,
    STATE_CLASS_ID  VARCHAR   NOT NULL,
    STATE           VARCHAR   NULL,
    CREATION_DATE   TIMESTAMP NOT NULL,
    PRIMARY KEY (CONFIG_ID, CHANNEL_ID, MESSAGE_ID)
);

CREATE INDEX IF NOT EXISTS MESSAGE_DATA_ID ON MESSAGE_DATA (CONFIG_ID);
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL ON MESSAGE_DATA (CHANNEL_ID);
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CHANNEL_MESSAGE ON MESSAGE_DATA (CHANNEL_ID, MESSAGE_ID);

ALTER TABLE MESSAGE_DATA
    ADD COLUMN IF NOT EXISTS GUILD_ID BIGINT;
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_GUILD ON MESSAGE_DATA (GUILD_ID);
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_GUILD_CHANNEl ON MESSAGE_DATA (GUILD_ID, CHANNEL_ID);
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CREATION_DATE_GUILD_CHANNEl ON MESSAGE_DATA (CREATION_DATE, GUILD_ID);
CREATE UNIQUE INDEX IF NOT EXISTS MESSAGE_DATA_CREATION_DATE_MESSAGE_ID ON MESSAGE_DATA (CREATION_DATE, MESSAGE_ID);
CREATE INDEX IF NOT EXISTS MESSAGE_DATA_CREATION_DATE ON MESSAGE_DATA (CREATION_DATE);


CREATE TABLE IF NOT EXISTS CHANNEL_CONFIG
(
    CONFIG_ID       UUID      NOT NULL,
    CHANNEL_ID      BIGINT    NOT NULL,
    GUILD_ID        BIGINT    NOT NULL,
    COMMAND_ID      VARCHAR   NOT NULL,
    CONFIG_CLASS_ID VARCHAR   NOT NULL,
    CONFIG          VARCHAR   NOT NULL,
    CREATION_DATE   TIMESTAMP NOT NULL,
    PRIMARY KEY (CHANNEL_ID, CONFIG_CLASS_ID)
);

CREATE INDEX IF NOT EXISTS CHANNEL_CONFIG_CHANNEL ON CHANNEL_CONFIG (CHANNEL_ID);


CREATE TABLE IF NOT EXISTS DB_VERSION
(
    MIGRATION_NAME VARCHAR   NOT NULL,
    CREATION_DATE  TIMESTAMP NOT NULL
);

INSERT INTO DB_VERSION(MIGRATION_NAME, CREATION_DATE)
select 'base', CURRENT_TIMESTAMP()
where not exists(select * from DB_VERSION where MIGRATION_NAME = 'base');
