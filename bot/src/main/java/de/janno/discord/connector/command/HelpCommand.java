package de.janno.discord.connector.command;

import de.janno.discord.connector.Metrics;
import de.janno.discord.connector.api.EmbedDefinition;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class HelpCommand implements ISlashCommand {
    @Override
    public String getName() {
        return "help";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getName())
                .description("Help to the commands and links for further information")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        Metrics.incrementSlashStartMetricCounter(getName(), "[]");
        return event.replyEphemeral(EmbedDefinition.builder()
                .field(new EmbedDefinition.Field("Command help", "type '/count_successes help', '/custom_dice help' or '/fate help' to get help for the commands", false))
                .field(new EmbedDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build());
    }
}
