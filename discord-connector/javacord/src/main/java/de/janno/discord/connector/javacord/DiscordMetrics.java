package de.janno.discord.connector.javacord;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import org.javacord.api.DiscordApi;

import java.time.Duration;
import java.util.Set;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class DiscordMetrics {

    private final static String METRIC_PREFIX = "dice.";
    private final static String METRIC_BUTTON_TIMER_PREFIX = "buttonTimer";
    private final static String METRIC_SLASH_TIMER_PREFIX = "slashTimer";
    private final static String COMMAND_TAG = "command";


    public static void startGuildCountGauge(Set<Long> botInGuildIdSet) {
        Gauge.builder(DiscordMetrics.METRIC_PREFIX + "guildsCount", botInGuildIdSet::size).register(Metrics.globalRegistry);
    }

    public static void startGatewayResponseTime(DiscordApi discordApi) {
        Gauge.builder(DiscordMetrics.METRIC_PREFIX + "gatewayResponseTime", () -> discordApi.getLatestGatewayLatency().toMillis())
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
}
