package de.janno.discord.bot.dice.image;

import de.janno.discord.bot.dice.image.provider.*;

import java.util.List;

public enum DiceImageStyle {
    none(new NoneProvider()),
    polyhedral_3d(new PolyhedralFileImageProvider("polyhedral_3d", List.of("red_and_white"), "red_and_white")),
    polyhedral_alies_v2(new PolyhedralFileImageProvider(
            "polyhedral_alies_v2",
            List.of("blue_and_silver", "black_and_gold", "blue_and_gold", "green_and_gold", "orange_and_silver", "red_and_gold", "purple_and_silver"),
            "blue_and_silver")),
    fate(new FateBasic()),
    d6_dots(new D6Dotted()),
    polyhedral_2d(new PolyhedralSvgWithColor()),
    polyhedral_alies_v1(new PolyhedralFileImageProvider("polyhedral_alies_v1", List.of("black_and_gold"), "black_and_gold"));

    private final ImageProvider imageProvider;

    DiceImageStyle(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public String getDefaultColor() {
        return imageProvider.getDefaultColor();
    }
}
