package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import io.avaje.config.Config;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.NewsChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

import static java.lang.Thread.sleep;

@Slf4j
public class JdaClient {

    private final static Set<Integer> SHARD_IDS_NOT_READY_FOR_SHUTDOWN = new ConcurrentSkipListSet<>();

    public JdaClient(@NonNull List<SlashCommand> slashCommands,
                     @NonNull List<ComponentInteractEventHandler> componentInteractEventHandlers,
                     @NonNull Function<DiscordConnector.WelcomeRequest, EmbedOrMessageDefinition> welcomeMessageDefinition,
                     @NonNull Set<Long> allGuildIdsInPersistence) {

        final LocalDateTime welcomeMessageStartTimePlusBuffer = LocalDateTime.now().plus(Duration.of(Config.getLong("welcomeMessageStartTimePlusBufferSec"), ChronoUnit.SECONDS));
        final Scheduler scheduler = Schedulers.boundedElastic();
        final Set<Long> botInGuildIdSet = new ConcurrentSkipListSet<>();
        final Duration timeout = Duration.of(Config.getLong("http.timeoutSec"), ChronoUnit.SECONDS);
        final OkHttpClient okHttpClient = IOUtil.newHttpClientBuilder()
                .eventListener(JdaMetrics.getOkHttpEventListener())
                .writeTimeout(timeout)
                .readTimeout(timeout)
                .connectTimeout(timeout)
                .build();

        JdaMetrics.registerHttpClient(okHttpClient);
        final String token = Config.get("token");
        if (Strings.isNullOrEmpty(token)) {
            throw new IllegalArgumentException("Missing discord token");
        }
        DefaultShardManagerBuilder shardManagerBuilder = DefaultShardManagerBuilder.createLight(token, Collections.emptyList())
                .setHttpClient(okHttpClient)
                .setEnableShutdownHook(false)
                .addEventListeners(
                        new ListenerAdapter() {
                            @Override
                            public void onGuildJoin(@NonNull GuildJoinEvent event) {
                                if (!botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot started in guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.add(event.getGuild().getIdLong());
                                    if (LocalDateTime.now().isAfter(welcomeMessageStartTimePlusBuffer)) {
                                        Optional.ofNullable(event.getGuild().getSystemChannel())
                                                .filter(GuildMessageChannel::canTalk)
                                                .ifPresent(textChannel -> {
                                                    EmbedOrMessageDefinition welcomeMessage = welcomeMessageDefinition.apply(new DiscordConnector.WelcomeRequest(event.getGuild().getIdLong(),
                                                            textChannel.getIdLong(), LocaleConverter.toLocale(event.getGuild().getLocale())));
                                                    Mono.fromFuture(textChannel.sendMessage(
                                                                            MessageComponentConverter.messageComponent2MessageLayout(welcomeMessage.getDescriptionOrContent(),
                                                                                    welcomeMessage.getComponentRowDefinitions()))
                                                                    .submit())
                                                            .doOnSuccess(m -> {
                                                                JdaMetrics.sendWelcomeMessage();
                                                                log.info("Welcome message send in '{}'.'{}'",
                                                                        event.getGuild().getName(),
                                                                        textChannel.getName());
                                                            })
                                                            .subscribeOn(scheduler)
                                                            .subscribe();
                                                });
                                    }
                                }
                            }

                            @Override
                            public void onGuildLeave(@NonNull GuildLeaveEvent event) {
                                if (botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot removed in guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.remove(event.getGuild().getIdLong());
                                }
                            }

                            @Override
                            public void onGuildReady(@NonNull GuildReadyEvent event) {
                                if (!botInGuildIdSet.contains(event.getGuild().getIdLong())) {
                                    log.info("Bot started with guild: name='{}', memberCount={}", event.getGuild().getName(),
                                            event.getGuild().getMemberCount());
                                    botInGuildIdSet.add(event.getGuild().getIdLong());
                                }
                            }

                            @Override
                            public void onReady(@NonNull ReadyEvent event) {
                                long inactiveGuildIdCountWithConfig = allGuildIdsInPersistence.stream()
                                        .filter(id -> !botInGuildIdSet.contains(id))
                                        .count();
                                log.info("Inactive guild count with config: {}", inactiveGuildIdCountWithConfig);
                                //todo wait until all shards are ready
                                sendMessageInNewsChannel(event.getJDA(), "Bot started and is ready");
                            }

                            @Override
                            public void onCommandAutoCompleteInteraction(@NonNull CommandAutoCompleteInteractionEvent event) {
                                Flux.fromIterable(slashCommands)
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .next()
                                        .map(command -> command.getAutoCompleteAnswer(fromEvent(event), LocaleConverter.toLocale(event.getUserLocale())))
                                        .flatMap(a -> Mono.fromFuture(event.replyChoices(a.stream()
                                                .map(c -> new Command.Choice(c.getName(), c.getValue()))
                                                .limit(25)
                                                .toList()).submit()))
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }

                            private AutoCompleteRequest fromEvent(CommandAutoCompleteInteractionEvent event) {
                                return new AutoCompleteRequest(event.getFocusedOption().getName(),
                                        event.getFocusedOption().getValue(),
                                        event.getOptions().stream()
                                                .map(s -> new OptionValue(s.getName(), s.getAsString()))
                                                .toList()
                                );
                            }

                            @Override
                            public void onSlashCommandInteraction(@NonNull SlashCommandInteractionEvent event) {
                                log.trace("ChatInputEvent: {} from {}", event.getInteraction().getCommandId(),
                                        event.getInteraction().getUser().getName());
                                Flux.fromIterable(slashCommands)
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .next()
                                        .flatMap(command -> {
                                            Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());
                                            JdaMetrics.userLocalInteraction(userLocale);
                                            return command.handleSlashCommandEvent(new SlashEventAdapterImpl(event,
                                                    new Requester(event.getInteraction().getUser().getName(),
                                                            event.getChannel().getName(),
                                                            Optional.ofNullable(event.getGuild()).map(Guild::getName).orElse(""),
                                                            event.getJDA().getShardInfo().getShardString(),
                                                            userLocale)
                                            ), UUID::randomUUID, LocaleConverter.toLocale(event.getUserLocale()));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("SlashCommandEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }

                            @Override
                            public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
                                log.trace("ComponentEvent: {} from {}", event.getInteraction().getComponentId(), event.getInteraction().getUser().getName());
                                Flux.fromIterable(componentInteractEventHandlers)
                                        .filter(command -> command.matchingComponentCustomId(event.getInteraction().getComponentId()))
                                        .next()
                                        .flatMap(command -> {
                                            Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());
                                            JdaMetrics.userLocalInteraction(userLocale);
                                            return command.handleComponentInteractEvent(new ButtonEventAdapterImpl(event,
                                                    new Requester(event.getInteraction().getUser().getName(),
                                                            event.getChannel().getName(),
                                                            Optional.ofNullable(event.getInteraction().getGuild()).map(Guild::getName).orElse(""),
                                                            event.getJDA().getShardInfo().getShardString(),
                                                            userLocale
                                                    )));
                                        })
                                        .onErrorResume(e -> {
                                            log.error("ButtonInteractEvent Exception: ", e);
                                            return Mono.empty();
                                        })
                                        .subscribeOn(scheduler)
                                        .subscribe();
                            }
                        }
                )
                .setActivity(Activity.customStatus("Type /quickstart or /help"));
        shardManagerBuilder.setShardsTotal(Config.getInt("shardsTotal", -1));
        Config.getOptional("shardIds", null)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList())
                .ifPresent(shardManagerBuilder::setShards);
        ShardManager shardManager = shardManagerBuilder.build();
        JdaMetrics.startGuildCountGauge(botInGuildIdSet);

        shardManager.getShards().forEach(jda -> {
            try {
                jda.awaitReady();
                JdaMetrics.startGatewayResponseTimeGauge(jda);
                JdaMetrics.startUserCacheGauge(jda);
                JdaMetrics.startShardCountGauge(jda);
                JdaMetrics.startTextChannelCacheGauge(jda);
                JdaMetrics.startGuildCacheGauge(jda);
                JdaMetrics.startRestLatencyGauge(jda);
                SHARD_IDS_NOT_READY_FOR_SHUTDOWN.add(jda.getShardInfo().getShardId());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                shardManager.getShards().parallelStream().forEach(jda -> {
                    log.info("start jda %s shutdown".formatted(jda.getShardInfo().getShardString()));
                    try {
                        sendMessageInNewsChannel(jda, "Bot shutdown started");
                    } finally {
                        SHARD_IDS_NOT_READY_FOR_SHUTDOWN.remove(jda.getShardInfo().getShardId());
                    }
                    waitUntilAllShardsReadyForShutdown(jda.getShardInfo().getShardId());
                    shutdown(jda);
                    log.info("finished jda %s shutdown".formatted(jda.getShardInfo().getShardString()));
                })));

        boolean disableCommandUpdate = Config.getBool("disableCommandUpdate", false);
        SlashCommandRegistry.builder()
                .addSlashCommands(slashCommands)
                .registerSlashCommands(shardManager.getShards().getFirst(), disableCommandUpdate);
    }

    private static void waitUntilAllShardsReadyForShutdown(int shardId) {
        long waitTime = 0;
        final long sleepTime = Config.getInt("waitUntilAllShardsReadyForShutdown.sleepTimeMs", 100);
        final long maxTotalWaitTime = Config.getInt("waitUntilAllShardsReadyForShutdown.maxTotalWaitTimeMs", 10_000);
        while (!SHARD_IDS_NOT_READY_FOR_SHUTDOWN.isEmpty() && waitTime < maxTotalWaitTime) {
            try {
                sleep(sleepTime);
                waitTime += sleepTime;
                log.info("shardId=%s waiting for shards=%s for %sms".formatted(shardId, SHARD_IDS_NOT_READY_FOR_SHUTDOWN, waitTime));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        log.info("All shards are finished for shardId=%s".formatted(shardId));
    }

    private static boolean hasPermission(GuildMessageChannel channel, Permission permission) {
        return Optional.of(channel)
                .flatMap(g -> Optional.of(g.getGuild()).map(Guild::getSelfMember).map(m -> m.hasPermission(g, permission)))
                .orElse(false);
    }

    private static void shutdown(JDA jda) {
        final long shutdownWaitTimeSec = Config.getInt("shutdownWaitTimeSec", 10);
        // Initiating the shutdown, this closes the gateway connection and subsequently closes the requester queue
        jda.shutdown();
        try {
            // Allow at most 5 seconds for remaining requests to finish
            if (!jda.awaitShutdown(Duration.ofSeconds(shutdownWaitTimeSec))) { // returns true if shutdown is graceful, false if timeout exceeded
                log.warn("shutdown took more then 10sec");
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                boolean finishWithoutTimeout = jda.awaitShutdown(Duration.ofSeconds(shutdownWaitTimeSec)); // Wait until shutdown is complete (10 sec)
                if (!finishWithoutTimeout) {
                    log.warn("shutdown now took more then 10sec");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendMessageInNewsChannel(JDA jda, String message) {
        final String newsGuildId = Config.getNullable("newsGuildId");
        final String newsChannelId = Config.getNullable("newsChannelId");

        if (Strings.isNullOrEmpty(newsGuildId)) {
            log.warn("No GuildId for start and shutdown messages");
            return;
        }
        if (Strings.isNullOrEmpty(newsChannelId)) {
            log.warn("No ChannelId for start and shutdown messages");
            return;
        }

        Optional<Guild> guild = Optional.ofNullable(jda.getGuildById(newsGuildId));
        if (guild.isEmpty()) {
            //the guild can be in another shard
            log.info("No news guild for: %s in shard: %s".formatted(newsGuildId, jda.getShardInfo().getShardId()));
            return;
        }
        Optional<StandardGuildMessageChannel> newsChannel = guild.flatMap(g -> Optional.ofNullable(g.getChannelById(StandardGuildMessageChannel.class, newsChannelId)));

        if (newsChannel.isEmpty()) {
            log.warn("Could not find channel for id: " + newsChannelId);
        }

        newsChannel.ifPresent(t -> {
            if (hasPermission(t, Permission.MESSAGE_SEND)) {
                Message sendMessage = t.sendMessage(message).complete();
                log.info("Sent '%s' to '%s'.'%s'".formatted(message, t.getGuild().getName(), t.getName()));
                if (t instanceof NewsChannel) {
                    if (hasPermission(t, Permission.MESSAGE_MANAGE)) {
                        try {
                            Thread.sleep(3000);
                            ((NewsChannel) t).crosspostMessageById(sendMessage.getId()).complete();
                        } catch (Exception e) {
                            log.error("Error while publish: {0}", e);
                        }
                        log.info("Published as news");
                    } else {
                        log.warn("Missing manage message permission for channel id: " + newsChannelId);
                    }
                }
            } else {
                log.warn("Missing send message permission for channel id: " + newsChannelId);
            }
        });
    }
}
