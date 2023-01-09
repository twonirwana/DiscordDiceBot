package de.janno.discord.bot.dice.image;

import com.google.common.io.Resources;
import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class FateBlack implements ImageProvider {

    private final BufferedImage BLANK;
    private final BufferedImage PLUS;
    private final BufferedImage MINUS;

    public FateBlack() {
        try {
            BLANK = ImageIO.read(Resources.getResource("images/fate_black/fate-blank-die.png").openStream());
            PLUS = ImageIO.read(Resources.getResource("images/fate_black/fate-plus-die.png").openStream());
            MINUS = ImageIO.read(Resources.getResource("images/fate_black/fate-minus-die.png").openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getDieHighAndWith() {
        return 50;
    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide) {
        switch (shownDieSide) {
            case -1 -> {
                return List.of(MINUS);
            }
            case 0 -> {
                return List.of(BLANK);
            }
            case 1 -> {
                return List.of(PLUS);
            }
        }
        return List.of();
    }
}
