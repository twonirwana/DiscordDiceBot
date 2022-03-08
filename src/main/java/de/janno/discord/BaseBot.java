package de.janno.discord;

import de.janno.discord.discord4j.JavaCordClient;

/**
 * - Config file
 * -- Metrics with port
 */
public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean disableCommandUpdate = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        Metrics.init(publishMetricsToUrl);
/*        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .metrics(!Strings.isNullOrEmpty(publishMetricsToUrl), s -> "")
                .followRedirect(true)
                .secure();*/
        new JavaCordClient(token, disableCommandUpdate);
    }

}
