package de.janno.discord.bot.dice.image;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import de.janno.discord.bot.ResultImage;
import de.janno.evaluator.dice.DiceEvaluator;
import de.janno.evaluator.dice.ExpressionException;
import de.janno.evaluator.dice.Roll;
import de.janno.evaluator.dice.random.GivenNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResultCreatorTest {
    private final ImageResultCreator underTest = new ImageResultCreator();

    private static String getFileHash(File file) throws IOException {
        ByteSource byteSource = Files.asByteSource(file);
        HashCode hc = byteSource.hash(Hashing.sha256());
        return hc.toString();
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
                //Arguments.of(DiceImageStyle.polyhedral_2d, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.polyhedral_knots, List.of(4, 6, 8, 10, 12, 20, 100)),
                Arguments.of(DiceImageStyle.fate, List.of(-1, 0, 1)),
                Arguments.of(DiceImageStyle.d6_dots, List.of(6))
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
        FileUtils.cleanDirectory(new File("imageCache/"));
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileUtils.cleanDirectory(new File("imageCache/"));
    }

    @Test
    void getImageForRoll_ResultImageNone() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d6");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.none, "none"));

        assertThat(res).isNull();
    }

    @Test
    void createRollCacheNameTest_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isEqualTo("polyhedral_alies_v1_black_and_gold@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void createRollCacheNameTest_multiColor() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 8, 10, 12, 20, 99), 1000).evaluate("color(1d4,'gray') + color(1d6,'black') + color(1d8,'white') + color(1d10,'red') + color(1d12,'blue') + color(1d20,'green') + color(1d100,'orange')");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "red"));

        assertThat(res).isEqualTo("polyhedral_2d_red@[gray:4∈[1...4]],[black:6∈[1...6]],[white:8∈[1...8]],[red:10∈[1...10]],[blue:12∈[1...12]],[green:20∈[1...20]],[orange:99∈[1...100]]");
    }

    @Test
    void createRollCacheNameTest_multiCol() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 8, 10, 12, 20, 99), 1000).evaluate("1d4 col 'gray' + 1d6 col 'black' + 1d8 col 'white' + 1d10 col 'red' + 1d12 col 'blue' + 1d20 col 'green' + 1d100 col 'orange'");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, "red"));

        assertThat(res).isEqualTo("polyhedral_2d_red@[gray:4∈[1...4]],[black:6∈[1...6]],[white:8∈[1...8]],[red:10∈[1...10]],[blue:12∈[1...12]],[green:20∈[1...20]],[orange:99∈[1...100]]");
    }

    @Test
    void createRollCacheNameTest_fateBlack() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 1), 1000).evaluate("4d[-1,0,1]");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.fate, "black"));

        assertThat(res).isEqualTo("fate_black@[-1∈[-1, 0, 1],0∈[-1, 0, 1],1∈[-1, 0, 1],-1∈[-1, 0, 1]]");
    }

    @Test
    void createRollCacheNameTest_explode() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000).evaluate("3d!6");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[6∈[1...6],6∈[1...6],5∈[1...6],5∈[1...6],4∈[1...6]]");
    }

    @Test
    void createRollCacheNameTest_explodeAdd() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000).evaluate("3d!!6");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[6∈[1...6],6∈[1...6],5∈[1...6],5∈[1...6],4∈[1...6]]");
    }

    @Test
    void getImageForRoll_explode() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000).evaluate("3d!6");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("80d2e9d37b7823ea0e1e340b0d39bf76c560a773967983898c44cfdd036fba64.png");
        assertThat(getFileHash(res)).isEqualTo("a78152e41956d655a4aa3428886e0be6c08c02f9dff25db08befb5e7086b3aae");
    }

    @Test
    void getImageForRoll_explodeAdd() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(6, 6, 5, 5, 4, 3), 1000).evaluate("3d!!6");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("d61dc18fdf623c75a8292736f3cf5cfa56085fdc9815989c00cc049ead21e27b.png");
        assertThat(getFileHash(res)).isEqualTo("4b433f278f98eeac7c0f838969b40fcdb1397cb82d662aeb22a47f5a326a5363");
    }

    @Test
    void getImageForRoll_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("7aa4997dc0a11e1669b443090eaa6659c261f65387389a236896061342404120.png");
        assertThat(getFileHash(res)).isEqualTo("704e741a90a7ca38dacd7e571863ab55bb9ae47120d0b16e7278993ac7f33b82");
    }

    @Test
    void getImageForRoll_noDie_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000).evaluate("5");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d7");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("7826d692fa9123c730674d847b37463c456e233245a08e40fbfb73af006a98e4.png");
        assertThat(getFileHash(res)).isEqualTo("f0cb399ebfb9e65265214e781e827bfaa20a720f3b76bed9fd97c81dcd09aec7");
    }

    @Test
    void getImageForRoll_D100_01_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("345c673601f34b98a86c7996ec632ea67997957dadc46fc6d30e7d0b789bd568.png");
        assertThat(getFileHash(res)).isEqualTo("3b3f20759ccbc0177a425de9f015d16f5e9a1ffbdcab7c4d4b585dd2e28b134f");
    }

    @Test
    void getImageForRoll_D100_99_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("2ad274a0488eb8c9c9aeb6ca30501edc8bd441c18dab77326f55d4f3e0a93bf7.png");
        assertThat(getFileHash(res)).isEqualTo("a3e5a7537f470c9d7ecefb23e16c77f9e51891ed8a39589339a95458f70c7885");
    }

    @Test
    void getImageForRoll_cache() throws ExpressionException, IOException, InterruptedException {
        List<Roll> rolls1 = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d6");
        File res1 = underTest.getImageForRoll(rolls1, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(res1).isNotNull();
        assertThat(res1).exists();
        long res1LastModified = res1.lastModified();
        assertThat(res1.getName()).isEqualTo("cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png");
        assertThat(getFileHash(res1)).isEqualTo("933002493c0ccf2ea6ad67c8656342d3f02642a19a840b032a62346e4a7a048b");

        Thread.sleep(10);

        List<Roll> rolls2 = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d6");
        File cachedRes1 = underTest.getImageForRoll(rolls2, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(cachedRes1).isNotNull();
        assertThat(cachedRes1).exists();
        assertThat(cachedRes1.lastModified()).isEqualTo(res1LastModified);
        assertThat(cachedRes1.getName()).isEqualTo("cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png");
        assertThat(getFileHash(cachedRes1)).isEqualTo("933002493c0ccf2ea6ad67c8656342d3f02642a19a840b032a62346e4a7a048b");

        Thread.sleep(10);

        List<Roll> rolls3 = new DiceEvaluator(new GivenNumberSupplier(2), 1000).evaluate("1d6");
        File res2 = underTest.getImageForRoll(rolls3, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));
        assertThat(res2).isNotNull();
        assertThat(res2).exists();
        assertThat(res2.lastModified()).isNotEqualTo(res1LastModified);
        assertThat(res2.getName()).isEqualTo("dbb28d1b4a2eb15d9a4b2301e76f751f0129efe722d94b4551049fc168524162.png");
        assertThat(getFileHash(res2)).isEqualTo("beb6e26993ee00f8c4fe5e9fd8f4f39cf0450626bbb25a020d18c22717808fe5");

    }

    @Test
    void createRollCacheNameTest_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0), new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void getImageForRoll_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("57261f71cc1e660691aecda358f65c5a7fd266718b532cf44a3c3ae21389a282.png");
        assertThat(getFileHash(res)).isEqualTo("2956ce00e097a7332f6769e37c69c7120074918316102b614758d65486c8cbfb");
    }

    @Test
    void getImageForRoll_noDie_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000).evaluate("5");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d7");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("373c138d418fdb26ec1beeef91de18a4af8dd21b4e237d5bcf625c582761d124.png");
        assertThat(getFileHash(res)).isEqualTo("60679ea2b91f3607f99cf9029c5cc6a552d245fab3fcd963ac88de437cf7118a");
    }

    @Test
    void getImageForRoll_D100_01_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("e164754db096348941dd0eb760bb3b001d76533c141ae45ade55ac8ddc304a7f.png");
        assertThat(getFileHash(res)).isEqualTo("2333543c0c9813f3013b2b781f8614dfba8accea373755301cae4b354d971337");
    }

    @Test
    void getImageForRoll_D100_99_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("bd158cac9329a2721e4e42d3a4a3c273989105782f3fc7d44c7a8ec3580ecff9.png");
        assertThat(getFileHash(res)).isEqualTo("fffee64a526f6fdd0e4a1e44cbcfcda0fe458709d3b17c7ff8745556e386ef67");
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1} -> {2}")
    @MethodSource("generateResultImageData")
    void testLegacyPolyhedralResultImage(ResultImage resultImage, List<Integer> sides) throws ExpressionException {
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000).evaluate("1d%d".formatted(d));
                File res = underTest.getImageForRoll(rolls, LegacyImageConfigHelper.getStyleAndColor(resultImage));
                assertThat(res).isNotNull();
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d[abc/cde]");
        File res = underTest.getImageForRoll(rolls, LegacyImageConfigHelper.getStyleAndColor(resultImage));
        assertThat(res).isNull();
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1} -> {2}")
    @MethodSource("generateDiceStyleData")
    void testPolyhedralResultImage(DiceImageStyle diceImageStyle, List<Integer> sides) throws ExpressionException {
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000).evaluate("1d%d".formatted(d));
                for (String color : diceImageStyle.getSupportedColors()) {
                    File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, color));
                    assertThat(res).isNotNull();
                }
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d[abc/cde]");
        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, "notAColor"));
        assertThat(res).isNull();
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1}, color:{2} -> {3}")
    @MethodSource("generateDiceStyleDataPerColor")
    void testPolyhedralResultImagePerColor(DiceImageStyle diceImageStyle, List<Integer> sides, String color) throws ExpressionException {
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000).evaluate("1d%d".formatted(d));
                File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, color));
                assertThat(res).isNotNull();
            }
        }
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d[abc/cde]");
        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(diceImageStyle, "notAColor"));
        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_polyhedralDrawColor() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 8, 10, 12, 20, 99), 1000).evaluate("color(1d4,'gray') + color(1d6,'black') + color(1d8,'white') + color(1d10,'red') + color(1d12,'blue') + color(1d20,'green') + color(1d100,'orange')");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_2d, DiceImageStyle.polyhedral_2d.getDefaultColor()));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("06a75350871a27e834984e9d7df253106cbb3e1d5567ca129dc998f0bf851b01.png");
        assertThat(getFileHash(res)).isEqualTo("95e4da4728bbbb27416801a6be380a7ac886ec254da2a9f28e9117c948e08c76");
        assertThat(res).exists();
    }

    @Test
    void getImageForRoll_polyhedral_RdD() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(4, 6, 7, 8, 10, 12, 20, 99, 8, 12), 1000).evaluate("1d4 + 1d6 + 1d7 + 1d8 + 1d10 +1d12 + 1d20 + 1d100  + 1d8 col 'special' + 1d12 col 'special'");

        File res = underTest.getImageForRoll(rolls, new DiceStyleAndColor(DiceImageStyle.polyhedral_RdD, DiceImageStyle.polyhedral_RdD.getDefaultColor()));

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("61c14c61b4aa28b648319b8e36b62ef9a1c61720c2e753cd34c0e97a89b67784.png");
        assertThat(getFileHash(res)).isEqualTo("de01222cf4ea85e2fe2eea6539ebdfe9809bfe88f6dfb8ece5736df9eeb6603d");
        assertThat(res).exists();
    }
}