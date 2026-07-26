package de.janno.discord.bot.dice.image;

import de.janno.discord.bot.dice.image.provider.ImageProvider;
import de.janno.discord.bot.dice.image.provider.RnRD6;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CreateStyleOverviewImageUtil {

    static void main() throws IOException {

        ImageProvider imageProvider = new RnRD6();

        for (String color : imageProvider.getSupportedColors()) {
            if (!color.equals("none")) {
                Map<Integer, List<Integer>> showDieFace = Map.of(6, List.of(1, 2, 3, 4, 5, 6));
                int singleDiceSize = 100;
                List<BufferedImage> images = showDieFace.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .flatMap(d -> d.getValue().stream().flatMap(s -> imageProvider.getImageFor(d.getKey(), s, color).stream()))
                        .toList();

                int w = singleDiceSize * (images.size());
                BufferedImage combined = new BufferedImage(w, singleDiceSize, BufferedImage.TYPE_INT_ARGB_PRE);

                Graphics g = combined.getGraphics();
                for (int j = 0; j < images.size(); j++) {
                    g.drawImage(images.get(j), singleDiceSize * j, 0, singleDiceSize, singleDiceSize, null);
                }
                g.dispose();

                ImageIO.write(combined, "PNG", new File(imageProvider.getClass().getSimpleName() + "_" + color + ".png"));
            }
        }
    }
}
