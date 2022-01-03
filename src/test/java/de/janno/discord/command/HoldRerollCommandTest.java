package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HoldRerollCommandTest {

    HoldRerollCommand underTest;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(ImmutableList.of("EMPTY", "6", "7", "", "", "0"), "reroll set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "", "7", "", "0"), "success set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "", "", "7", "0"), "failure set set [7] contains a number bigger then the sides of the die 6"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "1;4", "2;4", "3", "0"), "The numbers [4] are member of the reroll set and the success set, that is not allowed"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "1;4", "2", "3;4", "0"), "The numbers [4] are member of the reroll set and the failure set, that is not allowed"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "1,", "2;4", "3;4", "0"), "The numbers [4] are member of the success set and the failure set, that is not allowed"),
                Arguments.of(ImmutableList.of("EMPTY", "6", "2;3;4,", "5;6", "1", "0"), null)
        );
    }

    @BeforeEach
    void setup() {
        underTest = new HoldRerollCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getName() {
        String res = underTest.getName();
        assertThat(res).isEqualTo("hold_reroll");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("sides", "reroll_set", "success_set", "failure_set");
    }

    @Test
    void getDiceResult_withoutReroll() {
        List<DiceResult> res = underTest.getDiceResult("finish",
                ImmutableList.of("1;2;3;4;5;6",
                        "6",
                        "2;3;4",
                        "5;6",
                        "1",
                        "0"));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("Success: 2 and Failure: 1");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        List<DiceResult> res = underTest.getDiceResult("finish",
                ImmutableList.of("1;2;3;4;5;6",
                        "6",
                        "2;3;4",
                        "5;6",
                        "1",
                        "2"));
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("Success: 2, Failure: 1 and Rerolls: 2");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[**1**,2,3,4,**5**,**6**]");
    }

    @Test
    void getConfigFromEvent_roll3d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,3,EMPTY,6,2;3;4,5;6,1,0");

        assertThat(underTest.getConfigFromEvent(event)).containsExactly("1;1;1", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_finish() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,finish,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event)).containsExactly("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_clear() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,clear,1;2;3;4;5;6,6,2;3;4,5;6,1,0");
        assertThat(underTest.getConfigFromEvent(event)).containsExactly("EMPTY", "6", "2;3;4", "5;6", "1", "0");
    }

    @Test
    void getConfigFromEvent_reroll() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("hold_reroll,reroll,1;2;3;4;5;6,6,2;3;4,5;6,1,1");
        assertThat(underTest.getConfigFromEvent(event)).containsExactly("1;1;1;1;5;6", "6", "2;3;4", "5;6", "1", "2");
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("hold_reroll,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("hold_rerol")).isFalse();
    }

    @Test
    void getButtonMessage_clear() {
        String res = underTest.getButtonMessage("clear", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessage_finish() {
        String res = underTest.getButtonMessage("finish", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessage_noRerollPossible() {
        String res = underTest.getButtonMessage("reroll", ImmutableList.of("1;1;1;5;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice. Reroll set: [2, 3, 4], Success Set: [5, 6] and Failure Set: [1]");
    }

    @Test
    void getButtonMessage_rerollPossible() {
        String res = underTest.getButtonMessage("reroll", ImmutableList.of("1;2;3;4;5;6", "6", "2;3;4", "5;6", "1", "2"));

        assertThat(res).isEqualTo("[**1**,2,3,4,**5**,**6**] = 2 successes and 1 failures");
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(List<String> config, String expected) {
        assertThat(underTest.validate(config)).isEqualTo(expected);
    }
}