package de.janno.discord.connector.api;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

public final class BottomCustomIdUtils {
    public static final String CUSTOM_ID_DELIMITER = "\u001e";
    private static final String LEGACY_DELIMITER_V2 = "\u0000";
    private static final String LEGACY_DELIMITER_V1 = ",";
    public static final String LEGACY_CONFIG_SPLIT_DELIMITER_REGEX = String.format("[%s%s]", LEGACY_DELIMITER_V2, LEGACY_DELIMITER_V1);
    private static final int COMMAND_NAME_INDEX = 0;
    private static final int BUTTON_VALUE_INDEX = 1;

    static public @NonNull String createButtonCustomId(@NonNull String commandId, @NonNull String buttonValue) {
        return commandId + CUSTOM_ID_DELIMITER + buttonValue;
    }

    public static boolean isLegacyCustomId(@NonNull String customId) {
        return !customId.contains(CUSTOM_ID_DELIMITER);
    }

    public static @NonNull String getButtonValueFromCustomId(@NonNull String customId) {
        Preconditions.checkArgument(StringUtils.countMatches(customId, CUSTOM_ID_DELIMITER) == 1, "'%s' contains not the correct number of delimiter", customId);
        return customId.split(CUSTOM_ID_DELIMITER)[BUTTON_VALUE_INDEX];
    }

    public static @NonNull String getCommandNameFromCustomIdWithPersistence(@NonNull String customId) {
        Preconditions.checkArgument(StringUtils.countMatches(customId, CUSTOM_ID_DELIMITER) == 1, "'%s' contains not the correct number of delimiter", customId);
        return customId.split(CUSTOM_ID_DELIMITER)[COMMAND_NAME_INDEX];
    }

    /**
     * will be removed when almost all users have switched to the persisted button id
     */
    public static boolean matchesLegacyCustomId(@NonNull String customId, @NonNull String commandId) {
        return customId.matches("^" + commandId + LEGACY_CONFIG_SPLIT_DELIMITER_REGEX + ".*");
    }

}
