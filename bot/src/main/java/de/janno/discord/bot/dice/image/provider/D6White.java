package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import lombok.NonNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class D6White implements ImageProvider {

    private final Map<Integer, BufferedImage> imageMap;


    public D6White() {
        try {
            imageMap = ImmutableMap.of(
                    1, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-one.png").openStream()),
                    2, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-two.png").openStream()),
                    3, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-three.png").openStream()),
                    4, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-four.png").openStream()),
                    5, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-five.png").openStream()),
                    6, ImageIO.read(Resources.getResource("images/d6_white/dice-six-faces-six.png").openStream()));
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
        if (totalDieSides == null || shownDieSide == null) {
            return List.of();
        }
        if (totalDieSides == 6 && imageMap.containsKey(shownDieSide)) {
            return List.of(imageMap.get(shownDieSide));
        }
        return List.of();
    }
}
