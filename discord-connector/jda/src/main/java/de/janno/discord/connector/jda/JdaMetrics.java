package de.janno.discord.connector.jda;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import net.dv8tion.jda.api.JDA;

import java.time.Duration;
import java.util.Set;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class JdaMetrics {

    private static final String METRIC_PREFIX = "dice.";
    private static final String METRIC_BUTTON_TIMER_PREFIX = "buttonTimer";
    private static final String METRIC_SLASH_TIMER_PREFIX = "slashTimer";
    private static final String METRIC_WELCOME_COUNTER_PREFIX = "welcomeCounter";
    private static final String COMMAND_TAG = "command";


    public static void startGuildCountGauge(Set<Long> botInGuildIdSet) {
        Gauge.builder(METRIC_PREFIX + "guildsCount", botInGuildIdSet::size).register(Metrics.globalRegistry);
    }

    public static void startGatewayResponseTimeGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "gatewayResponseTime", discordApi::getGatewayPing)
                .register(Metrics.globalRegistry);
    }

    public static void startRestLatencyGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "restLatency", () -> discordApi.getRestPing().complete())
                .register(Metrics.globalRegistry);
    }

    public static void startUserCacheGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "userCacheSize", () -> discordApi.getUserCache().size())
                .register(Metrics.globalRegistry);
    }


    public static void timerButtonMetricCounter(@NonNull String commandName, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + METRIC_BUTTON_TIMER_PREFIX)
                .tags(Tags.of(COMMAND_TAG, commandName))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void timerSlashStartMetricCounter(@NonNull String commandName, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + METRIC_SLASH_TIMER_PREFIX)
                .tags(Tags.of(COMMAND_TAG, commandName))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(globalRegistry)
                .record(duration);
    }

    public static void sendWelcomeMessage() {
        globalRegistry.counter(METRIC_PREFIX + METRIC_WELCOME_COUNTER_PREFIX).increment();
    }
}
