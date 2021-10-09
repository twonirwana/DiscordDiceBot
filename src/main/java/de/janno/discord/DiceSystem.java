package de.janno.discord;

import de.janno.discord.command.*;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DiceSystem {

    /**
     * TODO:
     * - optionally moving the button after all messages to the end
     * - optional delay button remove
     * - optional config the max number of dice selection
     * - details optional
     * - timestamp optional
     * - system that compares slashCommand in code with the current and updates if there are changes
     **/

    public DiceSystem(HttpClient httpClient, String token, boolean updateSlashCommands) {

        DiscordClient discordClient = DiscordClientBuilder.create(token)
                .setReactorResources(ReactorResources.builder()
                        .httpClient(httpClient)
                        .build()).build();

        Snowflake botUserId = discordClient.getCoreResources().getSelfId();
        SlashCommandRegistry slashCommandRegistry = SlashCommandRegistry.builder()
                .addSlashCommand(new CountSuccessesCommand(botUserId))
                .addSlashCommand(new CustomDiceCommand(botUserId))
                .addSlashCommand(new FateCommand(botUserId))
                .addSlashCommand(new DirectRollCommand())
                .addSlashCommand(new HelpCommand())
                .registerSlashCommands(discordClient, updateSlashCommands);

        discordClient.withGateway(gw -> gw.on(new ReactiveEventAdapter() {
                            @Override
                            @NonNull
                            public Publisher<?> onChatInputInteraction(@NonNull ChatInputInteractionEvent event) {
                                return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                        .filter(command -> command.getName().equals(event.getCommandName()))
                                        .next()
                                        .flatMap(command -> command.handleSlashCommandEvent(event))
                                        .onErrorResume(e -> {
                                            log.error("SlashCommandEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .then(event.getInteraction().getGuild()
                                                .doOnNext(guild -> log.info(String.format("Slash '%s%s' in %s from %s",
                                                        event.getCommandName(),
                                                        getSlashOptionsToString(event),
                                                        guild.getName(),
                                                        event.getInteraction().getUser().getUsername())))
                                        );
                            }

                            @Override
                            @NonNull
                            public Publisher<?> onComponentInteraction(@NonNull ComponentInteractionEvent event) {
                                return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                        .ofType(IComponentInteractEventHandler.class)
                                        .filter(command -> command.matchingComponentCustomId(event.getCustomId()))
                                        .next()
                                        .flatMap(command -> command.handleComponentInteractEvent(event))
                                        .onErrorResume(e -> {
                                            log.error("ButtonInteractEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .then(event.getInteraction().getGuild()
                                                .doOnNext(guild -> log.info(String.format("Button '%s' in %s from %s",
                                                        event.getCustomId(),
                                                        guild.getName(),
                                                        event.getInteraction().getUser().getUsername())))
                                        );
                            }
                        }).then(gw.onDisconnect())
                )
                .block();

    }

    private static String getSlashOptionsToString(ChatInputInteractionEvent event) {
        List<String> options = event.getOptions().stream()
                .map(DiceSystem::optionToString)
                .collect(Collectors.toList());
        return options.isEmpty() ? "" : options.toString();
    }

    private static String optionToString(ApplicationCommandInteractionOption option) {
        List<String> subOptions = option.getOptions().stream().map(DiceSystem::optionToString).collect(Collectors.toList());
        return String.format("%s=%s%s",
                option.getName(),
                option.getValue().map(ApplicationCommandInteractionOptionValue::getRaw).orElse(""),
                subOptions.isEmpty() ? "" : subOptions.toString());
    }
}
