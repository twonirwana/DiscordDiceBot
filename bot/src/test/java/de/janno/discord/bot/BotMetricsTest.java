package de.janno.discord.bot;

import io.micrometer.core.instrument.util.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

class BotMetricsTest {

    @Test
    void init() throws IOException {
        BotMetrics.init("localhost");
        String res = IOUtils.toString(new URL("http://localhost:8080/prometheus").openStream());

        assertThat(res).contains("process_uptime_seconds");
    }
}