package de.janno.discord.bot;

import io.micrometer.core.instrument.util.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

class BotMetricsTest {

    @Test
    void init() throws IOException, URISyntaxException {
        BotMetrics.init();
        String res = IOUtils.toString(new URI("http://localhost:9090/prometheus").toURL().openStream());

        assertThat(res).contains("process_uptime_seconds");

        String res2 = IOUtils.toString(new URI("http://localhost:9090/prometheus").toURL().openStream());

        assertThat(res).isNotEqualTo(res2);
    }
}