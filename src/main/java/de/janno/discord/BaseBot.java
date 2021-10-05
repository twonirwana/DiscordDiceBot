package de.janno.discord;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        final boolean updateCommands = Boolean.parseBoolean(args[1]);

        Metrics.init();

        new DiceSystem(token, updateCommands);
    }

}
