-- If the bot is used outside of guilds, like direct messages then there is no guildId
ALTER TABLE MESSAGE_CONFIG
    ALTER COLUMN GUILD_ID DROP NOT NULL;
ALTER TABLE CHANNEL_CONFIG
    ALTER COLUMN GUILD_ID DROP NOT NULL;