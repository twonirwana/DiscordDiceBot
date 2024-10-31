package de.janno.discord.connector.jda;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpConnectionPoolMetrics;
import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import okhttp3.EventListener;
import okhttp3.OkHttpClient;

import java.util.Locale;
import java.util.Set;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

@Slf4j
public class JdaMetrics {

    private static final String METRIC_PREFIX = "dice.";
    private static final String METRIC_WELCOME_COUNTER_PREFIX = "welcomeCounter";
    private static final String METRIC_USER_INSTALL_PREFIX = "userInstall";
    private static final String USER_LOCALE = "userLocale";
    private static final String SHARD_ID = "shardId";

    public static void registerHttpClient(OkHttpClient client) {
        new OkHttpConnectionPoolMetrics(client.connectionPool()).bindTo(globalRegistry);
    }

    public static void startGuildCountGauge(Set<Long> botInGuildIdSet) {
        Gauge.builder(METRIC_PREFIX + "guildsCount", botInGuildIdSet::size).register(Metrics.globalRegistry);
    }

    public static void startGatewayResponseTimeGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "gatewayResponseTime", discordApi::getGatewayPing)
                .tag(SHARD_ID, discordApi.getShardInfo().getShardString())
                .register(Metrics.globalRegistry);
    }

    public static void startRestLatencyGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "restLatency", () -> discordApi.getRestPing().onErrorMap(t -> {
                    log.warn("Error while getting rest ping: " + t.getMessage());
                    return -1L;
                }).complete())
                .tag(SHARD_ID, discordApi.getShardInfo().getShardString())
                .register(Metrics.globalRegistry);
    }

    public static void startShardCountGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "totalShardCount", () -> discordApi.getShardInfo().getShardTotal())
                .register(Metrics.globalRegistry);
    }

    public static void startUserCacheGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "userCacheSize", () -> discordApi.getUserCache().size())
                .tag(SHARD_ID, discordApi.getShardInfo().getShardString())
                .register(Metrics.globalRegistry);
    }


    public static void startTextChannelCacheGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "userTextChannelSize", () -> discordApi.getTextChannelCache().size())
                .tag(SHARD_ID, discordApi.getShardInfo().getShardString())
                .register(Metrics.globalRegistry);
    }

    public static void startGuildCacheGauge(JDA discordApi) {
        Gauge.builder(METRIC_PREFIX + "guildCacheSize", () -> discordApi.getGuildCache().size())
                .tag(SHARD_ID, discordApi.getShardInfo().getShardString())
                .register(Metrics.globalRegistry);
    }

    public static void sendWelcomeMessage() {
        globalRegistry.counter(METRIC_PREFIX + METRIC_WELCOME_COUNTER_PREFIX).increment();
    }

    public static void userLocalInteraction(Locale locale) {
        globalRegistry.counter(METRIC_PREFIX + USER_LOCALE, Tags.of("locale", locale.toString(), "language", locale.getLanguage(), "country", locale.getCountry(), "displayCountry", locale.getDisplayCountry(), "cCountry", lastTwoCharacters(locale.toString()))).increment();
    }

    public static EventListener getOkHttpEventListener() {
        return OkHttpMetricsEventListener.builder(globalRegistry, "okHttpEvents").build();
    }

    private static String lastTwoCharacters(String in) {
        if (in == null) {
            return null;
        }
        if (in.length() <= 2) {
            return in.toUpperCase();
        }
        return in.substring(in.length() - 2).toUpperCase();
    }

    public static void userInstallSlashCommand() {
        globalRegistry.counter(METRIC_PREFIX + METRIC_USER_INSTALL_PREFIX).increment();
    }
}
