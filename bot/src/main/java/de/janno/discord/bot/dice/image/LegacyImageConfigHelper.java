package de.janno.discord.bot.dice.image;

import com.google.common.collect.ImmutableMap;
import de.janno.discord.bot.ResultImage;

import java.util.Map;

public final class LegacyImageConfigHelper {

    private final static Map<ResultImage, DiceImageStyle> LEGACY_IMAGE_PROVIDER_MAP = ImmutableMap.<ResultImage, DiceImageStyle>builder()
            .put(ResultImage.none, DiceImageStyle.none)
            .put(ResultImage.polyhedral_3d_red_and_white, DiceImageStyle.polyhedral_3d)
            .put(ResultImage.d6_dots_white, DiceImageStyle.d6_dots)
            .put(ResultImage.fate_black, DiceImageStyle.fate)
            .put(ResultImage.polyhedral_black_and_gold, DiceImageStyle.polyhedral_alies_v1)
            .put(ResultImage.polyhedral_alies_blue_and_silver, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_green_and_gold, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_red_and_gold, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_black_and_gold_v2, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_blue_and_gold, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_orange_and_silver, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_purple_and_silver, DiceImageStyle.polyhedral_alies_v2)
            .put(ResultImage.polyhedral_draw_color, DiceImageStyle.polyhedral_2d)
            .build();

    private final static Map<ResultImage, String> LEGACY_DEFAULT_COLOR = ImmutableMap.<ResultImage, String>builder()
            .put(ResultImage.polyhedral_black_and_gold, "black_and_gold")
            .put(ResultImage.polyhedral_alies_blue_and_silver, "blue_and_silver")
            .put(ResultImage.polyhedral_green_and_gold, "green_and_gold")
            .put(ResultImage.polyhedral_red_and_gold, "red_and_gold")
            .put(ResultImage.polyhedral_black_and_gold_v2, "black_and_gold")
            .put(ResultImage.polyhedral_blue_and_gold, "blue_and_gold")
            .put(ResultImage.polyhedral_orange_and_silver, "orange_and_silver")
            .put(ResultImage.polyhedral_purple_and_silver, "purple_and_silver")
            .build();


    public static DiceStyleAndColor getStyleAndColor(ResultImage resultImage) {
        DiceImageStyle diceImageStyle = LEGACY_IMAGE_PROVIDER_MAP.get(resultImage);
        return new DiceStyleAndColor(diceImageStyle, LEGACY_DEFAULT_COLOR.getOrDefault(resultImage, diceImageStyle.getDefaultColor()));
    }

}
