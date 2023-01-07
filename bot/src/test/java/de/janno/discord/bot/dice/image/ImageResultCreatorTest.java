package de.janno.discord.bot.dice.image;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
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
    void createRollCacheNameTest() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        String res = underTest.createRollCacheName(rolls.get(0));

        assertThat(res).isEqualTo("d6s1d6s2d6s3d6s4d6s5d6s6-d6s1-d6s2d6s3d6s4");
    }

    @Test
    void getImageForRoll() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6), 1000).evaluate("6d6+1d6+3d6");

        File res = underTest.getImageForRoll(rolls);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("1d773804f78e47b55c38c68988a6c9dcf5c9907aaca5cde1fabc81c7ecffafbf.png");
        assertThat(getFileHash(res)).isEqualTo("f50b94a5d32f8756173eb3a4a1a51df5962fc585d8140fea32702ce9929f7bdb");
    }

    @Test
    void getImageForRoll_D7() throws ExpressionException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d7");

        File res = underTest.getImageForRoll(rolls);

        assertThat(res).isNull();
    }

    @Test
    void getImageForRoll_D100_00() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(100), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("569fea42b9f46c667cde27b17a8dc0569602a7a51ce19ec9326cbdc4a4bc2fa1.png");
        assertThat(getFileHash(res)).isEqualTo("1aea8477fde3cc1279cc250e0bc3b2d64905d6a83d105d26b83469bee73913fc");
    }

    @Test
    void getImageForRoll_D100_01() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(1), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("73a55f88178367f1af66c455cbd8dd49bcabaa4b1cb9812d412e3640cc733290.png");
        assertThat(getFileHash(res)).isEqualTo("36c3fc7af4c0dfa40c67023c1fd55caf56bbce0930af7e5777291d0994c0bfd5");
    }

    @Test
    void getImageForRoll_D100_99() throws ExpressionException, IOException {
        List<Roll> rolls = new DiceEvaluator(new GivenNumberSupplier(99), 1000).evaluate("1d100");

        File res = underTest.getImageForRoll(rolls);

        assertThat(res).isNotNull();
        assertThat(res).exists();
        assertThat(res.getName()).isEqualTo("d24037fc6536240bda3db52c6b62958c889104179eff37be38031076356d5bef.png");
        assertThat(getFileHash(res)).isEqualTo("57166bfb4a57cfc25d8f8e9548edc3384db068a56b4592c8c12c582575638640");
    }
}