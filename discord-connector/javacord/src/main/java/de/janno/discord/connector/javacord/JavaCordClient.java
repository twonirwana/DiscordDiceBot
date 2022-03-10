package de.janno.discord.connector.javacord;

import com.google.common.base.Stopwatch;
import de.janno.discord.connector.api.IComponentInteractEventHandler;
import de.janno.discord.connector.api.ISlashCommand;
import de.janno.discord.connector.api.Requester;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.Nameable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Slf4j
public class JavaCordClient {

    /**
     * TODO:
     * - optionally moving the button after all messages to the end
     * - optional delay button remove
     * - details optional
     * - timestamp optional
     * - Changelog Command
     * - Old WoD Command, first dice pool, then in second message the target
     **/

    public void start(String token, boolean disableCommandUpdate, List<ISlashCommand> commands) {
        Set<Long> botInGuildIdSet = new ConcurrentSkipListSet<>();

        DiscordApi api = new DiscordApiBuilder()
                .setWaitForServersOnStartup(false)
                //todo rate?
                .setUserCacheEnabled(true)
                .addServerBecomesAvailableListener(event -> {
                    if (!botInGuildIdSet.contains(event.getServer().getId())) {
                        log.info("Bot started with guild: name='{}', description='{}', memberCount={}", event.getServer().getName(),
                                event.getServer().getDescription().orElse(""), event.getServer().getMemberCount());
                        botInGuildIdSet.add(event.getServer().getId());
                    }
                })
                .addServerJoinListener(event -> {
                    if (!botInGuildIdSet.contains(event.getServer().getId())) {
                        log.info("Bot started in guild: name='{}', description='{}', memberCount={}", event.getServer().getName(),
                                event.getServer().getDescription().orElse(""), event.getServer().getMemberCount());
                        botInGuildIdSet.add(event.getServer().getId());
                    }
                })
                .addServerLeaveListener(event -> {
                    if (botInGuildIdSet.contains(event.getServer().getId())) {
                        log.info("Bot removed in guild: name='{}', description='{}', memberCount={}", event.getServer().getName(),
                                event.getServer().getDescription().orElse(""), event.getServer().getMemberCount());
                        botInGuildIdSet.remove(event.getServer().getId());
                    }
                })
                .setToken(token).login()
                .join();

        //todo
      /*  Gauge.builder(Metrics.METRIC_PREFIX + "guildsCount", botInGuildIdSet::size).register(Metrics.globalRegistry);
        Gauge.builder(Metrics.METRIC_PREFIX + "gatewayResponseTime", () -> api.getLatestGatewayLatency().toMillis())
                .register(Metrics.globalRegistry);*/

        SlashCommandRegistry slashCommandRegistry = SlashCommandRegistry.builder()
                .addSlashCommands(commands)
                .registerSlashCommands(api, disableCommandUpdate);

        Scheduler scheduler = Schedulers.boundedElastic();

        api.addSlashCommandCreateListener(event -> {
            Stopwatch stopwatch = Stopwatch.createStarted();

            log.trace("ChatInputEvent: {} from {}", event.getSlashCommandInteraction().getCommandName(),
                    event.getInteraction().getUser().getName());
            Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                    .filter(command -> command.getName().equals(event.getSlashCommandInteraction().getCommandName()))
                    .next()
                    .flatMap(command -> command.handleSlashCommandEvent(new SlashEventAdapter(event,
                            Mono.just(new Requester(event.getInteraction().getUser().getName(),
                                    event.getSlashCommandInteraction().getChannel().flatMap(tc ->
                                            api.getServerChannelById(tc.getId())).map(Nameable::getName).orElse(""),
                                    event.getSlashCommandInteraction().getServer().map(Nameable::getName).orElse("")))
                    )))
                    .onErrorResume(e -> {
                        log.error("SlashCommandEvent Exception: ", e);
                        return Mono.empty();
                    })
                    //todo
                    /*.doAfterTerminate(() ->
                            Metrics.timerSlashStartMetricCounter(event.getSlashCommandInteraction().getCommandName(), stopwatch.elapsed())
                    )*/
                    .subscribeOn(scheduler)
                    .subscribe();
        });

        api.addButtonClickListener(event -> {
            Stopwatch stopwatch = Stopwatch.createStarted();
            log.trace("ComponentEvent: {} from {}", event.getButtonInteraction().getCustomId(), event.getInteraction().getUser().getName());
            Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                    .ofType(IComponentInteractEventHandler.class)
                    .filter(command -> command.matchingComponentCustomId(event.getButtonInteraction().getCustomId()))
                    .next()
                    .flatMap(command -> command.handleComponentInteractEvent(new ButtonEventAdapter(event,
                            Mono.just(new Requester(event.getInteraction().getUser().getName(),
                                    event.getButtonInteraction().getChannel().flatMap(tc ->
                                            api.getServerChannelById(tc.getId())).map(Nameable::getName).orElse(""),
                                    event.getButtonInteraction().getServer().map(Nameable::getName).orElse("")
                            )))))
                    .onErrorResume(e -> {
                        log.error("ButtonInteractEvent Exception: ", e);
                        return Mono.empty();
                    })
                    //todo
                    /*   .doAfterTerminate(() ->
                               Metrics.timerButtonMetricCounter(getCommandNameFromCustomId(event.getButtonInteraction().getCustomId()), stopwatch.elapsed())
                       )*/
                    .subscribeOn(scheduler)
                    .subscribe();
        });

    }

    /*private static String getCommandNameFromCustomId(String customId) {
        return customId.split(CONFIG_DELIMITER)[0];
    }*/
}
