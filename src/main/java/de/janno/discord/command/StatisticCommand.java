package de.janno.discord.command;

import de.janno.discord.dice.DiceUtils;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

import static de.janno.discord.DiscordMessageUtils.encodeUTF8;

public class StatisticCommand implements ISlashCommand {

    private static final String COMMAND_NAME = "statistics";
    private final List<? extends AbstractCommand> commands;

    public StatisticCommand(List<? extends AbstractCommand> commands) {
        this.commands = commands;
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    public ApplicationCommandRequest getApplicationCommand() {
        return ApplicationCommandRequest.builder()
                .name(COMMAND_NAME)
                .description("Returns the statistics of the dice rolls")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ChatInputInteractionEvent event) {
        return event.reply(encodeUTF8("Total of all dice roll results: " + DiceUtils.getResultStaticMap() + ", "
                + commands.stream().map(AbstractCommand::getStatistics).collect(Collectors.joining(", "))));
    }


}
