package de.janno.discord.bot.command;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class FetchCommand implements SlashCommand {

    private final PersistenceManager persistenceManager;
    private final CustomParameterCommand customParameterCommand;
    private final CustomDiceCommand customDiceCommand;
    private final SumCustomSetCommand sumCustomSetCommand;

    @Override
    public @NonNull String getCommandId() {
        return "fetch";
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        //todo fetch last and fetch by name
        return CommandDefinition.builder()
                .name(getCommandId())
                .nameLocales(I18n.allNoneEnglishMessagesNames("fetch.name"))
                .description(I18n.getMessage("fetch.description", Locale.ENGLISH))
                .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("fetch.description"))
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocal) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId());
        long fetchDelayMs = io.avaje.config.Config.getLong("command.fetch.delayMs", 60_000);
        Long oldestMessageIdWaitingToDeleted = MessageDeletionHelper.getMessageWaitingToBeDeleted(event.getChannelId()).stream()
                .min(Comparator.comparing(Function.identity()))
                .orElse(null);
        Optional<MessageConfigDTO> messageConfigDTOOptional = persistenceManager.getLastMessageDataInChannel(event.getChannelId(),
                LocalDateTime.now().minus(fetchDelayMs, ChronoUnit.MILLIS),
                oldestMessageIdWaitingToDeleted);
        log.info("{}: Fetch - {}",
                event.getRequester().toLogString(),
                messageConfigDTOOptional.map(m -> "found %s - %s".formatted(m.getCommandId(), m.getConfigUUID())).orElse("not found"));
        if (messageConfigDTOOptional.isPresent()) {
            final MessageConfigDTO messageConfigDTO = messageConfigDTOOptional.get();
            final UUID configUUID = messageConfigDTO.getConfigUUID();
            if (customDiceCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                CustomDiceConfig config = CustomDiceCommand.deserializeConfig(messageConfigDTO);
                return moveButtonMessage(config, customDiceCommand, configUUID, event);
            } else if (customParameterCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                CustomParameterConfig config = CustomParameterCommand.deserializeConfig(messageConfigDTO);
                return moveButtonMessage(config, customParameterCommand, configUUID, event);
            } else if (sumCustomSetCommand.getCommandId().equals(messageConfigDTO.getCommandId())) {
                SumCustomSetConfig config = SumCustomSetCommand.deserializeConfig(messageConfigDTO);
                return moveButtonMessage(config, sumCustomSetCommand, configUUID, event);
            }
        }
        return event.reply(I18n.getMessage("fetch.no.message.found", userLocal), true);
    }

    private <C extends RollConfig> Mono<Void> moveButtonMessage(C config, AbstractCommand<C, ?> command, UUID configUUID, SlashEventAdaptor event) {
        EmbedOrMessageDefinition buttonMessage = command.createSlashResponseMessage(configUUID, config, event.getChannelId());
        List<Mono<Void>> actions = List.of(
                Mono.defer(() -> event.reply(I18n.getMessage("fetch.reply", event.getRequester().getUserLocal()) , true)),
                Mono.defer(() -> event.sendMessage(buttonMessage)
                                .doOnNext(messageId -> command.createEmptyMessageData(configUUID, event.getGuildId(), event.getChannelId(), messageId)))
                        .flatMap(newMessageId -> MessageDeletionHelper.deleteOldMessageAndData(persistenceManager, newMessageId, null, configUUID, event.getChannelId(), event))
                        .then());
        return Flux.merge(1, actions.toArray(new Mono<?>[0]))
                .parallel()
                .then();
    }


}
