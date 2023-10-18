package de.janno.discord.bot.dice.image.provider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import lombok.NonNull;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class PolyhedralSvgWithColor implements ImageProvider {

    private static final Map<Integer, String> DICE_IMAGE_MAP = Stream.of(4, 6, 8, 10, 12, 20, 100)
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
    private static final String RED = "red";

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
            .put("cyan", Color.cyan)
            .put("blue", Color.blue)
            .put("indigo", new Color(	75, 0, 130))
            .build();


    @Override
    public @NonNull List<BufferedImage> getImageFor(Integer totalDieSides, Integer shownDieSide, String colorString) {
        if (totalDieSides == null || shownDieSide == null || !DICE_IMAGE_MAP.containsKey(totalDieSides)) {
            return List.of();
        }
        final String validatedColor;
        if (colorString == null || !getSupportedColors().contains(colorString)) {
            validatedColor = getDefaultColor();
        } else {
            validatedColor = colorString;
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

    private BufferedImage getImageAndSetNumberAndColor(Integer totalDieSides, String shownDieSide, String colorString) {
        String svgString = DICE_IMAGE_MAP.get(totalDieSides);
        Color color = Optional.ofNullable(colorString).map(String::toLowerCase).map(COLOR_MAP::get).orElse(Color.red);
        String hex = "#" + Integer.toHexString(color.getRGB()).substring(2);
        svgString = svgString.replace("fill:#ff0000", "fill:" + hex);
        svgString = svgString.replace("NUMBER", shownDieSide);

        if (shownDieSide.length() > 1) {
            svgString = svgString.replace("x=\"300\"", "x=\"60\"");
        }

        TranscoderInput transcoderInput = new TranscoderInput(IOUtils.toInputStream(svgString, StandardCharsets.UTF_8));

        PNGTranscoder pngTranscoder = new PNGTranscoder();
        pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, (float) getDieHighAndWide());
        pngTranscoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, (float) getDieHighAndWide());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);


        try {
            pngTranscoder.transcode(transcoderInput, output);
            outputStream.flush();
            outputStream.close();

            byte[] imgData = outputStream.toByteArray();
            return ImageIO.read(new ByteArrayInputStream(imgData));
        } catch (TranscoderException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getDieHighAndWide() {
        return 100;
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
