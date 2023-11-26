package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.SlashCommand;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class SlashCommandRegistry {

    private final List<SlashCommand> slashCommands;

    private SlashCommandRegistry(List<SlashCommand> slashCommands) {
        this.slashCommands = slashCommands;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<SlashCommand> getSlashCommands() {
        return slashCommands;
    }

    public static class Builder {
        private final List<SlashCommand> slashCommands = new ArrayList<>();

        public Builder addSlashCommands(Collection<SlashCommand> slashCommands) {
            this.slashCommands.addAll(slashCommands);
            return this;
        }

        public SlashCommandRegistry registerSlashCommands(JDA jda, boolean disableCommandUpdate) {
            long applicationId = jda.getSelfUser().getApplicationIdLong();
            log.info("ApplicationId: " + applicationId);
            if (!disableCommandUpdate) {
                //get the implemented bot commands
                Map<String, SlashCommand> botCommands = slashCommands.stream()
                        .collect(Collectors.toMap(SlashCommand::getCommandId, Function.identity()));

                //get already existing commands
                Map<String, Command> currentlyRegisteredCommands = jda.retrieveCommands(true).complete().stream()
                        .collect(Collectors.toMap(Command::getName, Function.identity()));
                log.info("Existing Commands: {}", String.join(", ", currentlyRegisteredCommands.keySet()));
                //delete old commands
                Flux.fromIterable(currentlyRegisteredCommands.values())
                        .filter(acd -> !botCommands.containsKey(acd.getName()))
                        .flatMap(acd -> {
                            log.debug("Deleting old command: {}", acd.getName());
                            return Mono.fromFuture(acd.delete().submit())
                                    .doOnError(t -> log.error("Error deleting old command: {}", acd, t));
                        })
                        .subscribe();


                //add missing commands
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> !currentlyRegisteredCommands.containsKey(sc.getCommandId()))
                        .flatMap(sc -> {
                                    log.info("Add missing command: {}", sc.getCommandDefinition());
                                    CommandData commandData = ApplicationCommandConverter.commandDefinition2CommandData(sc.getCommandDefinition());
                                    return Mono.fromFuture(jda.upsertCommand(commandData).submit())
                                            .doOnError(t -> log.error("Error adding missing command: {}", sc.getCommandDefinition(), t));
                                }
                        )
                        .subscribe();

                //update existing
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> currentlyRegisteredCommands.containsKey(sc.getCommandId()) &&
                                !sc.getCommandDefinition().equals(ApplicationCommandConverter.slashCommand2CommandDefinition(currentlyRegisteredCommands.get(sc.getCommandId()))))
                        .flatMap(sc -> {
                                    log.info("Update command: {} != {}", sc.getCommandDefinition(), ApplicationCommandConverter.slashCommand2CommandDefinition(currentlyRegisteredCommands.get(sc.getCommandId())));
                                    CommandData commandData = ApplicationCommandConverter.commandDefinition2CommandData(sc.getCommandDefinition());
                                    return Mono.fromFuture(jda.upsertCommand(commandData).submit())
                                            .doOnError(t -> log.error("Error updating command: {}", sc.getCommandDefinition(), t));

                                }
                        )
                        .subscribe();
            }
            return new SlashCommandRegistry(slashCommands);
        }

    }
}
