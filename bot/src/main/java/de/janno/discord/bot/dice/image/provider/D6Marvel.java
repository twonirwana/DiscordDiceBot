package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class D6Marvel extends AbstractD6 {

    public static final String BLUE = "blue";
    public static final String RED = "red";


    public D6Marvel() {
        super("marvel_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return BLUE;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(BLUE, RED);
    }


}
