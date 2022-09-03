package de.janno.discord.bot.command;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class ClearCommand implements SlashCommand {

    private final MessageDataDAO messageDataDAO;

    public ClearCommand(MessageDataDAO messageDataDAO) {
        this.messageDataDAO = messageDataDAO;
    }

    @Override
    public String getCommandId() {
        return "clear";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Removes all button messages and saved bot data for this channel")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "[]");
        return event.reply("Deleting messages and data ...")
                .then(Mono.just(messageDataDAO.deleteDataForChannel(event.getChannelId()))
                        .flux()
                        .flatMap(Flux::fromIterable)
                        .flatMap(id -> event.deleteMessage(id, true))
                        .then());
    }
}
