package de.janno.discord.connector.api;

import com.google.common.base.Strings;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public final class BottomCustomIdUtils {
    public static final String CUSTOM_ID_DELIMITER = "\u001e";
    private static final String LEGACY_DELIMITER_V2 = "\u0000";
    private static final String LEGACY_DELIMITER_V1 = ",";
    public static final String LEGACY_CONFIG_SPLIT_DELIMITER_REGEX = String.format("[%s%s]", LEGACY_DELIMITER_V2, LEGACY_DELIMITER_V1);
    private static final int COMMAND_NAME_INDEX = 0;
    private static final int BUTTON_VALUE_INDEX = 1;
    private static final int CONFIG_UUID_INDEX = 2;

    public static boolean isLegacyCustomId(@NonNull String customId) {
        return !customId.contains(CUSTOM_ID_DELIMITER);
    }

    static public @NonNull String createButtonCustomId(@NonNull String commandId, @NonNull String buttonValue, @NonNull UUID configUUID) {
        return commandId + CUSTOM_ID_DELIMITER + buttonValue + CUSTOM_ID_DELIMITER + configUUID;
    }

    static public @NonNull String createButtonCustomIdWithoutConfigId(@NonNull String commandId, @NonNull String buttonValue) {
        return commandId + CUSTOM_ID_DELIMITER + buttonValue;
    }

    public static @NonNull String getButtonValueFromCustomId(@NonNull String customId) {
        if (customId.split(CUSTOM_ID_DELIMITER).length >= 2) {
            return customId.split(CUSTOM_ID_DELIMITER)[BUTTON_VALUE_INDEX];
        }
        throw new IllegalStateException("'%s' contains not the correct number of delimiter".formatted(customId));
    }

    public static @NonNull String getCommandNameFromCustomId(@NonNull String customId) {
        if (customId.split(CUSTOM_ID_DELIMITER).length >= 2) {
            return customId.split(CUSTOM_ID_DELIMITER)[COMMAND_NAME_INDEX];
        }
        throw new IllegalStateException("'%s' contains not the correct number of delimiter".formatted(customId));
    }

    public static @NonNull Optional<UUID> getConfigUUIDFromCustomId(@NonNull String customId) {
        if (customId.split(CUSTOM_ID_DELIMITER).length == 2) {
            return Optional.empty();
        } else if (customId.split(CUSTOM_ID_DELIMITER).length >= 3) {
            return Optional.of(UUID.fromString(customId.split(CUSTOM_ID_DELIMITER)[CONFIG_UUID_INDEX]));
        }
        throw new IllegalStateException("'%s' contains not the correct number of delimiter".formatted(customId));
    }

    public static boolean isValidCustomId(String customId) {
        if (Strings.isNullOrEmpty(customId)) {
            return false;
        }
        String[] splits = customId.split(CUSTOM_ID_DELIMITER);
        long idElementCount = Arrays.stream(splits)
                .filter(s -> !Strings.isNullOrEmpty(s))
                .count();
        return idElementCount == 2 || idElementCount == 3;

    }
}
