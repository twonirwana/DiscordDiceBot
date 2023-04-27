package de.janno.discord.connector.api;

import de.janno.discord.connector.api.slash.CommandDefinition;
import lombok.NonNull;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public interface SlashCommand {

    @NonNull String getCommandId();

    @NonNull CommandDefinition getCommandDefinition();

    @NonNull Mono<Void> handleSlashCommandEvent(@NonNull SlashEventAdaptor event, @NonNull Supplier<UUID> uuidSupplier);

    default @NonNull Optional<AutoCompleteAnswer> getAutoCompleteAnswer(String option, String value) {
        return Optional.empty();
    }

}
