package de.janno.discord;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.*;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;

import static io.micrometer.core.instrument.Metrics.globalRegistry;

public class Metrics {

    public final static String METRIC_PREFIX = "dice.";
    public final static String METRIC_BUTTON_PREFIX = "buttonEvent";
    public final static String METRIC_SLASH_PREFIX = "slashEvent";
    public final static String CONFIG_TAG = "config";
    public final static String COMMAND_TAG = "command";

    public static void init(boolean collectSystemMetrics) {
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        io.micrometer.core.instrument.Metrics.addRegistry(prometheusRegistry);
        new UptimeMetrics().bindTo(globalRegistry);
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/prometheus", httpExchange -> {
                String response = prometheusRegistry.scrape();
                httpExchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = httpExchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            new Thread(server::start).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (collectSystemMetrics) {
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

    public static void incrementButtonMetricCounter(@NonNull String commandName, @NonNull List<String> config) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_BUTTON_PREFIX, Tags.of(COMMAND_TAG, commandName).and(CONFIG_TAG, config.toString())).increment();

    }

    public static void incrementSlashMetricCounter(@NonNull String commandName, @NonNull List<String> config) {
        globalRegistry.counter(METRIC_PREFIX + METRIC_SLASH_PREFIX, Tags.of(COMMAND_TAG, commandName).and(CONFIG_TAG, config.toString())).increment();
    }
}
