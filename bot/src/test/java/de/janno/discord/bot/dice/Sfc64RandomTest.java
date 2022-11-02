package de.janno.discord.bot.dice;

import de.janno.evaluator.dice.random.Sfc64Random;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Sfc64RandomTest {

    @Test
    void testRandom() {
        Sfc64Random sfc64Random = new Sfc64Random();
        long sum = 0;
        long totalCount = 10_000;
        for (int i = 0; i < totalCount; i++) {
            int next = sfc64Random.nextInt(100);
            assertThat(next).isBetween(0, 99);
            sum += next;
        }

        assertThat(sum).isBetween(totalCount * 45, totalCount * 55);
    }
}