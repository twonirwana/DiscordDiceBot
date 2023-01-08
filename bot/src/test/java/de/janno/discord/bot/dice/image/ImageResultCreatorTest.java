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

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResultCreatorTest {
    private final ImageResultCreator underTest = new ImageResultCreator();

    private static String getFileHash(File file) throws IOException {
        ByteSource byteSource = Files.asByteSource(file);
        HashCode hc = byteSource.hash(Hashing.sha256());
        return hc.toString();
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

        assertThat(res).isEqualTo("polyhedral_black_and_gold@d6s1d6s2d6s3d6s4d6s5d6s6-d6s1-d6s2d6s3d6s4");
    }

    @Test
    void getImageForRoll_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("30b270d39b928863c094018e2ff7e31ee08f3487c6c195d87393f0eda54f1595.png");
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
        assertThat(res.getName()).isEqualTo("b825213d06a75708264f129737dbd20bfba52dce1b54595f5f921e50f1aa7c43.png");
        assertThat(getFileHash(res)).isEqualTo("f0cb399ebfb9e65265214e781e827bfaa20a720f3b76bed9fd97c81dcd09aec7");
    }

    @Test
    void getImageForRoll_D100_01_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("309d239019987dd3d6991017a595489cc14a160e68bc0f302321958d15d740e3.png");
        assertThat(getFileHash(res)).isEqualTo("3b3f20759ccbc0177a425de9f015d16f5e9a1ffbdcab7c4d4b585dd2e28b134f");
    }

    @Test
    void getImageForRoll_D100_99_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("bc0063cead0421097605c4083c52ac00c0927f97b45dad9238f05f9378718f51.png");
        assertThat(getFileHash(res)).isEqualTo("a3e5a7537f470c9d7ecefb23e16c77f9e51891ed8a39589339a95458f70c7885");
    }

    @Test
    void createRollCacheNameTest_3dRedWhite() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0), ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isEqualTo("polyhedral_3d_red_and_white@d6s1d6s2d6s3d6s4d6s5d6s6-d6s1-d6s2d6s3d6s4");
    }

    @Test
    void getImageForRoll_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("46e25fef4f2abae5dab1d23c01252d8dcbfab494021403542bb8fed6d2147266.png");
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
        assertThat(res.getName()).isEqualTo("fad1676eb37f022ce8fd024c68cd63477e78aeec94809a9aed0f952cd4f7e3e0.png");
        assertThat(getFileHash(res)).isEqualTo("aeeb92835b8e23708933c7e6d3b3b728ff3e7c9ffe9531ba4c577e1a5a32edcf");
    }

    @Test
    void getImageForRoll_D100_01_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("3f7a76229877b9f5aadb3a387a0eb0d2963d4251fb4270493645e7a17d9c9c37.png");
        assertThat(getFileHash(res)).isEqualTo("595bbfeb0b3e428a649edd362b5429b87acc5bf0c5bde612c2fc4b83cf590421");
    }

    @Test
    void getImageForRoll_D100_99_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("ce19856874881bb8da73e415f57a4a798cd3073a78d3d3b66f7397af9944b193.png");
        assertThat(getFileHash(res)).isEqualTo("7a55b361153aae5947b2d13666b994bec85ba18127f702b657bea928ef60bee6");
    }
}