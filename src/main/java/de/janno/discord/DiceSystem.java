package de.janno.discord;

import de.janno.discord.command.*;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import io.micrometer.core.instrument.Gauge;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import static de.janno.discord.DiscordUtils.getSlashOptionsToString;
import static io.micrometer.core.instrument.Metrics.globalRegistry;

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

        Gauge.builder(Metrics.METRIC_PREFIX + "guildsCount", () -> discordClient.getGuilds().count().blockOptional().orElse(0L))
                .register(globalRegistry);

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
                                return Flux.concat(
                                                event.getInteraction().getGuild()
                                                        .doOnNext(guild -> log.info(String.format("Slash '%s%s' in '%s' from '%s'",
                                                                event.getCommandName(),
                                                                getSlashOptionsToString(event),
                                                                guild.getName(),
                                                                event.getInteraction().getUser().getUsername()))),
                                                Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                                        .filter(command -> command.getName().equals(event.getCommandName()))
                                                        .next()
                                                        .flatMap(command -> command.handleSlashCommandEvent(event))
                                        )
                                        .onErrorResume(e -> {
                                            log.error("SlashCommandEvent Exception: ", e);
                                            return Mono.empty();
                                        });
                            }

                            @Override
                            @NonNull
                            public Publisher<?> onComponentInteraction(@NonNull ComponentInteractionEvent event) {
                                return Flux.concat(
                                                event.getInteraction().getGuild()
                                                        .doOnNext(guild -> log.info(String.format("Button '%s' in '%s' from '%s'",
                                                                event.getCustomId(),
                                                                guild.getName(),
                                                                event.getInteraction().getUser().getUsername()))),
                                                Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                                        .ofType(IComponentInteractEventHandler.class)
                                                        .filter(command -> command.matchingComponentCustomId(event.getCustomId()))
                                                        .next()
                                                        .flatMap(command -> command.handleComponentInteractEvent(event))
                                        )
                                        .onErrorResume(e -> {
                                            log.error("ButtonInteractEvent Exception: ", e);
                                            return Mono.empty();
                                        });
                            }

                            @Override
                            @NonNull
                            public Publisher<?> onGuildCreate(@NonNull GuildCreateEvent event) {
                                log.info("Bot started in guild: name='{}', description='{}', memberCount={}", event.getGuild().getName(),
                                        event.getGuild().getDescription().orElse(""), event.getGuild().getMemberCount());
                                return super.onGuildCreate(event);
                            }
                        }).then(gw.onDisconnect())
                )
                .block();

    }
}
