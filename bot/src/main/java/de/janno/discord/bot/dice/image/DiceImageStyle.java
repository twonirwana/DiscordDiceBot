package de.janno.discord.bot.dice.image;

import de.janno.discord.bot.dice.image.provider.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum DiceImageStyle {
    none(new NoneProvider()),
    polyhedral_3d(new PolyhedralFileImageProvider("polyhedral_3d", List.of("red_and_white"), "red_and_white")),
    polyhedral_alies_v2(new PolyhedralFileImageProvider(
            "polyhedral_alies_v2",
            List.of("blue_and_silver", "black_and_gold", "blue_and_gold", "green_and_gold", "orange_and_silver", "red_and_gold", "purple_and_silver"),
            "blue_and_silver")),
    polyhedral_knots(new PolyhedralFileImageProvider("polyhedral_knots", List.of("purple_dark", "purple_white", "blue"), "blue")),
    polyhedral_RdD(new PolyhedralFileImageProvider("polyhedral_RdD", List.of("default", "special"), "default", Map.of(
            "default", List.of(4, 6, 7, 8, 10, 12, 20, 100),
            "special", List.of(8, 12)))),
    fate(new FateBasic()),
    d6_dots(new D6Dotted()),
    polyhedral_2d(new PolyhedralSvgWithColor()),
    polyhedral_alies_v1(new PolyhedralFileImageProvider("polyhedral_alies_v1", List.of("black_and_gold"), "black_and_gold"));

    private final ImageProvider imageProvider;

    DiceImageStyle(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    public static boolean isValidStyle(String name) {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.toSet()).contains(name);
    }

    public static String combineStyleAndColorName(DiceImageStyle style, String color) {
        return "%s_%s".formatted(style.name(), color);
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public String getDefaultColor() {
        return imageProvider.getDefaultColor();
    }

    public List<String> getSupportedColors() {
        return imageProvider.getSupportedColors();
    }
}
