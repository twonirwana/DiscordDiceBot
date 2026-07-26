package de.janno.discord.bot.dice.image.provider;

import lombok.NonNull;

import java.util.List;

public class HeroQuestD6 extends AbstractD6 {

    public static final String WHITE = "white";


    public HeroQuestD6() {
        super("heroQuest_");
    }

    @Override
    public @NonNull String getDefaultColor() {
        return WHITE;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(WHITE);
    }

}
