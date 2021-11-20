package de.janno.discord;

import com.google.common.base.Strings;
import reactor.netty.http.client.HttpClient;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean updateCommands = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        Metrics.init(publishMetricsToUrl);
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .metrics(!Strings.isNullOrEmpty(publishMetricsToUrl), s -> "")
                .followRedirect(true)
                .secure();
        new DiceSystem(httpClient, token, updateCommands);
    }

}
