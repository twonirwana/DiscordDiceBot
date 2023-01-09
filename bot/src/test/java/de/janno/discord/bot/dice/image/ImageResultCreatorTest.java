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
                Arguments.of(ResultImage.fate_black, List.of(-1, 0, 1)));
    }

    @Test
    void getImageForRoll_ResultImageNone() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d6");

        File res = underTest.getImageForRoll(rolls, ResultImage.none);

        assertThat(res).isNull();
    }

    @Test
    void createRollCacheNameTest_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0), ResultImage.polyhedral_black_and_gold);

        assertThat(res).isEqualTo("polyhedral_black_and_gold@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void createRollCacheNameTest_fateBlack() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 1), 1000).evaluate("4d[-1,0,1]");

        String res = underTest.createRollCacheName(rolls.get(0), ResultImage.fate_black);

        assertThat(res).isEqualTo("fate_black@[-1∈[-1, 0, 1],0∈[-1, 0, 1],1∈[-1, 0, 1],-1∈[-1, 0, 1]]");
    }


    @Test
    void getImageForRoll_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("43d3709ce537a2641b6f45bb78515bde126896298034eca8e0add0300fe9b360.png");
        assertThat(getFileHash(res)).isEqualTo("704e741a90a7ca38dacd7e571863ab55bb9ae47120d0b16e7278993ac7f33b82");
    }

    @Test
    void getImageForRoll_noDie_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000).evaluate("5");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_blackGold() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d7");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("a5bc9c7ad782b4025de27b5fffe83f84c275993d3f06ef913fb0ca93f2fc968f.png");
        assertThat(getFileHash(res)).isEqualTo("f0cb399ebfb9e65265214e781e827bfaa20a720f3b76bed9fd97c81dcd09aec7");
    }

    @Test
    void getImageForRoll_D100_01_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("9b4f33844cdd200d2187bfb9b19390c1efcd39dbc9cf9e30f0985aaab6631803.png");
        assertThat(getFileHash(res)).isEqualTo("3b3f20759ccbc0177a425de9f015d16f5e9a1ffbdcab7c4d4b585dd2e28b134f");
    }

    @Test
    void getImageForRoll_D100_99_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("070fdccadb565cad95924309985ac447e72c2b91063cc791565229ff4a327ba5.png");
        assertThat(getFileHash(res)).isEqualTo("a3e5a7537f470c9d7ecefb23e16c77f9e51891ed8a39589339a95458f70c7885");
    }

    @Test
    void createRollCacheNameTest_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0), ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@[1∈[1...6],2∈[1...6],3∈[1...6],4∈[1...6],5∈[1...6],6∈[1...6]],[1∈[1...6]],[2∈[1...6],3∈[1...6],4∈[1...6]]");
    }

    @Test
    void getImageForRoll_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("57261f71cc1e660691aecda358f65c5a7fd266718b532cf44a3c3ae21389a282.png");
        assertThat(getFileHash(res)).isEqualTo("99c655db17ab28c8a0a1159cbe58bee3935b036b95a28d02c89a975f765323d6");
    }

    @Test
    void getImageForRoll_noDie_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(), 1000).evaluate("5");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D7_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d7");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("373c138d418fdb26ec1beeef91de18a4af8dd21b4e237d5bcf625c582761d124.png");
        assertThat(getFileHash(res)).isEqualTo("aeeb92835b8e23708933c7e6d3b3b728ff3e7c9ffe9531ba4c577e1a5a32edcf");
    }

    @Test
    void getImageForRoll_D100_01_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("e164754db096348941dd0eb760bb3b001d76533c141ae45ade55ac8ddc304a7f.png");
        assertThat(getFileHash(res)).isEqualTo("595bbfeb0b3e428a649edd362b5429b87acc5bf0c5bde612c2fc4b83cf590421");
    }

    @Test
    void getImageForRoll_D100_99_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("bd158cac9329a2721e4e42d3a4a3c273989105782f3fc7d44c7a8ec3580ecff9.png");
        assertThat(getFileHash(res)).isEqualTo("7a55b361153aae5947b2d13666b994bec85ba18127f702b657bea928ef60bee6");
    }

    @ParameterizedTest(name = "{index} resultImage:{0}, sides:{1} -> {2}")
    @MethodSource("generateResultImageData")
    void testPolyhedralResultImage(ResultImage resultImage, List<Integer> sides) throws ExpressionException {
        for (int d : sides) {
            for (int s = 1; s <= d; s++) {
                List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(s), 1000).evaluate("1d%d".formatted(d));
                File res = underTest.getImageForRoll(rolls, resultImage);
                assertThat(res).isNotNull();
            }
        }
    }
}