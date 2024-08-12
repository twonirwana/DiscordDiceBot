package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class D6MarvelV2 extends AbstractD6 {

    public static final String WHITE = "white";
    public static final String RED = "red";


    public D6MarvelV2() {
        super("marvel_v2_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return WHITE;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(WHITE, RED);
    }


}
