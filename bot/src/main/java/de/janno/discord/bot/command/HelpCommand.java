package de.janno.discord.bot.command;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class HelpCommand implements ISlashCommand {
    @Override
    public String getCommandId() {
        return "help";
    }

    @Override
    public CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Help to the commands and links for further information")
                .build();
    }

    @Override
    public Mono<Void> handleSlashCommandEvent(@NonNull ISlashEventAdaptor event) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "[]");
        return event.replyEmbed(EmbedDefinition.builder()
                .field(new EmbedDefinition.Field("Command help", "type '/count_successes help', '/custom_dice help' or '/fate help' to get help for the commands", false))
                .field(new EmbedDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);
    }
}
