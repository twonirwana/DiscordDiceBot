package de.janno.discord.bot.command.help;

import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.I18n;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Locale;
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
                .nameLocales(I18n.additionalMessages("help.name"))
                .description(I18n.getMessage("help.description", Locale.ENGLISH))
                .descriptionLocales(I18n.additionalMessages("help.description"))
                .build();
    }

    @Override
    public @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier, @NonNull Locale userLocale) {
        BotMetrics.incrementSlashStartMetricCounter(getCommandId(), "[]");
        return event.replyWithEmbedOrMessageDefinition(EmbedOrMessageDefinition.builder()
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.quickstart.field.name", userLocale), I18n.getMessage("help.quickstart.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.command.field.name", userLocale), I18n.getMessage("help.command.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build(), true);
    }
}
