package de.janno.discord;

import reactor.netty.http.client.HttpClient;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean updateCommands = Boolean.parseBoolean(args[1]);
        final boolean systemMetric = Boolean.parseBoolean(args[2]);
        final boolean keepAlive = Boolean.parseBoolean(args[3]);
        Metrics.init(systemMetric);
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .metrics(systemMetric, s -> "")
                .keepAlive(keepAlive) //solves some problems with connection resets on some internet connections
                .followRedirect(true)
                .secure();
        new DiceSystem(httpClient, token, updateCommands);
    }

}
