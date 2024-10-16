package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class D6Dotted extends AbstractD6 {

    public static final String WHITE = "white";
    public static final String BLACK_AND_GOLD = "black_and_gold";

    public D6Dotted() {
        super("dotted_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return WHITE;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(WHITE, BLACK_AND_GOLD);
    }
}
