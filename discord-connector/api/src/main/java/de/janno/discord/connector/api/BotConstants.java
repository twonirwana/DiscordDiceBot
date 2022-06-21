package de.janno.discord.connector.api;

public final class BotConstants {
    public static final String CONFIG_DELIMITER = "\u0000";
    public static final String LEGACY_DELIMITER = ",";
    public static final String CONFIG_SPLIT_DELIMITER_REGEX = String.format("[%s%s]", CONFIG_DELIMITER, LEGACY_DELIMITER);

}
