package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableMap;
import de.janno.discord.command.ISlashCommand;
import discord4j.core.DiscordClient;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            slashCommands.add(slashCommand);
            return this;
        }

        public SlashCommandRegistry registerSlashCommands(DiscordClient discordClient, boolean disableCommandUpdate) {
            ApplicationService applicationService = discordClient.getApplicationService();
            long applicationId = discordClient.getApplicationId().blockOptional().orElseThrow();
            log.info("ApplicationId: " + applicationId);
            if (!disableCommandUpdate) {
                //get the implemented bot commands
                Map<String, ISlashCommand> botCommands = slashCommands.stream()
                        .collect(Collectors.toMap(ISlashCommand::getName, Function.identity()));

                //get already existing commands
                Map<String, ApplicationCommandData> currentlyRegisteredCommands = applicationService.getGlobalApplicationCommands(applicationId)
                        .collectMap(ApplicationCommandData::name, Function.identity()).blockOptional().orElse(ImmutableMap.of());
                log.info("Existing Commands: {}", String.join(", ", currentlyRegisteredCommands.keySet()));
                //delete old commands
                Flux.fromIterable(currentlyRegisteredCommands.values())
                        .filter(acd -> !botCommands.containsKey(acd.name()))
                        .flatMap(acd -> {
                            log.debug("Deleting old command: {}", acd);
                            return applicationService.deleteGlobalApplicationCommand(applicationId, Long.parseLong(acd.id()))
                                    .doOnError(t -> log.error("Error deleting old command: {}", acd, t));
                        })
                        .subscribe();


                //add missing commands
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> !currentlyRegisteredCommands.containsKey(sc.getName()))
                        .flatMap(sc -> {
                                    log.info("Add missing command: {}", sc.getApplicationCommand());
                                    return applicationService.createGlobalApplicationCommand(applicationId, sc.getApplicationCommand().buildRequest())
                                            .doOnError(t -> log.error("Error adding missing command: {}", sc.getApplicationCommand(), t));
                                }
                        )
                        .subscribe();

                //update existing
                Flux.fromIterable(botCommands.values())
                        .filter(sc -> currentlyRegisteredCommands.containsKey(sc.getName()) &&
                                !sc.getApplicationCommand().equalToApplicationCommandData(currentlyRegisteredCommands.get(sc.getName())))
                        .flatMap(sc -> {
                                    log.info("Update command: {}", sc.getApplicationCommand());
                                    return applicationService.modifyGlobalApplicationCommand(applicationId,
                                                    Long.parseLong(currentlyRegisteredCommands.get(sc.getName()).id()),
                                                    sc.getApplicationCommand().buildRequest())
                                            .doOnError(t -> log.error("Error updating command: {}", sc.getApplicationCommand(), t));

                                }
                        )
                        .subscribe();
            }
            return new SlashCommandRegistry(slashCommands);
        }

    }
}
