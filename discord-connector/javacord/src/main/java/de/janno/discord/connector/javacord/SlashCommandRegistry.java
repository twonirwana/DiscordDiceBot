package de.janno.discord.connector.javacord;

import de.janno.discord.connector.api.ISlashCommand;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.interaction.ApplicationCommand;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SlashCommandRegistry {

    private final List<ISlashCommand> slashCommands;

    private SlashCommandRegistry(List<ISlashCommand> slashCommands) {
        this.slashCommands = slashCommands;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<ISlashCommand> getSlashCommands() {
        return slashCommands;
    }

    public static class Builder {
        private final List<ISlashCommand> slashCommands = new ArrayList<>();

        public Builder addSlashCommand(ISlashCommand slashCommand) {
            this.slashCommands.add(slashCommand);
            return this;
        }
        public Builder addSlashCommands(Collection<ISlashCommand> slashCommands) {
            this.slashCommands.addAll(slashCommands);
            return this;
        }

        public SlashCommandRegistry registerSlashCommands(DiscordApi discordApi, boolean disableCommandUpdate) {
            long applicationId = discordApi.getClientId();
            log.info("ApplicationId: " + applicationId);
            if (!disableCommandUpdate) {
                //get the implemented bot commands
                Map<String, ISlashCommand> botCommands = slashCommands.stream()
                        .collect(Collectors.toMap(ISlashCommand::getName, Function.identity()));

                //get already existing commands
                Map<String, SlashCommand> currentlyRegisteredCommands = discordApi.getGlobalApplicationCommands().join().stream()
                        .flatMap(ac -> {
                            if (ac instanceof SlashCommand) {
                                return Stream.of((SlashCommand) ac);
                            }
                            return Stream.empty();
                        })
                        .collect(Collectors.toMap(ApplicationCommand::getName, Function.identity()));
                log.info("Existing Commands: {}", String.join(", ", currentlyRegisteredCommands.keySet()));
                //delete old commands
                Flux.fromIterable(currentlyRegisteredCommands.values())
                        .filter(acd -> !botCommands.containsKey(acd.getName()))
                        .flatMap(acd -> {
                            log.debug("Deleting old command: {}", acd.getName());
                            return Mono.fromFuture(acd.deleteGlobal())
                                    .doOnError(t -> log.error("Error deleting old command: {}", acd, t));
                        })
                        .subscribe();


                //add missing commands
                //todo use bulk
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> !currentlyRegisteredCommands.containsKey(sc.getName()))
                        .flatMap(sc -> {
                                    log.info("Add missing command: {}", sc.getCommandDefinition());
                                    SlashCommandBuilder builder = ApplicationCommandHelper.commandDefinition2SlashCommandBuilder(sc.getCommandDefinition());
                                    return Mono.fromFuture(builder.createGlobal(discordApi))
                                            .doOnError(t -> log.error("Error adding missing command: {}", sc.getCommandDefinition(), t));
                                }
                        )
                        .subscribe();

                //update existing
                //todo use bulk
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> currentlyRegisteredCommands.containsKey(sc.getName()) &&
                                !sc.getCommandDefinition().equals(ApplicationCommandHelper.slashCommand2CommandDefinition(currentlyRegisteredCommands.get(sc.getName()))))
                        .flatMap(sc -> {
                                    log.info("Update command: {} != {}", sc.getCommandDefinition(), ApplicationCommandHelper.slashCommand2CommandDefinition(currentlyRegisteredCommands.get(sc.getName())));
                                    SlashCommandBuilder builder = ApplicationCommandHelper.commandDefinition2SlashCommandBuilder(sc.getCommandDefinition());
                                    return Mono.fromFuture(builder.createGlobal(discordApi))
                                            .doOnError(t -> log.error("Error updating command: {}", sc.getCommandDefinition(), t));

                                }
                        )
                        .subscribe();
            }
            return new SlashCommandRegistry(slashCommands);
        }

    }
}
