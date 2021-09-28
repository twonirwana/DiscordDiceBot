package de.janno.discord;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.jvm.CachedThreadStatesGaugeSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.JvmAttributeGaugeSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean updateCommands = Boolean.parseBoolean(args[1]);
        MetricRegistry metricRegistry = new MetricRegistry();
        SharedMetricRegistries.setDefault("default", metricRegistry);
        metricRegistry.register("gc", new GarbageCollectorMetricSet());
        metricRegistry.register("threads", new CachedThreadStatesGaugeSet(10, TimeUnit.SECONDS));
        metricRegistry.register("memory", new MemoryUsageGaugeSet());
        metricRegistry.register("jvm", new JvmAttributeGaugeSet());
        final Slf4jReporter reporter = Slf4jReporter.forRegistry(metricRegistry)
                .outputTo(LoggerFactory.getLogger("de.janno.discord"))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.start(6, TimeUnit.HOURS);
        new DiceSystem(token, updateCommands);
    }

}
