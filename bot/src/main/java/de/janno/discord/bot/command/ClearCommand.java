package de.janno.discord.bot.command;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class ClearCommand implements SlashCommand {

    private static final String NAME_OPTION = "name";
    private final PersistenceManager persistenceManager;

    public ClearCommand(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    @Override
    public @NonNull String getCommandId() {
        return "clear";
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("clear.name"))
                .description(I18n.getMessage("clear.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("clear.description"))
                .option(CommandDefinitionOption.builder()
                        .autoComplete(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(false)
                        .name(I18n.getMessage("clear.option.name.name", Locale.ENGLISH))
                        .description(I18n.getMessage("clear.option.name.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("clear.option.name.description"))
                        .build())
                .build();
    }

    @Override
    public @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(@NonNull AutoCompleteRequest autoCompleteRequest, @NonNull Locale userLocale, long channelId, Long guildId, long userId) {
        if (!NAME_OPTION.equals(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }

        return persistenceManager.getNamedCommandsChannel(channelId).stream()
                .filter(nc -> Strings.isNullOrEmpty(autoCompleteRequest.getFocusedOptionValue()) || nc.toLowerCase().contains(autoCompleteRequest.getFocusedOptionValue().toLowerCase()))
                .map(n -> new AutoCompleteAnswer(n, n))
                .distinct()
                .sorted(Comparator.comparing(AutoCompleteAnswer::getName))
                .limit(5)
                .toList();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId());

        final String name = event.getOption(NAME_OPTION).map(CommandInteractionOption::getStringValue).orElse(null);
        Stopwatch deleteStopwatch = Stopwatch.createStarted();
        return event.reply(I18n.getMessage("clear.reply", userLocal), false)
                .then(Mono.just(persistenceManager.deleteMessageDataForChannel(event.getChannelId(), name))
                        .flux()
                        .flatMap(Flux::fromIterable)
                        .delayElements(Duration.ofMillis(io.avaje.config.Config.getLong("command.clear.messageDeleteDelay", 1000)))
                        .flatMap(messageId -> event.deleteMessageById(messageId).thenReturn(messageId))
                        .count()
                        .doOnSuccess(c -> log.info("{}: Finish delete{} with {} messages in {}ms",
                                event.getRequester().toLogString(),
                                Optional.ofNullable(name).map(" of '%s'"::formatted).orElse(""),
                                c,
                                deleteStopwatch.elapsed(TimeUnit.MILLISECONDS)))
                        .then())
                .doOnSuccess(v -> {
                    if (Strings.isNullOrEmpty(name)) {
                        persistenceManager.deleteAllChannelConfig(event.getChannelId());
                    }
                    persistenceManager.deleteAllMessageConfigForChannel(event.getChannelId(), name);
                });
    }
}
