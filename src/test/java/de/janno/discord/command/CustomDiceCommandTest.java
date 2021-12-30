package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import discord4j.core.GatewayDiscordClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    IDice diceMock;

    @BeforeEach
    void setup() {
        diceMock = Mockito.mock(IDice.class);
        underTest = new CustomDiceCommand(new DiceParserHelper(diceMock));
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage("1d6", ImmutableList.of());

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getConfigFromEvent() {
        List<String> res = underTest.getConfigFromEvent(TestUtils.createEventWithCustomId(mock(GatewayDiscordClient.class), "custom_dice",
                "Click on a button to roll the dice", "1d6"));
        assertThat(res).containsExactly("1d6");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_dice,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_dice")).isFalse();
    }

    @Test
    void getDiceResult_1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        List<DiceResult> res = underTest.getDiceResult("1d6", ImmutableList.of("1d6"));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("1d6 = 3");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[3]");
    }

    @Test
    void getName() {
        String res = underTest.getName();

        assertThat(res).isEqualTo("custom_dice");
    }
}