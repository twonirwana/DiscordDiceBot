package de.janno.discord.bot.command;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class ClearCommand implements SlashCommand {

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
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId());

        return event.reply(I18n.getMessage("clear.reply", userLocal), false)
                .then(Mono.just(persistenceManager.deleteMessageDataForChannel(event.getChannelId()))
                        .flux()
                        .flatMap(Flux::fromIterable)
                        .delayElements(Duration.ofMillis(io.avaje.config.Config.getLong("command.clear.messageDeleteDelay", 1000)))
                        .flatMap(event::deleteMessageById)
                        .then())
                .doOnSuccess(v -> {
                    persistenceManager.deleteAllChannelConfig(event.getChannelId());
                    persistenceManager.deleteAllMessageConfigForChannel(event.getChannelId());
                });
    }
}
