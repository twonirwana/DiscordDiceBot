package de.janno.discord;

public class BaseBot {
    public static void main(final String[] args) {
        final String token = args[0];
        new DiceSystem(token);
    }

}
