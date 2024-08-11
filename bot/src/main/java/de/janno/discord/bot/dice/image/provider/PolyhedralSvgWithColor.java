package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.fellbaum.jemoji.Emoji;
import net.fellbaum.jemoji.EmojiManager;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import javax.annotation.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class PolyhedralSvgWithColor implements ImageProvider {

    private static final Map<Integer, String> DICE_IMAGE_MAP = Stream.of(2, 4, 6, 8, 10, 12, 20, 100)
            .collect(ImmutableMap.toImmutableMap(d -> d, d -> {
                final String fileName;
                if (d == 100) {
                    fileName = "images/polyhedral_draw_color/D10.svg";
                } else {
                    fileName = "images/polyhedral_draw_color/D%d.svg".formatted(d);
                }
                try {
                    return IOUtils.toString(Resources.getResource(fileName).openStream(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }));
    public static final String RED = "red";
    public static final String CYAN = "cyan";

    private static final Map<String, Color> COLOR_MAP = ImmutableMap.<String, Color>builder()
            .put("white", Color.white)
            //.put("light_gray", Color.lightGray)
            .put("gray", Color.gray)
            //.put("dark_gray", Color.darkGray)
            .put("black", Color.black)
            .put(RED, Color.red)
            .put("pink", Color.pink)
            .put("orange", Color.orange)
            .put("yellow", Color.yellow)
            .put("green", Color.green)
            .put("magenta", Color.magenta)
            .put(CYAN, Color.cyan)
            .put("blue", Color.blue)
            .put("indigo", new Color(75, 0, 130))
            .build();
    private static final float IMAGE_SIZE = 100;

    public PolyhedralSvgWithColor() {
        try {
            boolean loadedNotoEmoji = GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, Resources.getResource("fonts/NotoEmoji.ttf").openStream()));
            log.info("Loaded NotoEmoji.ttf: " + loadedNotoEmoji);
            boolean loadedNotoSansMono = GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(Font.createFont(Font.TRUETYPE_FONT, Resources.getResource("fonts/NotoSansMono.ttf").openStream()));
            log.info("Loaded NotoSansMono.ttf: " + loadedNotoSansMono);
        } catch (FontFormatException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean isExactlyOneEmoji(String in) {
        List<Emoji> emojis = EmojiManager.extractEmojisInOrder(in);
        if (emojis.size() != 1) {
            return false;
        }
        return EmojiManager.removeAllEmojis(in).isEmpty();
    }

    private static BufferedImage getImageAndSetNumberAndColor(Integer totalDieSides, String diceValue, String colorString) {
        String svgString = DICE_IMAGE_MAP.get(totalDieSides);
        Color color = Optional.ofNullable(colorString).map(String::toLowerCase).map(COLOR_MAP::get).orElse(Color.red);
        String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);
        svgString = svgString.replace("fill:#ff0000", "fill:" + hex);
        if (isExactlyOneEmoji(diceValue)) {
            //apache batik 1.17 has bug with emojis that have more than one character. It will display the first one correct
            //but afterward use the same emoji for all other emoji that have the same first character. We draw it late with java
            svgString = svgString.replace("DICE_VALUE", "");
        } else {
            svgString = svgString.replace("DICE_VALUE", diceValue);
        }

        final BufferedImage image;

        try (Reader reader = CharSource.wrap(svgString).openStream()) {
            final TranscoderInput transcoderInput = new TranscoderInput(reader);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                final TranscoderOutput output = new TranscoderOutput(outputStream);
                final PNGTranscoder pngTranscoder = new PNGTranscoder();
                pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, IMAGE_SIZE);
                pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, IMAGE_SIZE);
                pngTranscoder.transcode(transcoderInput, output);
                image = ImageIO.read(new ByteArrayInputStream(outputStream.toByteArray()));
            }
        } catch (TranscoderException | IOException e) {
            throw new RuntimeException(e);
        }

        if (isExactlyOneEmoji(diceValue)) {
            Graphics g = image.getGraphics();
            g.setColor(Color.WHITE);
            g.setFont(new Font("Noto Emoji", Font.BOLD, 70));
            g.drawString(diceValue, 5, 75);
            g.setColor(Color.BLACK);
            g.setFont(new Font("Noto Emoji", Font.PLAIN, 70));
            g.drawString(diceValue, 5, 75);
            g.dispose();
        }

        return image;


    }

    @Override
    public @NonNull List<BufferedImage> getImageFor(int totalDieSides, int shownDieSide, String color) {
        if (!DICE_IMAGE_MAP.containsKey(totalDieSides)) {
            return List.of();
        }
        final String validatedColor;
        if (color == null || !getSupportedColors().contains(color)) {
            validatedColor = getDefaultColor();
        } else {
            validatedColor = color;
        }

        if (totalDieSides == 100) {
            int tens = (shownDieSide / 10) * 10;

            int ones = shownDieSide - (tens);
            final String tensString;
            if (tens == 100 || tens == 0) {
                tensString = "00";
            } else {
                tensString = String.valueOf(tens);
            }
            return List.of(
                    getImageAndSetNumberAndColor(100, tensString, validatedColor),
                    getImageAndSetNumberAndColor(10, String.valueOf(ones), validatedColor)
            );
        }

        return List.of(getImageAndSetNumberAndColor(totalDieSides, String.valueOf(shownDieSide), validatedColor));
    }

    @Override
    public @NonNull List<BufferedImage> getImageForString(int totalDieSides, @NonNull String diceValue, @Nullable String color) {
        if (!DICE_IMAGE_MAP.containsKey(totalDieSides) || totalDieSides == 100) {
            return List.of();
        }
        diceValue = diceValue.trim();

        if (isExactlyOneEmoji(diceValue) ||
                //one or two non emoji characters
                (!EmojiManager.containsEmoji(diceValue)
                        && diceValue.length() <= 2
                        && !diceValue.isEmpty())) {
            final String validatedColor;
            if (color == null || !getSupportedColors().contains(color)) {
                validatedColor = getDefaultColor();
            } else {
                validatedColor = color;
            }

            return List.of(getImageAndSetNumberAndColor(totalDieSides, diceValue, validatedColor));
        }

        return List.of();
    }

    @Override
    public @NonNull String getDefaultColor() {
        return RED;
    }

    @Override
    public @NonNull List<String> getSupportedColors() {
        return ImmutableList.copyOf(COLOR_MAP.keySet());
    }
}
