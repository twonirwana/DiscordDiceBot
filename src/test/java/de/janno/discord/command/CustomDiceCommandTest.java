package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import discord4j.discordjson.json.ApplicationCommandOptionData;
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
        String res = underTest.getButtonMessage(new CustomDiceCommand.State("1d6"), new CustomDiceCommand.Config(ImmutableList.of()));

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of("custom_dice,1d6"));
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceCommand.Config(ImmutableList.of("1d6")));
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
        List<DiceResult> res = underTest.getDiceResult(new CustomDiceCommand.State("1d6"), new CustomDiceCommand.Config(ImmutableList.of("1d6")));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("1d6 = 3");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[3]");
    }

    @Test
    void getName() {
        String res = underTest.getName();

        assertThat(res).isEqualTo("custom_dice");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("1_button",
                "2_button",
                "3_button",
                "4_button",
                "5_button",
                "6_button",
                "7_button",
                "8_button",
                "9_button",
                "10_button",
                "11_button",
                "12_button",
                "13_button",
                "14_button",
                "15_button",
                "16_button",
                "17_button",
                "18_button",
                "19_button",
                "20_button",
                "21_button",
                "22_button",
                "23_button",
                "24_button",
                "25_button");
    }
}