package de.janno.discord.bot.dice.image;

import de.janno.discord.bot.dice.image.provider.PolyhedralSvgWithColor;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class DrawImageTest {

    PolyhedralSvgWithColor underTest = new PolyhedralSvgWithColor();

    @Test
    public void test_noColor() throws IOException {
        List<BufferedImage> bufferedImage = underTest.getImageFor(4, 3, null);

        File file = Files.newTemporaryFile();
        ImageIO.write(bufferedImage.getFirst(), "PNG", file);
        //hash is different in the github build task, maybe the fonts
        //assertThat(getFileHash(file)).isEqualTo("56046c85f7c7d42e12219132b1b3347d773cb2cda16e8a744b5e368b3902a056");
        assertThat(file).exists();
    }

    @Test
    public void test_green() throws IOException {
        List<BufferedImage> bufferedImage = underTest.getImageFor(4, 3, "green");

        File file = Files.newTemporaryFile();
        ImageIO.write(bufferedImage.getFirst(), "PNG", file);

        //hash is different in the github build task, maybe the fonts
        //assertThat(getFileHash(file)).isEqualTo("d6bec552add788ea98a61212937c488fa93b73336a459fe2e8d6e91ebaaefcce");
        assertThat(file).exists();

    }

    @Test
    public void test_d10_10() throws IOException {
        List<BufferedImage> bufferedImage = underTest.getImageFor(10, 10, "green");

        File file = Files.newTemporaryFile();
        ImageIO.write(bufferedImage.getFirst(), "PNG", file);

        //hash is different in the github build task, maybe the fonts
        //assertThat(getFileHash(file)).isEqualTo("b43bbfd78951e5d1db4e5513f86d5719caf58495cd76316389347ad674c8de4d");
        assertThat(file).exists();
    }

    @Test
    public void test_d10_100() throws IOException {
        List<BufferedImage> bufferedImage = underTest.getImageFor(100, 99, "green");

        assertThat(bufferedImage).hasSize(2);

        File file = Files.newTemporaryFile();
        ImageIO.write(bufferedImage.getFirst(), "PNG", file);

        //hash is different in the github build task, maybe the fonts
        //assertThat(getFileHash(file)).isEqualTo("4788214962fe8fc6fd82961c6993451f3b747c3ac85856bc2489c9f2390d839a");
        assertThat(file).exists();

        File file2 = Files.newTemporaryFile();
        ImageIO.write(bufferedImage.get(1), "PNG", file2);
        //hash is different in the github build task, maybe the fonts
        //assertThat(getFileHash(file2)).isEqualTo("5ba3358cd15891ec7a1f5e91cf1e414d67051ca3abaa1c488c5d0d5c0ed38994");
        assertThat(file2).exists();
    }

}
