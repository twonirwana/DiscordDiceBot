package de.janno.discord.bot.dice.image.provider;

import com.google.common.io.Resources;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class FateBasic implements ImageProvider {

    private static final String BLACK = "black";
    private final BufferedImage BLANK;
    private final BufferedImage PLUS;
    private final BufferedImage MINUS;

    public FateBasic() {
        try {
            BLANK = ImageIO.read(Resources.getResource("images/fate_black/fate-blank-die.png").openStream());
            PLUS = ImageIO.read(Resources.getResource("images/fate_black/fate-plus-die.png").openStream());
            MINUS = ImageIO.read(Resources.getResource("images/fate_black/fate-minus-die.png").openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @NonNull String getDefaultColor() {
        return BLACK;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return List.of(BLACK);
    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(int totalDieSides, int shownDieSide, String color) {
        return getImageForString(totalDieSides, String.valueOf(shownDieSide), color);
    }

    @Override
    public @NonNull List<BufferedImage> getImageForString(int totalDieSides, @NonNull String diceValue, @Nullable String color) {
        switch (diceValue) {
            case "-1" -> {
                return List.of(MINUS);
            }
            case "0" -> {
                return List.of(BLANK);
            }
            case "1" -> {
                return List.of(PLUS);
            }
        }
        return List.of();
    }
}
