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
        assertThat(getFileHash(res)).isEqualTo("f50b94a5d32f8756173eb3a4a1a51df5962fc585d8140fea32702ce9929f7bdb");
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
        assertThat(getFileHash(res)).isEqualTo("1aea8477fde3cc1279cc250e0bc3b2d64905d6a83d105d26b83469bee73913fc");
    }

    @Test
    void getImageForRoll_D100_01_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("309d239019987dd3d6991017a595489cc14a160e68bc0f302321958d15d740e3.png");
        assertThat(getFileHash(res)).isEqualTo("36c3fc7af4c0dfa40c67023c1fd55caf56bbce0930af7e5777291d0994c0bfd5");
    }

    @Test
    void getImageForRoll_D100_99_blackGold() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_black_and_gold);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("bc0063cead0421097605c4083c52ac00c0927f97b45dad9238f05f9378718f51.png");
        assertThat(getFileHash(res)).isEqualTo("57166bfb4a57cfc25d8f8e9548edc3384db068a56b4592c8c12c582575638640");
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
        assertThat(getFileHash(res)).isEqualTo("341399e80d88a9a73f767d89f3a72ef810d70669a2ba57fda5c518ca1a126775");
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
        assertThat(getFileHash(res)).isEqualTo("843cfc42d75ec15d9c97870757f85041cb3efc1a95f9dfd8bb65fa8ef439b2ef");
    }

    @Test
    void getImageForRoll_D100_01_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("3f7a76229877b9f5aadb3a387a0eb0d2963d4251fb4270493645e7a17d9c9c37.png");
        assertThat(getFileHash(res)).isEqualTo("9eae16b7f9e1f8677a9c27e0a2cb69a624602e8f90e1985a70b8c4b085e9e5b4");
    }

    @Test
    void getImageForRoll_D100_99_3dRedWhite() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls, ResultImage.polyhedral_3d_red_and_white);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("ce19856874881bb8da73e415f57a4a798cd3073a78d3d3b66f7397af9944b193.png");
        assertThat(getFileHash(res)).isEqualTo("cad44fb5ec6250dcbf3166017d8267b0700a70baa2a62b872e29faf4b9558f6f");
    }
}