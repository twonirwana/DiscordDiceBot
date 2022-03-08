package de.janno.discord.discord4j;

import com.google.common.base.Stopwatch;
import de.janno.discord.Metrics;
import de.janno.discord.api.IComponentInteractEventHandler;
import de.janno.discord.api.Requester;
import de.janno.discord.command.*;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.Nameable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import static de.janno.discord.command.AbstractCommand.CONFIG_DELIMITER;
import static io.micrometer.core.instrument.Metrics.globalRegistry;

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

    public JavaCordClient(String token, boolean disableCommandUpdate) {
        DiscordApi api = new DiscordApiBuilder()
                //todo rate?
                .setToken(token).login().join();

       /* DiscordClient discordClient = DiscordClientBuilder.create(token)
                .onClientResponse(
                        ResponseFunction.retryWhen(
                                RouteMatcher.any(),
                                Retry.anyOf(Errors.NativeIoException.class)))
                .setReactorResources(ReactorResources.builder()
                        .httpClient(httpClient)
                        .build()).build();*/

        Set<Long> botInGuildIdSet = new ConcurrentSkipListSet<>();
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
                .registerSlashCommands(api, disableCommandUpdate);

        api.addSlashCommandCreateListener(event -> {
            Stopwatch stopwatch = Stopwatch.createStarted();

            log.trace("ChatInputEvent: {} from {}", event.getSlashCommandInteraction().getCommandName(),
                    event.getInteraction().getUser().getName());
            Flux.fromIterable(slashCommandRegistry.getSlashCommands())
                    .filter(command -> command.getName().equals(event.getSlashCommandInteraction().getCommandName()))
                    .next()
                    .flatMap(command -> command.handleSlashCommandEvent(new SlashEventAdapter(event,
                            Mono.just(new Requester(event.getInteraction().getUser().getName(),
                                    "",//TODO
                                    event.getSlashCommandInteraction().getServer().map(Nameable::getName).orElse("")))
                    )))
                    .onErrorResume(e -> {
                        log.error("SlashCommandEvent Exception: ", e);
                        return Mono.empty();
                    })
                    .doAfterTerminate(() -> Metrics.timerSlashStartMetricCounter(event.getSlashCommandInteraction().getCommandName(), stopwatch.elapsed()))
                    .subscribe(); //TODO subscribe on a single threadpool

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
                                    "",//TODO
                                    event.getButtonInteraction().getServer().map(Nameable::getName).orElse(""))))))
                    .onErrorResume(e -> {
                        log.error("ButtonInteractEvent Exception: ", e);
                        return Mono.empty();
                    })
                    .doAfterTerminate(() -> Metrics.timerButtonMetricCounter(getCommandNameFromCustomId(event.getButtonInteraction().getCustomId()), stopwatch.elapsed()))
                    .subscribe(); //TODO subscribe on central threadpool

        });

        api.addServerBecomesAvailableListener(event -> {
            //TODO dont work on startup
            if (!botInGuildIdSet.contains(event.getServer().getId())) {
                log.info("Bot started in guild: name='{}', description='{}', memberCount={}", event.getServer().getName(),
                        event.getServer().getDescription().orElse(""), event.getServer().getMemberCount());
                botInGuildIdSet.add(event.getServer().getId());
            }
        });

        api.addServerBecomesUnavailableListener(event -> {
            if (botInGuildIdSet.contains(event.getServer().getId())) {
                log.info("Bot removed in guild: name='{}', description='{}', memberCount={}", event.getServer().getName(),
                        event.getServer().getDescription().orElse(""), event.getServer().getMemberCount());
                botInGuildIdSet.remove(event.getServer().getId());
            }

        });

        /*
                                    Gauge.builder(Metrics.METRIC_PREFIX + "gatewayResponseTime", () ->
                                    gw.getGatewayClient(0)
                                            .map(GatewayClient::getResponseTime)
                                            .map(Duration::toMillis).orElse(-1L)).register(globalRegistry);
         */

    }

    private static String getCommandNameFromCustomId(String customId) {
        return customId.split(CONFIG_DELIMITER)[0];
    }
}
