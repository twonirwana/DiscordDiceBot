package de.janno.discord.discord4j;

import com.google.common.base.Stopwatch;
import de.janno.discord.Metrics;
import de.janno.discord.api.IComponentInteractEventHandler;
import de.janno.discord.api.Requester;
import de.janno.discord.command.*;
import discord4j.common.ReactorResources;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.ReactiveEventAdapter;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.gateway.GatewayClient;
import io.micrometer.core.instrument.Gauge;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static de.janno.discord.command.AbstractCommand.CONFIG_DELIMITER;
import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class Discord4JClient {

    /**
     * TODO:
     * - optionally moving the button after all messages to the end
     * - optional delay button remove
     * - details optional
     * - timestamp optional
     * - Changelog Command
     * - Old WoD Command, first dice pool, then in second message the target
     **/

    public Discord4JClient(HttpClient httpClient, String token, boolean disableCommandUpdate) {

        DiscordClient discordClient = DiscordClientBuilder.create(token)
                .setReactorResources(ReactorResources.builder()
                        .httpClient(httpClient)
                        .build()).build();

        Set<Snowflake> botInGuildIdSet = new ConcurrentSkipListSet<>();
        Gauge.builder(Metrics.METRIC_PREFIX + "guildsCount", botInGuildIdSet::size).register(globalRegistry);

        SlashCommandRegistry slashCommandRegistry = SlashCommandRegistry.builder()
                .addSlashCommand(new CountSuccessesCommand())
                .addSlashCommand(new CustomDiceCommand())
                .addSlashCommand(new FateCommand())
                .addSlashCommand(new DirectRollCommand())
                .addSlashCommand(new SumDiceSetCommand())
                .addSlashCommand(new SumCustomSetCommand())
                .addSlashCommand(new HoldRerollCommand())
                .addSlashCommand(new PoolTargetCommand())
                .addSlashCommand(new HelpCommand())
                .registerSlashCommands(discordClient, disableCommandUpdate);

        discordClient.withGateway(gw -> {
                            Gauge.builder(Metrics.METRIC_PREFIX + "gatewayResponseTime", () ->
                                    gw.getGatewayClient(0)
                                            .map(GatewayClient::getResponseTime)
                                            .map(Duration::toMillis).orElse(-1L)).register(globalRegistry);
                            return gw.on(new ReactiveEventAdapter() {

                                @Override
                                @NonNull
                                public Publisher<?> onChatInputInteraction(@NonNull ChatInputInteractionEvent event) {
                                    Stopwatch stopwatch = Stopwatch.createStarted();
                                    log.trace("ChatInputEvent: {} from {}", event.getCommandName(), event.getInteraction().getUser().getUsername());
                                    return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                            .filter(command -> command.getName().equals(event.getCommandName()))
                                            .next()
                                            .flatMap(command -> command.handleSlashCommandEvent(new SlashEventAdapter(event,
                                                    Mono.zip(event.getInteraction().getChannel()
                                                                    .ofType(TextChannel.class)
                                                                    .map(TextChannel::getName),
                                                            event.getInteraction().getGuild().map(Guild::getName)
                                                    ).map(channelAndGuild -> new Requester(event.getInteraction().getUser().getUsername(),
                                                            channelAndGuild.getT1(), channelAndGuild.getT2()))
                                            )))
                                            .onErrorResume(e -> {
                                                log.error("SlashCommandEvent Exception: ", e);
                                                return Mono.empty();
                                            })
                                            .doAfterTerminate(() -> Metrics.timerSlashStartMetricCounter(event.getCommandName(), stopwatch.elapsed()));
                                }

                                @Override
                                @NonNull
                                public Publisher<?> onComponentInteraction(@NonNull ComponentInteractionEvent event) {
                                    Stopwatch stopwatch = Stopwatch.createStarted();
                                    log.trace("ComponentEvent: {} from {}", event.getCustomId(), event.getInteraction().getUser().getUsername());
                                    return Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                                            .ofType(IComponentInteractEventHandler.class)
                                            .filter(command -> command.matchingComponentCustomId(event.getCustomId()))
                                            .next()
                                            .flatMap(command -> command.handleComponentInteractEvent(new ButtonEventAdapter(event,
                                                    Mono.zip(event.getInteraction().getChannel()
                                                                    .ofType(TextChannel.class)
                                                                    .map(TextChannel::getName),
                                                            event.getInteraction().getGuild().map(Guild::getName)
                                                    ).map(channelAndGuild -> new Requester(event.getInteraction().getUser().getUsername(),
                                                            channelAndGuild.getT1(), channelAndGuild.getT2())))))
                                            .onErrorResume(e -> {
                                                log.error("ButtonInteractEvent Exception: ", e);
                                                return Mono.empty();
                                            })
                                            .doAfterTerminate(() -> Metrics.timerButtonMetricCounter(getCommandNameFromCustomId(event.getCustomId()), stopwatch.elapsed()));
                                }

                                @Override
                                @NonNull
                                public Publisher<?> onGuildCreate(@NonNull GuildCreateEvent event) {
                                    if (!botInGuildIdSet.contains(event.getGuild().getId())) {
                                        log.info("Bot started in guild: name='{}', description='{}', memberCount={}", event.getGuild().getName(),
                                                event.getGuild().getDescription().orElse(""), event.getGuild().getMemberCount());
                                        botInGuildIdSet.add(event.getGuild().getId());
                                    }
                                    return super.onGuildCreate(event);
                                }

                                @Override
                                @NonNull
                                public Publisher<?> onGuildDelete(@NonNull GuildDeleteEvent event) {
                                    if (!event.isUnavailable() && event.getGuild().isPresent()) {
                                        if (botInGuildIdSet.contains(event.getGuild().get().getId())) {
                                            log.info("Bot removed in guild: name='{}', description='{}', memberCount={}", event.getGuild().map(Guild::getName).orElse(""),
                                                    event.getGuild().flatMap(Guild::getDescription).orElse(""), event.getGuild().map(Guild::getMemberCount).orElse(0));
                                            botInGuildIdSet.remove(event.getGuild().get().getId());
                                        }
                                    }
                                    return super.onGuildDelete(event);
                                }
                            }).then(gw.onDisconnect());
                        }
                )
                .block();

    }

    private static String getCommandNameFromCustomId(String customId) {
        return customId.split(CONFIG_DELIMITER)[0];
    }
}
