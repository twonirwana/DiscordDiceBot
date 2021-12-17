package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.GatewayDiscordClient;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CountSuccessesCommandTest {

    CountSuccessesCommand underTest = new CountSuccessesCommand(new DiceUtils(new ArrayDeque<>(ImmutableList.of(1, 1, 1, 1, 5, 6, 6, 6))));

    @Test
    void getCommandDescription() {
        assertThat(underTest.getCommandDescription()).isEqualTo("Configure buttons for dice, with the same side, that counts successes against a target number");
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("count_successes");
    }


    @Test
    void getButtonMessage_noGlitch() {
        List<String> config = ImmutableList.of("6", "6", "no_glitch");
        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        List<String> config = ImmutableList.of("6", "6", "half_dice_one");
        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        List<String> config = ImmutableList.of("6", "6", "count_ones");
        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 and count of 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        List<String> config = ImmutableList.of("6", "6", "subtract_ones");
        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getConfigFromEvent_legacyOnlyTwo() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "count_successes",
                "1", "6", "6"))).containsExactly("6", "6", "no_glitch", "15");
    }

    @Test
    void getConfigFromEvent_legacyOnlyThree() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "count_successes",
                "1", "6", "6", "no_glitch"))).containsExactly("6", "6", "no_glitch", "15");
    }

    @Test
    void getConfigFromEvent() {
        assertThat(underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "count_successes",
                "1", "6", "6", "no_glitch", "15"))).containsExactly("6", "6", "no_glitch", "15");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("count_successes,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("count_successe")).isFalse();
    }

    @Test
    void rollDice() {
        DiceResult result = underTest.rollDice("6", ImmutableList.of("6", "6", "no_glitch", "15"));

        assertThat(result.getResultTitle()).isEqualTo("6d6 = 1");
        assertThat(result.getResultDetails()).isEqualTo("6d6: [1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        DiceResult result = underTest.rollDice("6", ImmutableList.of("6", "6", "half_dice_one", "15"));

        assertThat(result.getResultTitle()).isEqualTo("6d6 = 1 - Glitch!");
        assertThat(result.getResultDetails()).isEqualTo("6d6: [**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        DiceResult result = underTest.rollDice("8", ImmutableList.of("6", "6", "half_dice_one", "15"));

        assertThat(result.getResultTitle()).isEqualTo("8d6 = 3");
        assertThat(result.getResultDetails()).isEqualTo("8d6: [1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        DiceResult result = underTest.rollDice("6", ImmutableList.of("6", "6", "count_ones", "15"));

        assertThat(result.getResultTitle()).isEqualTo("6d6 = 1 successes and 4 ones");
        assertThat(result.getResultDetails()).isEqualTo("6d6: [**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        DiceResult result = underTest.rollDice("6", ImmutableList.of("6", "6", "subtract_ones", "15"));

        assertThat(result.getResultTitle()).isEqualTo("6d6 = -3");
        assertThat(result.getResultDetails()).isEqualTo("6d6: [**1**,**1**,**1**,**1**,5,**6**] ≥6 -1s = -3");
    }
}