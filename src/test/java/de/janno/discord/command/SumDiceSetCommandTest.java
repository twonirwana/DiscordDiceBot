package de.janno.discord.command;

import com.google.common.collect.ImmutableMap;
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

class SumDiceSetCommandTest {
    SumDiceSetCommand underTest;

    static Stream<Arguments> generateEditMessageData() {
        return Stream.of(
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d4", "1d4"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d6", "1d6"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d8", "1d8"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d10", "1d10"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d12", "1d12"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1d20", "1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+1", "1"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+5", "5"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "+10", "10"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1)), "-1d4", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d6", 1)), "-1d6", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d8", 1)), "-1d8", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d10", 1)), "-1d10", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d12", 1)), "-1d12", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d20", 1)), "-1d20", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", 1)), "-1", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", -1)), "+1d4", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d6", -1)), "+1d6", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d8", -1)), "+1d8", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d10", -1)), "+1d10", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d12", -1)), "+1d12", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d20", -1)), "+1d20", "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", -1)), "+1", "Click on the buttons to add dice to the set"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d4", "-1d4"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d6", "-1d6"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d8", "-1d8"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d10", "-1d10"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d12", "-1d12"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1d20", "-1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-1", "-1"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of()), "-5", "-5"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", 10)), "-5", "5"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", 2)), "-5", "-3"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", -2)), "+5", "3"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("m", -10)), "+5", "-5"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d4", "2d4 +1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d6", "1d4 +2d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d8", "1d4 +1d6 +2d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d10", "1d4 +1d6 +1d8 +2d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d12", "1d4 +1d6 +1d8 +1d10 +2d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1d20", "1d4 +1d6 +1d8 +1d10 +1d12 +2d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "+1", "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 +1"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d4", "1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d6", "1d4 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d8", "1d4 +1d6 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d10", "1d4 +1d6 +1d8 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d12", "1d4 +1d6 +1d8 +1d10 +1d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1d20", "1d4 +1d6 +1d8 +1d10 +1d12"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "-1", "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 -1"),

                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d4", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d6", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d8", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d10", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d12", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.Config(ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "+1d20", "100d4 +100d6 +100d8 +100d10 +100d12 +100d20")
        );
    }

    @BeforeEach
    void setup() {
        underTest = new SumDiceSetCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void editMessage_clear() {
        String res = underTest.editMessage("clear", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 1,
                "d8", 1,
                "d10", 1,
                "d12", 1,
                "d20", 1)));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void editMessage_roll() {
        String res = underTest.editMessage("roll", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 1,
                "d8", 1,
                "d10", 1,
                "d12", 1,
                "d20", 1)));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void editMessage_x2() {
        String res = underTest.editMessage("x2", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 2,
                "d8", 3,
                "d10", 4,
                "d12", 5,
                "m", 10)));
        assertThat(res).isEqualTo("2d4 +4d6 +6d8 +8d10 +10d12 +20");
    }

    @Test
    void editMessageNegativeModifier_x2() {
        String res = underTest.editMessage("x2", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", -1,
                "d6", -2,
                "d8", -3,
                "d10", -4,
                "d12", 5,
                "m", -10)));
        assertThat(res).isEqualTo("-2d4 -4d6 -6d8 -8d10 +10d12 -20");
    }

    @Test
    void editMessage_limit() {
        String res = underTest.editMessage("x2", new SumDiceSetCommand.Config(ImmutableMap.of("d4", 51)));
        assertThat(res).isEqualTo("100d4");
    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateEditMessageData")
    void editMessage(SumDiceSetCommand.Config config, String buttonId, String expected) {
        String res = underTest.editMessage(buttonId, config);
        assertThat(res).isEqualTo(expected);
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("sum_dice_set");
    }

    @Test
    void getConfigFromEvent_1d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set,+1d21");
        when(event.getMessageContent()).thenReturn("1d6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new SumDiceSetCommand.Config(ImmutableMap.of("d6", 1)));
    }

    @Test
    void getConfigFromEvent_1d4_2d6_3d8_4d12_5d20() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set,+1d21");
        when(event.getMessageContent()).thenReturn("1d4 +2d6 +3d8 +4d12 +5d20");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 2,
                "d8", 3,
                "d12", 4,
                "d20", 5
        )));
    }

    @Test
    void getConfigFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set,+1d21");
        when(event.getMessageContent()).thenReturn("1d4 + 2d6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 2
        )));
    }

    @Test
    void getConfigFromEvent_empty() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set,+1d21");
        when(event.getMessageContent()).thenReturn("Click on the buttons to add dice to the set");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new SumDiceSetCommand.Config(ImmutableMap.of()));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_se")).isFalse();
    }

    @Test
    void createNewMessage_roll_true() {
        boolean res = underTest.createAnswerMessage("roll", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d6", 1
        )));
        assertThat(res).isTrue();
    }

    @Test
    void createNewMessage_rollNoConfig_false() {
        boolean res = underTest.createAnswerMessage("roll", new SumDiceSetCommand.Config(ImmutableMap.of()));
        assertThat(res).isFalse();
    }

    @Test
    void createNewMessage_modifyMessage_false() {
        boolean res = underTest.createAnswerMessage("+1d6", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d6", 1
        )));
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        boolean res = underTest.copyButtonMessageToTheEnd("roll", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d6", 1
        )));
        assertThat(res).isTrue();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        boolean res = underTest.copyButtonMessageToTheEnd("roll", new SumDiceSetCommand.Config(ImmutableMap.of()));
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        boolean res = underTest.copyButtonMessageToTheEnd("+1d6", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d6", 1
        )));
        assertThat(res).isFalse();
    }

    @Test
    void getButtonMessage_empty() {
        String res = underTest.getButtonMessage(null, new SumDiceSetCommand.Config(ImmutableMap.of()));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessage_1d6() {
        String res = underTest.getButtonMessage(null, new SumDiceSetCommand.Config(ImmutableMap.of(
                "d6", 1
        )));
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        SumDiceSetCommand.Config res = underTest.getConfigValuesFromStartOptions(null);
        assertThat(res).isEqualTo(new SumDiceSetCommand.Config(ImmutableMap.of()));
    }


    @Test
    void rollDice_1d4plus1d6plus10() {
        List<DiceResult> res = underTest.getDiceResult("roll", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", 1,
                "d6", 1,
                "m", 10
        )));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("1d4 +1d6 +10 = 12");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[1, 1, 10]");
    }

    @Test
    void rollDice_minus1d4plus1d6minux10() {
        List<DiceResult> res = underTest.getDiceResult("roll", new SumDiceSetCommand.Config(ImmutableMap.of(
                "d4", -1,
                "d6", 1,
                "m", -10
        )));

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getResultTitle()).isEqualTo("-1d4 +1d6 -10 = -10");
        assertThat(res.get(0).getResultDetails()).isEqualTo("[-1, 1, -10]");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res).isEmpty();
    }
}