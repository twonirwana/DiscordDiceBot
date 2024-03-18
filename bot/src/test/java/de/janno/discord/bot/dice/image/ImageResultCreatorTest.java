package de.janno.discord.bot.dice.image;

import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import de.janno.discord.bot.ResultImage;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.random.GivenNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResultCreatorTest {
    private final ImageResultCreator underTest = new ImageResultCreator();

    private static String getDataHash(@Nullable Supplier<? extends InputStream> data) throws IOException {
        if (data == null) {
            return null;
        }
        //FileUtils.copyInputStreamToFile(data.get(), new File("test2.png"));
        return Hashing.sha256()
                .hashBytes(ByteStreams.toByteArray(data.get()))
                .toString();
    }

    static Stream<Arguments> generateResultImageData() {
        return Stream.of(
                Arguments.of(ResultImage.polyhedral_3d_red_and_white, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_alies_blue_and_silver, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_green_and_gold, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_red_and_gold, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_black_and_gold, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_draw_color, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_purple_and_silver, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_orange_and_silver, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_blue_and_gold, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.polyhedral_black_and_gold_v2, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(ResultImage.fate_black, List.of(-1, 0, 1)),
                Arguments.of(ResultImage.d6_dots_white, List.of(6))
        );
    }

    static Stream<Arguments> generateDiceStyleData() {
        return Stream.of(
                Arguments.of(DiceImageStyle.polyhedral_alies_v1, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.polyhedral_alies_v2, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.polyhedral_3d, List.of(4, 6, 8, 10, 12, 20, 100)),
                //creating all color versions of this takes to long (~90s)
                //Arguments.of(DiceImageStyle.polyhedral_2d, List.of(2, 4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.polyhedral_knots, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.fate, List.of(-1, 0, 1)),
                Arguments.of(DiceImageStyle.d6_dots, List.of(6)),
                Arguments.of(DiceImageStyle.expanse, List.of(6)),
                Arguments.of(DiceImageStyle.d6_marvel, List.of(6))
        );
    }

    static Stream<Arguments> generateDiceStyleDataPerColor() {
        return Stream.of(
                Arguments.of(DiceImageStyle.polyhedral_2d, List.of(4, 6, 8, 10, 12, 20, 100), "red"),
                Arguments.of(DiceImageStyle.polyhedral_RdD, List.of(4, 6, 7, 8, 10, 12, 20, 100), "default"),
                Arguments.of(DiceImageStyle.polyhedral_RdD, List.of(8, 12), "special")
        );
    }


    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
    }

    @Test
    void getImageForRoll_ResultImageNone() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000, 10_000, true).evaluate("1d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.none, "none"));

        assertThat(res).isNull();
    }

    @Test
    void createRollCacheNameTest_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isEqualTo("polyhedral_alies_v1_black_and_gold@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void createRollCacheNameTest_multiColor() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 8, 10, 12, 20, 99), 1000, 10_000, true).evaluate("color(1d4,'gray') + color(1d6,'black') + color(1d8,'white') + color(1d10,'red') + color(1d12,'blue') + color(1d20,'green') + color(1d100,'orange')");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "red"));

        assertThat(res).isEqualTo("polyhedral_2d_red@[gray:4∈[1...4]],[black:6∈[1...6]],[white:8∈[1...8]],[red:10∈[1...10]],[blue:12∈[1...12]],[green:20∈[1...20]],[orange:99∈[1...100]]");
    }

    @Test
    void createRollCacheNameTest_multiCol() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 8, 10, 12, 20, 99), 1000, 10_000, true).evaluate("1d4 col 'gray' + 1d6 col 'black' + 1d8 col 'white' + 1d10 col 'red' + 1d12 col 'blue' + 1d20 col 'green' + 1d100 col 'orange'");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "red"));

        assertThat(res).isEqualTo("polyhedral_2d_red@[gray:4∈[1...4]],[black:6∈[1...6]],[white:8∈[1...8]],[red:10∈[1...10]],[blue:12∈[1...12]],[green:20∈[1...20]],[orange:99∈[1...100]]");
    }

    @Test
    void createRollCacheNameTest_fateBlack() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 1), 1000, 10_000, true).evaluate("4d[-1,0,1]");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.fate, "black"));

        assertThat(res).isEqualTo("fate_black@[-1∈[-1, 0, 1],0∈[-1, 0, 1],1∈[-1, 0, 1],-1∈[-1, 0, 1]]");
    }

    @Test
    void createRollCacheNameTest_explode() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000, 10_000, true).evaluate("3d!6");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[6∈[1...6],6∈[1...6],5∈[1...6],5∈[1...6],4∈[1...6]]");
    }

    @Test
    void createRollCacheNameTest_explodeAdd() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000, 10_000, true).evaluate("3d!!6");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[6∈[1...6],6∈[1...6],5∈[1...6],5∈[1...6],4∈[1...6]]");
    }

    @Test
    void getImageForRoll_explode() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000, 10_000, true).evaluate("3d!6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("a78152e41956d655a4aa3428886e0be6c08c02f9dff25db08befb5e7086b3aae");
    }

    @Test
    void getImageForRoll_explodeAdd() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000, 10_000, true).evaluate("3d!!6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("4b433f278f98eeac7c0f838969b40fcdb1397cb82d662aeb22a47f5a326a5363");
    }

    @Test
    void getImageForRoll_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6+1d6+3d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("a2c774609da21cd6982d3fad969a7ec87db430c1cb83cf7dadb61b3374cf5589");
    }

    @Test
    void getImageForRoll_noDie_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000, 10_000, true).evaluate("5");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000, 10_000, true).evaluate("1d7");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("f0cb399ebfb9e65265214e781e827bfaa20a720f3b76bed9fd97c81dcd09aec7");
    }

    @Test
    void getImageForRoll_D100_01_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("3b3f20759ccbc0177a425de9f015d16f5e9a1ffbdcab7c4d4b585dd2e28b134f");
    }

    @Test
    void getImageForRoll_D100_99_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("a3e5a7537f470c9d7ecefb23e16c77f9e51891ed8a39589339a95458f70c7885");
    }

    @Test
    void getImageForRoll_cache() throws ExpressionException, IOException, InterruptedException {
        List<Roll> rolls1 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d6");
        Supplier<? extends InputStream> res1 = underTest.getImageForRoll(rolls1, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(res1).isNotNull();

        File cacheFile = new File("imageCache/polyhedral_3d_red_and_white/cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png");
        assertThat(cacheFile).exists();
        long res1LastModified = cacheFile.lastModified();
        assertThat(getDataHash(res1)).isEqualTo("933002493c0ccf2ea6ad67c8656342d3f02642a19a840b032a62346e4a7a048b");

        Thread.sleep(100);

        List<Roll> rolls2 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d6");
        Supplier<? extends InputStream> cachedRes1 = underTest.getImageForRoll(rolls2, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(cachedRes1).isNotNull();

        File cacheFile2 = new File("imageCache/polyhedral_3d_red_and_white/cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png");
        assertThat(cacheFile2).exists();
        assertThat(cacheFile2.lastModified()).isEqualTo(res1LastModified);
        assertThat(getDataHash(cachedRes1)).isEqualTo("933002493c0ccf2ea6ad67c8656342d3f02642a19a840b032a62346e4a7a048b");

        Thread.sleep(100);

        List<Roll> rolls3 = new DiceEvaluator(new GivenNumberSupplier(2), 1000, 10_000, true).evaluate("1d6");
        Supplier<? extends InputStream> res2 = underTest.getImageForRoll(rolls3, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(res2).isNotNull();

        File cacheFile3 = new File("imageCache/polyhedral_3d_red_and_white/dbb28d1b4a2eb15d9a4b2301e76f751f0129efe722d94b4551049fc168524162.png");
        assertThat(cacheFile3).exists();
        assertThat(cacheFile3.lastModified()).isNotEqualTo(res1LastModified);

        assertThat(getDataHash(res2)).isEqualTo("beb6e26993ee00f8c4fe5e9fd8f4f39cf0450626bbb25a020d18c22717808fe5");
    }

    @Test
    void getImageForRoll_noCacheForLargeDiceSets() throws ExpressionException, IOException {
        List<Roll> rolls1 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("7d10");
        Supplier<? extends InputStream> res1 = underTest.getImageForRoll(rolls1, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(res1).isNotNull();
        File cacheFolder = new File("imageCache/");
        assertThat(cacheFolder).isEmptyDirectory();
        assertThat(getDataHash(res1)).isEqualTo("c6ea9275d2ab8391ff4978a4fd8e3f36fa0ef0ab4dc6fa85074a175e2bd307b0");

        List<Roll> rolls2 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("7d10");
        Supplier<? extends InputStream> res2 = underTest.getImageForRoll(rolls2, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(cacheFolder).isEmptyDirectory();
        assertThat(res2).isNotNull();
        assertThat(getDataHash(res2)).isEqualTo("c6ea9275d2ab8391ff4978a4fd8e3f36fa0ef0ab4dc6fa85074a175e2bd307b0");
    }

    @Test
    void getImageForCustomRoll_cache() throws ExpressionException, IOException, InterruptedException {
        List<Roll> rolls1 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d[-1,0,1]");
        Supplier<? extends InputStream> res1 = underTest.getImageForRoll(rolls1, new DiceStyleAndColor(DiceImageStyle.fate, "black"));
        assertThat(res1).isNotNull();

        File cacheFile = new File("imageCache/fate_black/9f295c8cb283c4dbc3c58ba29e5b93884bb3f0cb369ca66f3f932220db2fa6bf.png");
        assertThat(cacheFile).exists();
        long res1LastModified = cacheFile.lastModified();
        assertThat(getDataHash(res1)).isEqualTo("cb853512212a4e57391479cdae47cee5e014be1eed9511b1298908b12623c0bb");

        Thread.sleep(100);

        List<Roll> rolls2 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d[-1,0,1]");
        Supplier<? extends InputStream> cachedRes1 = underTest.getImageForRoll(rolls2, new DiceStyleAndColor(DiceImageStyle.fate, "black"));
        assertThat(cachedRes1).isNotNull();

        File cacheFile2 = new File("imageCache/fate_black/9f295c8cb283c4dbc3c58ba29e5b93884bb3f0cb369ca66f3f932220db2fa6bf.png");
        assertThat(cacheFile2).exists();
        assertThat(cacheFile2.lastModified()).isEqualTo(res1LastModified);
        assertThat(getDataHash(cachedRes1)).isEqualTo("cb853512212a4e57391479cdae47cee5e014be1eed9511b1298908b12623c0bb");

        Thread.sleep(100);

        List<Roll> rolls3 = new DiceEvaluator(new GivenNumberSupplier(2), 1000, 10_000, true).evaluate("1d[-1,0,1]");
        Supplier<? extends InputStream> res2 = underTest.getImageForRoll(rolls3, new DiceStyleAndColor(DiceImageStyle.fate, "black"));
        assertThat(res2).isNotNull();

        File cacheFile3 = new File("imageCache/fate_black/3b1ffd07870d80beda782bd15bd670798617a179419084d5a5206f91c7ce8048.png");
        assertThat(cacheFile3).exists();
        assertThat(cacheFile3.lastModified()).isNotEqualTo(res1LastModified);

        assertThat(getDataHash(res2)).isEqualTo("6aee5beb6715fbbfd1b808a4215c3d066488aeadd7f57d30b476b0286df3ef58");
    }

    @Test
    void getImageForCustomRoll_noCacheForLargeDiceSets() throws ExpressionException, IOException {
        List<Roll> rolls1 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("7d[-1,0,1]");
        Supplier<? extends InputStream> res1 = underTest.getImageForRoll(rolls1, new DiceStyleAndColor(DiceImageStyle.fate, "black"));
        assertThat(res1).isNotNull();
        File cacheFolder = new File("imageCache/");
        assertThat(cacheFolder).isEmptyDirectory();
        assertThat(getDataHash(res1)).isEqualTo("b3929fb41bad88f279554cb27e729c05ca783f03134ffe531f6bd3c3cb315d5a");

        List<Roll> rolls2 = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("7d[-1,0,1]");
        Supplier<? extends InputStream> res2 = underTest.getImageForRoll(rolls2, new DiceStyleAndColor(DiceImageStyle.fate, "black"));

        assertThat(cacheFolder).isEmptyDirectory();
        assertThat(res2).isNotNull();
        assertThat(getDataHash(res2)).isEqualTo("b3929fb41bad88f279554cb27e729c05ca783f03134ffe531f6bd3c3cb315d5a");
    }

    @Test
    void createRollCacheNameTest_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.getFirst(), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void getImageForRoll_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6+1d6+3d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("5bb342eaacea0ef00fab10901e786a3b5f3b38b287f7c7961b9cf7f117694f6e");
    }

    @Test
    void getImageForRoll_d6DotsWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.d6_dots, "white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("2be6d12e5678f9b2ac87d0b5b49169d8b657881a2d2c849332426e24d8a6be54");
    }

    @Test
    void getImageForRoll_d6DotsBlackAndGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.d6_dots, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("acd0714a5dee4f9c3948933b194aa79786b5e4d46fd7e2d2c4386e7f90238527");
    }


    @Test
    void getImageForRoll_noDie_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000, 10_000, true).evaluate("5");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000, 10_000, true).evaluate("1d7");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("60679ea2b91f3607f99cf9029c5cc6a552d245fab3fcd963ac88de437cf7118a");
    }

    @Test
    void getImageForRoll_D100_01_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("2333543c0c9813f3013b2b781f8614dfba8accea373755301cae4b354d971337");
    }

    @Test
    void getImageForRoll_D100_99_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("fffee64a526f6fdd0e4a1e44cbcfcda0fe458709d3b17c7ff8745556e386ef67");
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1} -> {2}")
    @MethodSource("generateResultImageData")
    void testLegacyPolyhedralResultImage(ResultImage resultImage, List<Integer> sides) throws ExpressionException {
        ImageResultCreator underTestWithoutCache = new ImageResultCreator(-1);
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000, 10_000, true).evaluate("1d%d".formatted(d));
                Supplier<? extends InputStream> res = underTestWithoutCache.getImageForRoll(rolls, LegacyImageConfigHelper.getStyleAndColor(resultImage));
                assertThat(res).isNotNull();
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d[abc/cde]");
        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, LegacyImageConfigHelper.getStyleAndColor(resultImage));
        assertThat(res).isNull();
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1} -> {2}")
    @MethodSource("generateDiceStyleData")
    void testPolyhedralResultImage(DiceImageStyle diceImageStyle, List<Integer> sides) throws ExpressionException {
        ImageResultCreator underTestWithoutCache = new ImageResultCreator(-1);
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000, 10_000, true).evaluate("1d%d".formatted(d));
                for (String color : diceImageStyle.getSupportedColors()) {
                    Supplier<? extends InputStream> res = underTestWithoutCache.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, color));
                    assertThat(res).isNotNull();
                    assertThat(res.get()).isNotEmpty();
                }
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d[abc/cde]");
        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, "notAColor"));
        assertThat(res).isNull();
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1}, color:{2} -> {3}")
    @MethodSource("generateDiceStyleDataPerColor")
    void testPolyhedralResultImagePerColor(DiceImageStyle diceImageStyle, List<Integer> sides, String color) throws ExpressionException {
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000, 10_000, true).evaluate("1d%d".formatted(d));
                Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, color));
                assertThat(res).isNotNull();
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d[abc/cde]");
        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, "notAColor"));
        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_polyhedralDrawColor() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(2, 4, 6, 8, 10, 12, 20, 99), 1000, 10000, true)
                .evaluate("color(1d2,'indigo')  +color(1d4,'gray') + color(1d6,'black') + color(1d8,'white') + color(1d10,'red') + color(1d12,'blue') + color(1d20,'green') + color(1d100,'orange')");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor()));

        assertThat(res).isNotNull();

        //hash is different in the GitHub build task, maybe the fonts
        //assertThat(getDataHash(res)).isEqualTo("5699c6165fcba298ed003346317d42434b8bfb35cc0f80e3f821a0e9963ed967");
    }

    @Test
    void getImageForRoll_polyhedralDrawColorCustom() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(2, 2, 6, 8, 10, 12, 20, 1, 1, 1), 1000, 10000, true)
                .evaluate("color(d[\uD83D/\uD83D\uDC41],'indigo')" +
                                "+ color(1d[-1/-2/-3/+1/+2/+3],'gray') " +
                                "+ color(1d[0/10/20/30/40/50/60/70],'black') " +
                                "+ color(1d[a/b/c/d/e/f/g/+],'white') " +
                                "+ color(1d[aa/bb/cc/dd/ee/ff/gg/hh/ii/up],'red') " +
                                "+ color(1d[A/B/C/D/E/F/G/H/I/J/K/X],'blue') " +
                                "+ color(1d[AA/BB/CC/DD/EE/FF/GG/HH/II/JJ/AA/BB/CC/DD/EE/FF/GG/HH/II/AA],'green') " +
                                "+ color(1d[⚔/-2/-3/+1/+2/+3],'cyan') " +
                                "+ color(1d[\uD83D\uDDE1/-2/-3/+1/+2/+3],'orange') " +
                                "+ color(1d[\uD83D\uDC4D/-2/-3/+1/+2/+3],'yellow') "
                        //       + "+ color(1d2,'indigo')  +color(1d4,'gray') + color(1d6,'black') + color(1d8,'white') + color(1d10,'red') + color(1d12,'blue') + color(1d20,'green') + color(1d100,'orange')"
                );

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor()));

        assertThat(res).isNotNull();

        //hash is different in the GitHub build task, maybe the fonts
        //assertThat(getDataHash(res)).isEqualTo("1f9f17eea89318e58a743310d039551d59b51e5b47ad30cd8dcee0311d2ff5e4");
    }

    @Test
    void getImageForRoll_polyhedralDrawUtf16Emoji() throws ExpressionException, IOException {
        DiceEvaluator evaluator = new DiceEvaluator(new GivenNumberSupplier(1, 2), 1000, 10000, true);
        List<Roll> roll1 = evaluator.evaluate("color(d[\uD83D\uDDE1/\uD83D\uDC4D],'cyan') ");
        List<Roll> roll2 = evaluator.evaluate("color(d[\uD83D\uDDE1/\uD83D\uDC4D],'cyan') ");
        String image1Hash = getDataHash(underTest.getImageForRoll(roll1, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor())));
        String image2Hash = getDataHash(underTest.getImageForRoll(roll2, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor())));
        assertThat(roll1).isNotEqualTo(roll2);
        assertThat(image1Hash).isNotEqualTo(image2Hash);

    }

    @Test
    void getImageForRoll_polyhedral_RdD() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 7, 8, 10, 12, 20, 99, 8, 12), 1000, 10_000, true).evaluate("1d4 + 1d6 + 1d7 + 1d8 + 1d10 +1d12 + 1d20 + 1d100  + 1d8 col 'special' + 1d12 col 'special'");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("134795311673058eac57eaeecb83010b9323b0f9056bd8d80af3355d81d3e674");
    }

    @Test
    void getImageForRoll_polyhedral_RdD_specialColor() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000, 10_000, true).evaluate("1d100");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, "special"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_Expanse() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.expanse, DiceImageStyle.expanse.getDefaultColor()));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("fc777c82f5daca2e2ca1cfc1cb996f7ecc4eddacdf76bc0521513c92ff5a75f5");
    }

    @Test
    void getImageForRoll_marvelBlue() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.d6_marvel, "blue"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("f51d48f80e727ef7c1f5d623e93e332b856e4b514776c0c13b446a598771fb34");
    }

    @Test
    void getImageForRoll_marvelRed() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6), 1000, 10_000, true).evaluate("6d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.d6_marvel, "red"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("bef02d399cfabbb7c5396af3d555206e07287deba7a281fca90e501f489f7279");
    }


    @Test
    void getImageForRoll_none_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000, 10_000, true).evaluate("1d6 col 'none'");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_noneAndVisible_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2), 1000, 10_000, true).evaluate("1d6 col 'none' + 1d6");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(getDataHash(res)).isEqualTo("beb6e26993ee00f8c4fe5e9fd8f4f39cf0450626bbb25a020d18c22717808fe5");
    }

    @Test
    @Disabled
    void debug() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 20, 99, 8, 12), 1000, 10_000, true).evaluate("1d4 + 6d6 + 1d7 + 1d8 + 1d10 +1d12 + 1d20 + 1d100  + 1d8 col 'special' + 1d12 col 'special'");

        Supplier<? extends InputStream> res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()));


        File out = new File("out.png");
        assertThat(res).isNotNull();
        FileUtils.copyInputStreamToFile(res.get(), out);
    }
}