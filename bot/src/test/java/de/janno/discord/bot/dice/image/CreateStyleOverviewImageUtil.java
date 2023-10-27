package de.janno.discord.bot.dice.image;

import com.google.common.io.Resources;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CreateStyleOverviewImageUtil {

    public static void main(String[] args) throws IOException {
        String style = "d6_black_and_gold";
        Map<Integer, List<Integer>> showDieFace = Map.of(6, List.of(1, 2, 3, 4, 5, 6));
        int singleDiceSize = 100;
        List<BufferedImage> images = showDieFace.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .flatMap(d -> d.getValue().stream().map(s -> {
                    try {
                        return ImageIO.read(Resources.getResource("images/%s/d%d/d%ds%d.png".formatted(style, d.getKey(), d.getKey(), s)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .toList();


        int w = singleDiceSize * (images.size());
        BufferedImage combined = new BufferedImage(w, singleDiceSize, BufferedImage.TYPE_INT_ARGB_PRE);

        Graphics g = combined.getGraphics();
        for (int j = 0; j < images.size(); j++) {
            g.drawImage(images.get(j), singleDiceSize * j, 0, singleDiceSize, singleDiceSize, null);
        }
        g.dispose();

        ImageIO.write(combined, "PNG", new File(style + ".png"));

    }
}
