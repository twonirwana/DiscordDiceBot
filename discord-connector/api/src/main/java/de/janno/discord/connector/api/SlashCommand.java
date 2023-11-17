package de.janno.discord.connector.api;

import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

public interface SlashCommand {

    @NonNull String getCommandId();

    @NonNull CommandDefinition getCommandDefinition();

    @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier);

    default @NonNull List<AutoCompleteAnswer> getAutoCompleteAnswer(AutoCompleteRequest autoCompleteRequest, Locale userLocale) {
        return List.of();
    }

}
