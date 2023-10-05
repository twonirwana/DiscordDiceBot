package de.janno.discord.bot.command.help;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.function.Supplier;

@Slf4j
public class HelpCommand implements SlashCommand {
    @Override
    public @NonNull String getCommandId() {
        return "help";
    }

    @Override
    public @NonNull CommandDefinition getCommandDefinition() {
        return CommandDefinition.builder()
                .name(getCommandId())
                .description("Help to the commands and links for further information")
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "[]");
        return event.replyEmbed(EmbedOrMessageDefinition.builder()
                .field(new EmbedOrMessageDefinition.Field("Quick start", "Write to `/welcome start` get a quick start message", false))
                .field(new EmbedOrMessageDefinition.Field("Command help", "Add `help` after a command to get specific help for it, e.g. '/custom_dice help'", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);
    }
}
