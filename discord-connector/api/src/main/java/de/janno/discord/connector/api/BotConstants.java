package de.janno.discord.connector.api;

public final class BotConstants {
    public static final String LEGACY_DELIMITER_V2 = "\u0000";
    public static final String CUSTOM_ID_DELIMITER = "\u001e";
    public static final String LEGACY_DELIMITER_V1 = ",";
    public static final String LEGACY_CONFIG_SPLIT_DELIMITER_REGEX = String.format("[%s%s]", LEGACY_DELIMITER_V2, LEGACY_DELIMITER_V1);

}
