package de.janno.discord.bot.dice;

import de.janno.discord.bot.dice.DiceUtils;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiceUtilsTest {

    @Test
    void rollDice() {
        List<Integer> res = new DiceUtils().rollDiceOfType(100_000, 6);

        assertThat(res.stream())
                .noneMatch(i -> i > 6)
                .noneMatch(i -> i < 1)
                .hasSize(100_000);

        assertThat(res.stream().mapToInt(i -> i).average().orElseThrow()).isCloseTo(3.5, Offset.offset(0.05));
    }
}