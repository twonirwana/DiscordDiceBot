package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class Expanse extends AbstractD6 {

    public static final String BELT_DARK = "belt_dark";
    public static final String BELT_LIGHT = "belt_light";
    public static final String EARTH_DARK = "earth_dark";
    public static final String EARTH_LIGHT = "earth_light";
    public static final String MARS_DARK = "mars_dark";
    public static final String MARS_LIGHT = "mars_light";
    public static final String PROTOTGEN_DARK = "protogen_dark";
    public static final String PROTOTGEN_LIGHT = "protogen_light";

    public Expanse() {
        super("expanse_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return BELT_DARK;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(BELT_DARK, BELT_LIGHT, EARTH_DARK, EARTH_LIGHT, MARS_DARK, MARS_LIGHT, PROTOTGEN_DARK, PROTOTGEN_LIGHT);
    }
}
