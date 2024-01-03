package de.janno.discord.bot;

import com.google.common.base.Strings;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.SimpleFileServer;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import io.avaje.config.Config;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class BotMetrics {

    private final static String METRIC_PREFIX = "dice.";
    private final static String METRIC_BUTTON_PREFIX = "buttonEvent";
    private final static String METRIC_DATABASE_PREFIX = "database";
    private final static String METRIC_ANSWER_DELAY_PREFIX = "answerDelayDuration";
    private final static String METRIC_IMAGE_CREATION_DURATION_PREFIX = "imageCreationDuration";
    private final static String METRIC_IS_DELAYED_PREFIX = "answerIsDelayed";
    private final static String METRIC_LEGACY_BUTTON_PREFIX = "legacyButtonEvent";
    private final static String METRIC_UUID_BUTTON_PREFIX = "uuidButtonEvent";
    private final static String METRIC_SLASH_PREFIX = "slashEvent";
    private final static String METRIC_SLASH_HELP_PREFIX = "slashHelpEvent";
    private final static String METRIC_IMAGE_RESULT_PREFIX = "imageResult";
    private final static String METRIC_USE_IMAGE_RESULT_PREFIX = "useImageResult";
    private final static String METRIC_AUTOCOMPLETE_VALID_PREFIX = "autoCompleteValid";
    private final static String METRIC_USE_ALIAS_PREFIX = "useAlias";
    private final static String METRIC_ANSWER_FORMAT_PREFIX = "answerFormat";
    private final static String METRIC_DICE_PARSER_SYSTEM_PREFIX = "diceParserSystem";
    private final static String CONFIG_TAG = "config";
    private final static String COMMAND_TAG = "command";
    private final static String UUID_USAGE_TAG = "uuidUsage";
    private final static String CACHE_TAG = "cache";
    private final static String IMAGE_RESULT_TAG = "imageResult";
    private final static String TYPE_TAG = "type";
    private final static String ALIAS_TAG = "alias";
    private final static String VALID_TAG = "valid";
    private final static String ANSWER_FORMAT_TAG = "answerFormat";
    private final static String DICE_SYSTEM_TAG = "diceSystem";
    private final static String ACTION_TAG = "action";
    private final static String DELAYED_TAG = "delayed";
    private static final String ANSWER_TIMER_PREFIX = "answerTimer";
    private static final String NEW_BUTTON_TIMER_PREFIX = "newButtonTimer";
    private static final String publishMetricsToUrl = Config.get("metric.url", "localhost");
    private static final int publishPort = Config.getInt("metric.port", 8080);

    public static void init() throws IOException {
        if (!Strings.isNullOrEmpty(publishMetricsToUrl)) {
            PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            Metrics.addRegistry(prometheusRegistry);
            new UptimeMetrics().bindTo(globalRegistry);

            HttpServer server = HttpServer.create(new InetSocketAddress(publishMetricsToUrl, publishPort),
                    SimpleFileServer.OutputLevel.INFO.ordinal(),
                    "/prometheus",
                    exchange -> {
                        final var bytes = prometheusRegistry.scrape().getBytes(StandardCharsets.UTF_8);
                        try (exchange) {
                            exchange.getRequestBody().readAllBytes();
                            exchange.sendResponseHeaders(200, bytes.length);
                            exchange.getResponseBody().write(bytes);
                        }
                    }
            );

            server.start();

            prometheusRegistry.config().commonTags("application", "DiscordDiceBot");
            new JvmMemoryMetrics().bindTo(globalRegistry);
            new JvmGcMetrics().bindTo(globalRegistry);
            new ProcessorMetrics().bindTo(globalRegistry);
            new JvmThreadMetrics().bindTo(globalRegistry);
            new LogbackMetrics().bindTo(globalRegistry);
            new ClassLoaderMetrics().bindTo(globalRegistry);
            new JvmHeapPressureMetrics().bindTo(globalRegistry);
            new JvmInfoMetrics().bindTo(globalRegistry);
        }
    }


    public static void incrementButtonMetricCounter(@NonNull String commandName, @NonNull String configString) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_BUTTON_PREFIX, Tags.of(COMMAND_TAG, commandName, CONFIG_TAG, configString)).increment();
    }

    public static void incrementLegacyButtonMetricCounter(@NonNull String commandName) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_LEGACY_BUTTON_PREFIX, Tags.of(COMMAND_TAG, commandName)).increment();
    }

    public static void incrementButtonUUIDUsageMetricCounter(@NonNull String commandName, boolean hasUUIDinCustomId) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_UUID_BUTTON_PREFIX, Tags.of(COMMAND_TAG, commandName, UUID_USAGE_TAG, String.valueOf(hasUUIDinCustomId))).increment();
    }

    public static void incrementSlashStartMetricCounter(@NonNull String commandName, @NonNull String configString) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_SLASH_PREFIX, Tags.of(COMMAND_TAG, commandName, CONFIG_TAG, configString)).increment();
    }

    public static void incrementSlashHelpMetricCounter(@NonNull String commandName) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_SLASH_HELP_PREFIX, Tags.of(COMMAND_TAG, commandName)).increment();
    }

    public static void incrementDiceParserSystemCounter(@NonNull DiceParserSystem diceParserSystem) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_DICE_PARSER_SYSTEM_PREFIX, Tags.of(DICE_SYSTEM_TAG, diceParserSystem.name())).increment();
    }

    public static void incrementAnswerFormatCounter(@NonNull AnswerFormatType answerFormatType, @NonNull String commandName) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_ANSWER_FORMAT_PREFIX, Tags.of(
                COMMAND_TAG, commandName,
                ANSWER_FORMAT_TAG, answerFormatType.name()
        )).increment();
    }

    public static void incrementImageResultMetricCounter(@NonNull CacheTag tag) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_IMAGE_RESULT_PREFIX, Tags.of(CACHE_TAG, tag.name())).increment();
    }


    public static void incrementUseImageResultMetricCounter(@NonNull DiceStyleAndColor resultImage) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_USE_IMAGE_RESULT_PREFIX, Tags.of(IMAGE_RESULT_TAG, resultImage.toString())).increment();
    }

    public static void incrementAliasUseMetricCounter(@NonNull String type, @NonNull String alias) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_USE_ALIAS_PREFIX, Tags.of(TYPE_TAG, type, ALIAS_TAG, alias)).increment();
    }


    public static void databaseTimer(@NonNull String action, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + METRIC_DATABASE_PREFIX)
                .tags(Tags.of(ACTION_TAG, action))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void delayTimer(@NonNull String commandName, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + METRIC_ANSWER_DELAY_PREFIX)
                .tags(Tags.of(COMMAND_TAG, commandName))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void imageCreationTimer(@NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + METRIC_IMAGE_CREATION_DURATION_PREFIX)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void incrementDelayCounter(@NonNull String commandName, boolean isDelayed) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_IS_DELAYED_PREFIX, Tags.of(COMMAND_TAG, commandName, DELAYED_TAG, String.valueOf(isDelayed))).increment();
    }

    public static void timerAnswerMetricCounter(@NonNull String commandName, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + ANSWER_TIMER_PREFIX)
                .tags(Tags.of(COMMAND_TAG, commandName))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void timerNewButtonMessageMetricCounter(@NonNull String commandName, @NonNull Duration duration) {
        Timer.builder(METRIC_PREFIX + NEW_BUTTON_TIMER_PREFIX)
                .tags(Tags.of(COMMAND_TAG, commandName))
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram(true)
                .register(globalRegistry)
                .record(duration);
    }

    public static void incrementValidationCounter(boolean valid) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_AUTOCOMPLETE_VALID_PREFIX, Tags.of(VALID_TAG, String.valueOf(valid))).increment();
    }

    public enum CacheTag {
        CACHE_HIT,
        CACHE_MISS,
        CACHE_SKIP
    }

}
