package de.janno.discord.command;

import com.google.common.collect.ImmutableMap;
import discord4j.core.DiscordClient;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.rest.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;

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

        public SlashCommandRegistry registerSlashCommands(DiscordClient discordClient) {
            ApplicationService applicationService = discordClient.getApplicationService();
            long applicationId = discordClient.getApplicationId().blockOptional().orElseThrow();
            Map<String, ISlashCommand> buttonSlashCommandMap = slashCommands.stream()
                    .collect(Collectors.toMap(ISlashCommand::getName, Function.identity()));
            log.info("ApplicationId: " + applicationId);

            //delete old commands
            applicationService.getGlobalApplicationCommands(applicationId)
                    .doOnEach(acd -> log.info("Existing command: " + acd))
                    .filter(acd -> !buttonSlashCommandMap.containsKey(acd.name()))
                    .doOnEach(acd -> log.info("Deleting command: " + acd))
                    .flatMap(acd -> applicationService.deleteGlobalApplicationCommand(applicationId, Long.parseLong(acd.id())))
                    .subscribe();

            //get already existing commands
            Map<String, ApplicationCommandData> applicationCommandData = applicationService.getGlobalApplicationCommands(applicationId)
                    .collectMap(ApplicationCommandData::name, Function.identity()).blockOptional().orElse(ImmutableMap.of());

            //add missing commands
            buttonSlashCommandMap.values().stream()
                    .filter(sc -> !applicationCommandData.containsKey(sc.getName()))
                    .forEach(sc -> {
                                log.info("Creating command: " + sc.getApplicationCommand());
                                applicationService.createGlobalApplicationCommand(applicationId, sc.getApplicationCommand())
                                        .doOnEach(acd -> log.info("Created command: " + acd))
                                        .block();
                            }
                    );

            //update existing
            //todo check for changes in existing commands
/*
            buttonSlashCommandMap.values().stream()
                    .filter(sc -> applicationCommandData.containsKey(sc.getName()))
                    .forEach(sc -> {
                                log.info("Creating command: " + sc.getApplicationCommand());
                                applicationService.modifyGlobalApplicationCommand(applicationId,
                                        Long.parseLong(applicationCommandData.get(sc.getName()).id()),
                                        sc.getApplicationCommand())
                                        .doOnEach(acd -> log.info("Update command: " + acd))
                                        .block();
                            }
                    );*/
            return new SlashCommandRegistry(slashCommands);
        }

    }
}
