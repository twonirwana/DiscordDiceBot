package de.janno.discord;

import reactor.netty.http.client.HttpClient;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean updateCommands = Boolean.parseBoolean(args[1]);
        final boolean collectSystemMetricAndPublish = Boolean.parseBoolean(args[2]);
        Metrics.init(collectSystemMetricAndPublish);
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .metrics(collectSystemMetricAndPublish, s -> "")
                .followRedirect(true)
                .secure();
        new DiceSystem(httpClient, token, updateCommands);
    }

}
