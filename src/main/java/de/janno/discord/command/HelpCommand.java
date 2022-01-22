package de.janno.discord.command;

import de.janno.discord.Metrics;
import de.janno.discord.api.ISlashEventAdaptor;
import de.janno.discord.discord4j.ApplicationCommand;
import discord4j.core.spec.EmbedCreateSpec;
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
    public ApplicationCommand getApplicationCommand() {
        return ApplicationCommand.builder()
                .name(getName())
                .description("Help to the commands and links for further information")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        Metrics.incrementSlashStartMetricCounter(getName(), "[]");
        return event.replyEphemeral(EmbedCreateSpec.builder()
                .addField("Command help", "type '/count_successes help', '/custom_dice help' or '/fate help' to get help for the commands", false)
                .addField("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false)
                .addField("Discord Server", "https://discord.gg/e43BsqKpFr", false)
                .build());
    }
}
