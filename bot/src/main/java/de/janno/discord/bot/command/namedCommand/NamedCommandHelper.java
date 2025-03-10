package de.janno.discord.bot.command.namedCommand;

import com.google.common.annotations.VisibleForTesting;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.channelConfig.AliasConfig;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.*;

public final class NamedCommandHelper {

    public static Optional<NamedConfig> getConfigForName(PersistenceManager persistenceManager, String name, Locale locale, @Nullable Long guildId, long userId) {
        Optional<NamedConfig> savedNamedCommandConfig = persistenceManager.getLastUsedNamedCommandsOfUserAndGuild(userId, guildId).stream()
                .filter(nc -> Objects.equals(nc.name(), name))
                .flatMap(nc -> getConfigForNamedCommand(persistenceManager, nc.id()).map(snc -> new NamedConfig(snc.namedConfig().name(), snc.namedConfig().commandId(), snc.namedConfig().configClassId(), snc.namedConfig().config())).stream())
                .findFirst();
        if (savedNamedCommandConfig.isPresent()) {
            return savedNamedCommandConfig;
        }

        Optional<RpgSystemCommandPreset.PresetId> presetId = getPresetId(name, locale);
        presetId.ifPresent(p -> BotMetrics.incrementPresetMetricCounter(p.name()));
        return presetId.map(id -> createNameConfigFromPresetId(name, id, locale));
    }

    @VisibleForTesting
    static Optional<RpgSystemCommandPreset.PresetId> getPresetId(@NonNull String id, @NonNull Locale userLocal) {
        String trimId = id.trim().toLowerCase();
        Optional<RpgSystemCommandPreset.PresetId> matchingDisplayName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> Objects.equals(presetId.getName(userLocal).toLowerCase(), trimId))
                .findFirst();
        if (matchingDisplayName.isPresent()) {
            return matchingDisplayName;
        }
        Optional<RpgSystemCommandPreset.PresetId> matchingSynonymeName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> presetId.getSynonymes(userLocal).stream().map(String::toLowerCase).anyMatch(s -> s.equals(trimId)))
                .findFirst();
        if (matchingSynonymeName.isPresent()) {
            return matchingSynonymeName;
        }
        Optional<RpgSystemCommandPreset.PresetId> startsWithDisplayName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> presetId.getName(userLocal).toLowerCase().startsWith(trimId))
                .findFirst();
        if (startsWithDisplayName.isPresent()) {
            return startsWithDisplayName;
        }
        Optional<RpgSystemCommandPreset.PresetId> matchingId = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> Objects.equals(presetId.name().toLowerCase(), trimId))
                .findFirst();
        if (matchingId.isPresent()) {
            return matchingId;
        }
        return Optional.empty();
    }

    public static NamedConfig createNameConfigFromPresetId(String name, RpgSystemCommandPreset.PresetId presetId, Locale locale) {
        Config config = RpgSystemCommandPreset.createConfig(presetId, locale);
        String commandId = presetId.getCommandId();
        String configClassId = presetId.getConfigClassType();
        return new NamedConfig(name, commandId, configClassId, config);
    }

    public static Optional<SavedNamedConfig> getConfigForNamedCommand(PersistenceManager persistenceManager, UUID configUUID) {
        Optional<MessageConfigDTO> optionalMessageConfigDTO = persistenceManager.getMessageConfig(configUUID);
        if (optionalMessageConfigDTO.isPresent() && optionalMessageConfigDTO.get().getName() != null) {
            MessageConfigDTO messageConfigDTO = optionalMessageConfigDTO.get();
            if (CustomDiceCommand.COMMAND_NAME.equals(messageConfigDTO.getCommandId())
                    && CustomDiceCommand.CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), CustomDiceCommand.deserializeConfig(messageConfigDTO))));
            } else if (CustomParameterCommand.COMMAND_NAME.equals(messageConfigDTO.getCommandId())
                    && CustomParameterCommand.CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), CustomParameterCommand.deserializeConfig(messageConfigDTO))));
            } else if (SumCustomSetCommand.COMMAND_NAME.equals(messageConfigDTO.getCommandId())
                    && SumCustomSetCommand.CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId())) {
                return Optional.of(new SavedNamedConfig(configUUID, new NamedConfig(messageConfigDTO.getName(), messageConfigDTO.getCommandId(), messageConfigDTO.getConfigClassId(), SumCustomSetCommand.deserializeConfig(messageConfigDTO))));
            }
        }
        return Optional.empty();
    }


    public static Config updateCallStarterConfigAfterFinish(Config genericConfig, UUID starterConfigAfterFinish) {
        return switch (genericConfig) {
            case CustomDiceConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case CustomParameterConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case SumCustomSetConfig config -> config.toBuilder()
                    .callStarterConfigAfterFinish(starterConfigAfterFinish)
                    .build();
            case AliasConfig config -> config;
            case null, default ->
                    throw new IllegalStateException("command not supported:  %s".formatted(genericConfig));
        };
    }

}
