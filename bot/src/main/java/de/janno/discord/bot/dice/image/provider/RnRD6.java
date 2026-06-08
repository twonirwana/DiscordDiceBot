package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class RnRD6 extends AbstractD6 {

    public static final String RED = "red";


    public RnRD6() {
        super("RnR_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return RED;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of( RED);
    }


}
