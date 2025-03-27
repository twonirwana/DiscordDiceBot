package de.janno.discord.connector.jda;

import com.google.common.base.Strings;
import de.janno.discord.connector.api.*;
import io.avaje.config.Config;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
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
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.internal.utils.IOUtil;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;

@Slf4j
public class JdaClient {

    private final static String WITHOUT_GUILD = "without_guild";

    public JdaClient(@NonNull List<SlashCommand> slashCommands,
                     @NonNull List<ComponentCommand> componentCommands,
                     @NonNull WelcomeMessageCreator welcomeMessageCreator,
                     @NonNull Set<Long> allGuildIdsInPersistence) {

        final LocalDateTime startTime = LocalDateTime.now();
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
                                    if (LocalDateTime.now().isAfter(startTime.plus(Duration.of(Config.getLong("welcomeMessageStartTimePlusBufferSec"), ChronoUnit.SECONDS)))) {
                                        Optional.ofNullable(event.getGuild().getSystemChannel())
                                                .filter(GuildMessageChannel::canTalk)
                                                .ifPresent(textChannel -> {
                                                    final WelcomeMessageCreator.WelcomeRequest request = new WelcomeMessageCreator.WelcomeRequest(event.getGuild().getIdLong(),
                                                            textChannel.getIdLong(), LocaleConverter.toLocale(event.getGuild().getLocale()));
                                                    final WelcomeMessageCreator.MessageAndConfigId welcomeMessageAndConfigId = welcomeMessageCreator.getWelcomeMessage(request);
                                                    createMonoFrom(() -> textChannel.sendMessage(
                                                            MessageComponentConverter.messageComponent2MessageLayout(welcomeMessageAndConfigId.embedOrMessageDefinition().getDescriptionOrContent(),
                                                                    welcomeMessageAndConfigId.embedOrMessageDefinition().getComponentRowDefinitions())))
                                                            .doOnNext(m -> welcomeMessageCreator.processMessageId(request, welcomeMessageAndConfigId.configUUID(), m.getIdLong()))
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
                                List<SlashCommand> matchingHandler = slashCommands.stream()
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .toList();

                                Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());

                                Requester requester = new Requester(event.getInteraction().getUser().getName(),
                                        event.getChannel().getName(),
                                        getGuildName(event.getInteraction()),
                                        event.getJDA().getShardInfo().getShardString(),
                                        userLocale,
                                        null);
                                if (matchingHandler.size() != 1) {
                                    log.error("{}: Invalid handler for {} -> {}", requester.toLogString(), event.getInteraction().getCommandId(), matchingHandler.stream().map(SlashCommand::getCommandId).toList());
                                } else {
                                    Mono.just(matchingHandler.getFirst())
                                            .map(command -> command.getAutoCompleteAnswer(fromEvent(event),
                                                    LocaleConverter.toLocale(event.getUserLocale()),
                                                    event.getChannel().getIdLong(),
                                                    Optional.ofNullable(event.getGuild()).map(ISnowflake::getIdLong).orElse(null),
                                                    event.getUser().getIdLong()))
                                            .flatMap(a -> createMonoFrom(() -> event.replyChoices(a.stream()
                                                    .filter(alias -> StringUtils.isNoneBlank(alias.getName(), alias.getValue()))
                                                    .map(c -> new Command.Choice(c.getName(), c.getValue()))
                                                    .limit(25)
                                                    .toList())
                                            ))
                                            .onErrorResume(t -> {
                                                //todo better?
                                                log.error(t.getMessage(), t);
                                                return Mono.empty();
                                            })
                                            .subscribeOn(scheduler)
                                            .subscribe();
                                }
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
                                List<SlashCommand> matchingHandler = slashCommands.stream()
                                        .filter(command -> command.getCommandId().equals(event.getName()))
                                        .toList();

                                Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());

                                //no central slash handler, therefore we set the metric here
                                if (event.isFromGuild() && !event.isFromAttachedGuild()) { //has guild but it is not attached
                                    JdaMetrics.userInstallSlashCommand();
                                }

                                Requester requester = new Requester(event.getInteraction().getUser().getName(),
                                        event.getChannel().getName(),
                                        getGuildName(event.getInteraction()),
                                        event.getJDA().getShardInfo().getShardString(),
                                        userLocale,
                                        null);
                                if (matchingHandler.size() != 1) {
                                    log.error("{}: Invalid handler for {} -> {}", requester.toLogString(), event.getInteraction().getCommandId(), matchingHandler.stream().map(SlashCommand::getCommandId).toList());
                                } else {
                                    Mono.just(matchingHandler.getFirst())
                                            .flatMap(command -> {
                                                JdaMetrics.userLocalInteraction(userLocale);
                                                return command.handleSlashCommandEvent(new SlashEventAdapterImpl(event,
                                                        requester
                                                ), UUID::randomUUID, LocaleConverter.toLocale(event.getUserLocale()));
                                            })
                                            .onErrorResume(e -> {
                                                log.error("SlashCommandEvent Exception: ", e);
                                                return Mono.empty();
                                            })
                                            .subscribeOn(scheduler)
                                            .subscribe();
                                }
                            }

                            @Override
                            public void onStringSelectInteraction(@NonNull StringSelectInteractionEvent event) {
                                //todo multi selection support
                                String value = !event.getSelectedOptions().isEmpty() ? event.getSelectedOptions().getFirst().getValue() : null;
                                if (value == null) {
                                    //todo better handling of deselect with event.getComponentId()
                                    event.getInteraction().deferEdit().complete();
                                    //deselect of selection
                                    return;
                                }
                                onComponentEvent(value, event, componentCommands, scheduler);
                            }

                            @Override
                            public void onButtonInteraction(@NonNull ButtonInteractionEvent event) {
                                onComponentEvent(event.getComponentId(), event, componentCommands, scheduler);
                            }
                        }
                )
                .setActivity(Activity.customStatus("Type /quickstart or /help"));
        final int shardsTotal = Config.getInt("shardsTotal", -1);
        log.info("Configured ShardTotal: {}", shardsTotal);
        shardManagerBuilder.setShardsTotal(shardsTotal);
        Optional<List<Integer>> shardIds = Config.getOptional("shardIds", null)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .map(Integer::parseInt)
                        .toList());
        log.info("Configured ShardIds: {}", shardIds.orElse(List.of()));
        shardIds.ifPresent(shardManagerBuilder::setShards);
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
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shardManager.getShards().forEach(jda -> sendMessageInNewsChannel(jda, "Bot shutdown started"));
            shardManager.getShards().parallelStream().forEach(JdaClient::shutdown);
        }));

        boolean disableCommandUpdate = Config.getBool("disableCommandUpdate", false);
        SlashCommandRegistry.builder()
                .addSlashCommands(slashCommands)
                .registerSlashCommands(shardManager.getShards().getFirst(), disableCommandUpdate);
    }

    private static void onComponentEvent(final String customId, GenericComponentInteractionCreateEvent event, List<ComponentCommand> componentCommands, Scheduler scheduler) {
        log.trace("ComponentEvent: {} from {}", customId, event.getInteraction().getUser().getName());
        if (!BottomCustomIdUtils.isValidCustomId(customId)) {
            log.warn("Custom id {} is not a valid custom id.", customId);
        }
        List<ComponentCommand> matchingHandler = componentCommands.stream()
                .filter(command -> command.matchingComponentCustomId(customId))
                .toList();
        Locale userLocale = LocaleConverter.toLocale(event.getInteraction().getUserLocale());

        Requester requester = new Requester(event.getInteraction().getUser().getName(),
                event.getChannel().getName(),
                getGuildName(event.getInteraction()),
                event.getJDA().getShardInfo().getShardString(),
                userLocale,
                BottomCustomIdUtils.getConfigUUIDFromCustomId(customId).orElse(null));
        if (matchingHandler.size() != 1) {
            log.error("{}: Invalid handler for {} -> {}", requester.toLogString(), customId, matchingHandler.stream().map(ComponentCommand::getCommandId).toList());
        } else {
            Mono.just(matchingHandler.getFirst())
                    .flatMap(command -> {
                        JdaMetrics.userLocalInteraction(userLocale);
                        return command.handleComponentInteractEvent(new ButtonEventAdapterImpl(customId, event, requester));
                    })
                    .onErrorResume(e -> {
                        log.error("{} - ButtonInteractEvent Exception: ", requester.toLogString(), e);
                        return Mono.empty();
                    })
                    .subscribeOn(scheduler)
                    .subscribe();
        }
    }

    private static boolean hasPermission(GuildMessageChannel channel, Permission permission) {
        return Optional.of(channel)
                .flatMap(g -> Optional.of(g.getGuild()).map(Guild::getSelfMember).map(m -> m.hasPermission(g, permission)))
                .orElse(false);
    }

    private static String getGuildName(Interaction interaction) {
        if (!interaction.isFromAttachedGuild()) {
            return WITHOUT_GUILD;
        }
        return Optional.ofNullable(interaction.getGuild()).map(Guild::getName).orElse(WITHOUT_GUILD);
    }

    private static void shutdown(JDA jda) {
        log.info("ShardId={}: start jda shutdown", jda.getShardInfo().getShardString());
        final long shutdownWaitTimeSec = Config.getInt("shutdownWaitTimeSec", 10);
        // Initiating the shutdown, this closes the gateway connection and subsequently closes the requester queue
        jda.shutdown();
        try {
            // Allow some seconds for remaining requests to finish
            if (!jda.awaitShutdown(Duration.ofSeconds(shutdownWaitTimeSec))) { // returns true if shutdown is graceful, false if timeout exceeded
                log.warn("ShardId={}: shutdown took more then {}sec", jda.getShardInfo().getShardString(), shutdownWaitTimeSec);
                jda.shutdownNow(); // Cancel all remaining requests, and stop thread-pools
                boolean finishWithoutTimeout = jda.awaitShutdown(Duration.ofSeconds(shutdownWaitTimeSec)); // Wait until shutdown is complete (10 sec)
                if (!finishWithoutTimeout) {
                    log.warn("ShardId={}: shutdown now took more then {}sec", jda.getShardInfo().getShardString(), shutdownWaitTimeSec);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("ShardId={}: finished jda shutdown", jda.getShardInfo().getShardString());
    }

    private static void sendMessageInNewsChannel(JDA jda, String message) {
        if (!Config.getBool("sendStatusMessage", true)) {
            return;
        }
        final String newsGuildId = Config.getNullable("newsGuildId");
        final String newsChannelId = Config.getNullable("newsChannelId");

        if (Strings.isNullOrEmpty(newsGuildId)) {
            log.warn("ShardId={}: No GuildId for start and shutdown messages", jda.getShardInfo().getShardString());
            return;
        }
        if (Strings.isNullOrEmpty(newsChannelId)) {
            log.warn("ShardId={}: No ChannelId for start and shutdown messages", jda.getShardInfo().getShardString());
            return;
        }

        Optional<Guild> guild = Optional.ofNullable(jda.getGuildById(newsGuildId));
        if (guild.isEmpty()) {
            //the guild can be in another shard
            log.info("ShardId={}: No news guild for: {}", jda.getShardInfo().getShardString(), newsGuildId);
            return;
        }
        Optional<StandardGuildMessageChannel> newsChannel = guild.flatMap(g -> Optional.ofNullable(g.getChannelById(StandardGuildMessageChannel.class, newsChannelId)));

        if (newsChannel.isEmpty()) {
            log.warn("ShardId={}: Could not find channel for id: {}", jda.getShardInfo().getShardString(), newsChannelId);
        }

        newsChannel.ifPresent(t -> {
            if (hasPermission(t, Permission.MESSAGE_SEND)) {
                Message sendMessage = t.sendMessage(message).complete();
                log.info("ShardId={}: Sent '{}' to '{}'.'{}'", jda.getShardInfo().getShardString(), message, t.getGuild().getName(), t.getName());
                if (t instanceof NewsChannel n) {
                    if (hasPermission(t, Permission.MESSAGE_MANAGE)) {
                        try {
                            //without the wait time the crosspost resulted often rate limit error
                            Thread.sleep(Config.getInt("newsChannelPublishWaitMilliSec", 1000));
                            n.crosspostMessageById(sendMessage.getId()).complete();
                        } catch (Exception e) {
                            log.error("Error while publish: {0}", e);
                        }
                        log.info("ShardId={}: Published as news", jda.getShardInfo().getShardString());
                    } else {
                        log.warn("ShardId={}: Missing manage message permission for channel id: {}", newsChannelId, jda.getShardInfo().getShardString());
                    }
                }
            } else {
                log.warn("ShardId={}: Missing send message permission for channel id: {}", newsChannelId, newsChannelId);
            }
        });
    }

    protected static <T> Mono<T> createMonoFrom(Supplier<RestAction<T>> actionSupplier) {
        try {
            return Mono.fromFuture(actionSupplier.get().submit());
        } catch (Throwable t) {
            return Mono.error(t);
        }
    }
}
