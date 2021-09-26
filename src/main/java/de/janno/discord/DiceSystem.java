package de.janno.discord;

import de.janno.discord.command.IComponentInteractEventHandler;
import de.janno.discord.command.*;
import de.janno.discord.persistance.IPersistable;
import de.janno.discord.persistance.Persistence;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.interaction.ComponentInteractEvent;
import discord4j.core.event.domain.interaction.SlashCommandEvent;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
public class DiceSystem {

    /**
     * TODO:
     * - slash Commands
     * -- help
     * -- version? git hash?
     * - metrics
     * - optionally moving the button after all messages to the end
     * - optional delay button remove
     * - optional config the max number of dice selection
     * - system that compares slashCommand in code with the current and updates if there are changes
     * - Refactoring
     **/

    /**
     * Discord4J  https://docs.discord4j.com/
     * discord4j examples https://docs.discord4j.com/examples
     * <p>
     * Discord
     * - slash commands https://discord.com/developers/docs/interactions/application-commands#application-command-object
     * - buttons https://discord.com/developers/docs/interactions/message-components
     * <p>
     * Reactor
     * https://projectreactor.io/docs/core/release/reference/index.html
     * <p>
     * alternatives Java Discord Framework
     * https://github.com/DV8FromTheWorld/JDA/
     **/

    public DiceSystem(String token) {
        DiscordClient discordClient = DiscordClient.create(token);
        Snowflake botUserId = discordClient.getCoreResources().getSelfId();


        SlashCommandRegistry slashCommandRegistry = SlashCommandRegistry.builder()
                .addSlashCommand(new FateCommand(botUserId))
                .addSlashCommand(new CountSuccessesCommand(botUserId))
                .addSlashCommand(new StatisticCommand())
                .registerSlashCommands(discordClient);


        Persistence persistence = new Persistence(slashCommandRegistry.getSlashCommands().stream()
                .filter(s -> s instanceof IPersistable)
                .map(s -> (IPersistable) s)
                .collect(Collectors.toList())
        );
        persistence.loadAll();



        discordClient.withGateway(gw -> gw.on(new ReactiveEventAdapter() {

                    @Override
                    @NonNull
                    public Publisher<?> onSlashCommand(@NonNull SlashCommandEvent event) {
                        return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                .filter(command -> command.getName().equals(event.getCommandName()))
                                .next()
                                .flatMap(command -> command.handleSlashCommandEvent(event))
                                .doOnEach(t -> persistence.acceptSaveTrigger(t.get()))
                                .onErrorResume(e -> {
                                    log.error("SlashCommandEvent Exception: ", e);
                                    return Mono.empty();
                                });
                    }


                    @Override
                    @NonNull
                    public Publisher<?> onComponentInteract(@NonNull ComponentInteractEvent event) {
                        return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                .ofType(IComponentInteractEventHandler.class)
                                .flatMap(command -> command.handleComponentInteractEvent(event))
                                .onErrorResume(e -> {
                                    log.error("ButtonInteractEvent Exception: ", e);
                                    return Mono.empty();
                                });
                    }
                }).then(gw.onDisconnect())
        )
                .block();

    }


}
