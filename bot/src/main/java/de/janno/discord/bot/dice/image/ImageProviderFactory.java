package de.janno.discord.bot.dice.image;

import com.google.common.collect.ImmutableMap;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.dice.image.provider.*;
import lombok.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

public final class ImageProviderFactory {

    private static final ImageProvider NONE_PROVIDER = new NoneProvider();

    private final static Map<ResultImage, ImageProvider> IMAGE_PROVIDER_MAP = ImmutableMap.<ResultImage, ImageProvider>builder()
            .put(ResultImage.none, NONE_PROVIDER)
            .put(ResultImage.polyhedral_3d_red_and_white, new Polyhedral3dRedAndWhite())
            .put(ResultImage.d6_dots_white, new D6White())
            .put(ResultImage.fate_black, new FateBlack())
            .put(ResultImage.polyhedral_black_and_gold, new PolyhedralBlackAndGold())
            .put(ResultImage.polyhedral_alies_blue_and_silver, new PolyhedralAlies())
            .put(ResultImage.polyhedral_green_and_gold, new PolyhedralGreenAndGold())
            .put(ResultImage.polyhedral_red_and_gold, new PolyhedralRedAndGold())
            .put(ResultImage.polyhedral_draw_color, new PolyhedralSvgWithColor())
            .build();

    @NonNull
    public static List<BufferedImage> getImageFor(@NonNull ResultImage resultImage,
                                                  @Nullable Integer totalDieSides,
                                                  @Nullable Integer shownDieSide,
                                                  @Nullable String color) {
        return IMAGE_PROVIDER_MAP.getOrDefault(resultImage, NONE_PROVIDER).getImageFor(totalDieSides, shownDieSide, color);
    }

    public static int getDieHighAndWith(@NonNull ResultImage resultImage) {
        return IMAGE_PROVIDER_MAP.getOrDefault(resultImage, NONE_PROVIDER).getDieHighAndWith();
    }
}
