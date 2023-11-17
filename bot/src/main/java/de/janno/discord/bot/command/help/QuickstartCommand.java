package de.janno.discord.bot.command.help;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class QuickstartCommand implements SlashCommand {

    public static final String ROLL_COMMAND_ID = "quickstart";
    protected static final String ACTION_SYSTEM = "system";
    private final RpgSystemCommandPreset rpgSystemCommandPreset;

    public QuickstartCommand(RpgSystemCommandPreset rpgSystemCommandPreset) {
        this.rpgSystemCommandPreset = rpgSystemCommandPreset;
    }

    @VisibleForTesting
    static Optional<RpgSystemCommandPreset.PresetId> getPresetId(@NonNull String id) {
        String trimId = id.trim().toLowerCase();
        Optional<RpgSystemCommandPreset.PresetId> matchingId = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> Objects.equals(presetId.name().toLowerCase(), trimId))
                .findFirst();
        if (matchingId.isPresent()) {
            return matchingId;
        }
        Optional<RpgSystemCommandPreset.PresetId> matchingDisplayName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> Objects.equals(presetId.getDisplayName().toLowerCase(), trimId))
                .findFirst();
        if (matchingDisplayName.isPresent()) {
            return matchingDisplayName;
        }
        Optional<RpgSystemCommandPreset.PresetId> matchingSynonymeName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> presetId.getSynonymes().stream().map(String::toLowerCase).anyMatch(s -> s.equals(trimId)))
                .findFirst();
        if (matchingSynonymeName.isPresent()) {
            return matchingSynonymeName;
        }
        Optional<RpgSystemCommandPreset.PresetId> startsWithDisplayName = Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(presetId -> presetId.getDisplayName().toLowerCase().startsWith(trimId))
                .findFirst();
        if (startsWithDisplayName.isPresent()) {
            return matchingSynonymeName;
        }
        return Optional.empty();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest option, @NonNull Locale userLocale) {
        if (!ACTION_SYSTEM.equals(option.getFocusedOptionName())) {
            return List.of();
        }
        return Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(p -> Stream.concat(Stream.of(p.getDisplayName()), p.getSynonymes().stream())
                        .anyMatch(n -> n.toLowerCase().contains(option.getFocusedOptionValue().toLowerCase())))
                .sorted(Comparator.comparing(RpgSystemCommandPreset.PresetId::getDisplayName))
                .map(p -> new AutoCompleteAnswer(p.getDisplayName(), p.name()))
                .collect(Collectors.toList());
    }

    @Override
    public @NonNull String getCommandId() {
        return ROLL_COMMAND_ID;
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Select a preconfigured dice set from a given systems")
                .option(CommandDefinitionOption.builder()
                        .name(ACTION_SYSTEM)
                        .required(true)
                        .autoComplete(true)
                        .description("Start typing to filter and see more options")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Optional<String> checkPermissions = event.checkPermissions();
        if (checkPermissions.isPresent()) {
            return event.reply(checkPermissions.get(), false);
        }

        final UUID newConfigUUID = uuidSupplier.get();
        final long guildId = event.getGuildId();
        final long channelId = event.getChannelId();
        Optional<CommandInteractionOption> expressionOptional = event.getOption(ACTION_SYSTEM);
        if (expressionOptional.isPresent()) {
            final String systemId = expressionOptional
                    .map(CommandInteractionOption::getStringValue)
                    .orElseThrow();
            final Optional<RpgSystemCommandPreset.PresetId> presetIdOptional = getPresetId(systemId);
            if (presetIdOptional.isPresent()) {
                final RpgSystemCommandPreset.PresetId presetId = presetIdOptional.get();
                BotMetrics.incrementSlashStartMetricCounter(getCommandId(), presetId.name());
                RpgSystemCommandPreset.CommandAndMessageDefinition commandAndMessageDefinition = rpgSystemCommandPreset.createMessage(presetId, newConfigUUID, guildId, channelId);
                return Mono.defer(() -> event.createMessageWithoutReference(commandAndMessageDefinition.getMessageDefinition()))
                        .doOnSuccess(v -> BotMetrics.timerNewButtonMessageMetricCounter(getCommandId(), stopwatch.elapsed()))
                        .then(event.reply("`%s`".formatted(commandAndMessageDefinition.getCommand()), false))
                        .doOnSuccess(v ->
                                log.info("{}: '{}' {}ms",
                                        event.getRequester().toLogString(),
                                        presetId.name(),
                                        stopwatch.elapsed(TimeUnit.MILLISECONDS)
                                ));
            } else {
                log.info("Can't match RPG system id: '{}'", systemId);
                return event.reply("Unknown rpg: `%s`".formatted(systemId), true);
            }

        }
        return Mono.empty();
    }
}