ALTER TABLE CHANNEL_CONFIG
    ADD COLUMN IF NOT EXISTS USER_ID BIGINT;
CREATE INDEX IF NOT EXISTS CHANNEL_CONFIG_CHANNEL_USER ON CHANNEL_CONFIG (CHANNEL_ID, USER_ID);
