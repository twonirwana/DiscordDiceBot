package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableMap;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SumDiceSetCommandTest {
    SumDiceSetCommand underTest;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new SumDiceSetCommand.State("+1d4", ImmutableMap.of()), "1d4"),
                Arguments.of(new SumDiceSetCommand.State("+1d6", ImmutableMap.of()), "1d6"),
                Arguments.of(new SumDiceSetCommand.State("+1d8", ImmutableMap.of()), "1d8"),
                Arguments.of(new SumDiceSetCommand.State("+1d10", ImmutableMap.of()), "1d10"),
                Arguments.of(new SumDiceSetCommand.State("+1d12", ImmutableMap.of()), "1d12"),
                Arguments.of(new SumDiceSetCommand.State("+1d20", ImmutableMap.of()), "1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1", ImmutableMap.of()), "1"),
                Arguments.of(new SumDiceSetCommand.State("+5", ImmutableMap.of()), "5"),
                Arguments.of(new SumDiceSetCommand.State("+10", ImmutableMap.of()), "10"),

                Arguments.of(new SumDiceSetCommand.State("-1d4", ImmutableMap.of("d4", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1d6", ImmutableMap.of("d6", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1d8", ImmutableMap.of("d8", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1d10", ImmutableMap.of("d10", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1d12", ImmutableMap.of("d12", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1d20", ImmutableMap.of("d20", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("-1", ImmutableMap.of("m", 1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d4", ImmutableMap.of("d4", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d6", ImmutableMap.of("d6", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d8", ImmutableMap.of("d8", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d10", ImmutableMap.of("d10", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d12", ImmutableMap.of("d12", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1d20", ImmutableMap.of("d20", -1)), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumDiceSetCommand.State("+1", ImmutableMap.of("m", -1)), "Click on the buttons to add dice to the set"),

                Arguments.of(new SumDiceSetCommand.State("-1d4", ImmutableMap.of()), "-1d4"),
                Arguments.of(new SumDiceSetCommand.State("-1d6", ImmutableMap.of()), "-1d6"),
                Arguments.of(new SumDiceSetCommand.State("-1d8", ImmutableMap.of()), "-1d8"),
                Arguments.of(new SumDiceSetCommand.State("-1d10", ImmutableMap.of()), "-1d10"),
                Arguments.of(new SumDiceSetCommand.State("-1d12", ImmutableMap.of()), "-1d12"),
                Arguments.of(new SumDiceSetCommand.State("-1d20", ImmutableMap.of()), "-1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1", ImmutableMap.of()), "-1"),
                Arguments.of(new SumDiceSetCommand.State("-5", ImmutableMap.of()), "-5"),

                Arguments.of(new SumDiceSetCommand.State("-5", ImmutableMap.of("m", 10)), "5"),
                Arguments.of(new SumDiceSetCommand.State("-5", ImmutableMap.of("m", 2)), "-3"),
                Arguments.of(new SumDiceSetCommand.State("+5", ImmutableMap.of("m", -2)), "3"),
                Arguments.of(new SumDiceSetCommand.State("+5", ImmutableMap.of("m", -10)), "-5"),

                Arguments.of(new SumDiceSetCommand.State("+1d4", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "2d4 +1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d6", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +2d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d8", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +2d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d10", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +2d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d12", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +2d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d20", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +1d12 +2d20"),
                Arguments.of(new SumDiceSetCommand.State("+1", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 +1"),

                Arguments.of(new SumDiceSetCommand.State("-1d4", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1d6", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1d8", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d10 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1d10", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d12 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1d12", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +1d20"),
                Arguments.of(new SumDiceSetCommand.State("-1d20", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +1d12"),
                Arguments.of(new SumDiceSetCommand.State("-1", ImmutableMap.of("d4", 1, "d6", 1, "d8", 1, "d10", 1, "d12", 1, "d20", 1)), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 -1"),

                Arguments.of(new SumDiceSetCommand.State("+1d4", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d6", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d8", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d10", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d12", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new SumDiceSetCommand.State("+1d20", ImmutableMap.of("d4", 100, "d6", 100, "d8", 100, "d10", 100, "d12", 100, "d20", 100)), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20")
        );
    }

    @BeforeEach
    void setup() {
        underTest = new SumDiceSetCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("clear", ImmutableMap.of(
                "d4", 1,
                "d6", 1,
                "d8", 1,
                "d10", 1,
                "d12", 1,
                "d20", 1)), new SumDiceSetCommand.Config());
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                        "d4", 1,
                        "d6", 1,
                        "d8", 1,
                        "d10", 1,
                        "d12", 1,
                        "d20", 1)), new SumDiceSetCommand.Config())
                .get().getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getEditButtonMessage_x2() {
        Optional<String> res = underTest.getEditButtonMessage(new SumDiceSetCommand.State("x2", ImmutableMap.of(
                "d4", 1,
                "d6", 2,
                "d8", 3,
                "d10", 4,
                "d12", 5,
                "m", 10)), new SumDiceSetCommand.Config());
        assertThat(res).contains("2d4 +4d6 +6d8 +8d10 +10d12 +20");
    }

    @Test
    void getEditButtonMessageNegativeModifier_x2() {
        Optional<String> res = underTest.getEditButtonMessage(new SumDiceSetCommand.State("x2", ImmutableMap.of(
                "d4", -1,
                "d6", -2,
                "d8", -3,
                "d10", -4,
                "d12", 5,
                "m", -10)), new SumDiceSetCommand.Config());
        assertThat(res).contains("-2d4 -4d6 -6d8 -8d10 +10d12 -20");
    }

    @Test
    void getEditButtonMessage_limit() {
        Optional<String> res = underTest.getEditButtonMessage(new SumDiceSetCommand.State("x2", ImmutableMap.of("d4", 51)), new SumDiceSetCommand.Config());
        assertThat(res).contains("100d4");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage(new SumDiceSetCommand.Config()).getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessageWithState() {
        String res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("roll", ImmutableMap.of("d4", 51)), new SumDiceSetCommand.Config())
                .get().getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(SumDiceSetCommand.State state, String expected) {
        Optional<String> res = underTest.getEditButtonMessage(state, new SumDiceSetCommand.Config());
        assertThat(res).contains(expected);
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("sum_dice_set");
    }

    @Test
    void getStateFromEvent_1d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d21");
        when(event.getMessageContent()).thenReturn("1d6");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumDiceSetCommand.State("+1d21", ImmutableMap.of("d6", 1)));
    }

    @Test
    void getStateFromEvent_1d4_2d6_3d8_4d12_5d20() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d21");
        when(event.getMessageContent()).thenReturn("1d4 +2d6 +3d8 +4d12 +5d20");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumDiceSetCommand.State("+1d21", ImmutableMap.of(
                "d4", 1,
                "d6", 2,
                "d8", 3,
                "d12", 4,
                "d20", 5
        )));
    }

    @Test
    void getStateFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d21");
        when(event.getMessageContent()).thenReturn("1d4 + 2d6");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumDiceSetCommand.State("+1d21", ImmutableMap.of(
                "d4", 1,
                "d6", 2
        )));
    }

    @Test
    void getStateFromEvent_empty() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d21");
        when(event.getMessageContent()).thenReturn("Click on the buttons to add dice to the set");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumDiceSetCommand.State("+1d21", ImmutableMap.of()));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set\u0000x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_se")).isFalse();
    }

    @Test
    void getAnswer_roll_true() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                "d6", 1
        )), new SumDiceSetCommand.Config());
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumDiceSetCommand.State("roll", ImmutableMap.of()), new SumDiceSetCommand.Config());
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumDiceSetCommand.State("+1d6", ImmutableMap.of(
                "d6", 1
        )), new SumDiceSetCommand.Config());
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                "d6", 1
        )), new SumDiceSetCommand.Config());
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("roll", ImmutableMap.of()), new SumDiceSetCommand.Config());
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("+1d6", ImmutableMap.of(
                "d6", 1
        )), new SumDiceSetCommand.Config());
        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_1d6() {
        Optional<String> res = underTest.getEditButtonMessage(new SumDiceSetCommand.State("+1d6", ImmutableMap.of(
                "d6", 1
        )), new SumDiceSetCommand.Config());
        assertThat(res).contains("2d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        SumDiceSetCommand.Config res = underTest.getConfigFromStartOptions(null);
        assertThat(res).isEqualTo(new SumDiceSetCommand.Config());
    }


    @Test
    void rollDice_1d4plus1d6plus10() {
        EmbedDefinition res = underTest.getAnswer(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                "d4", 1,
                "d6", 1,
                "m", 10
        )), new SumDiceSetCommand.Config()).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d4 +1d6 +10 = 12");
        assertThat(res.getDescription()).isEqualTo("[1, 1, 10]");
    }

    @Test
    void rollDice_minus1d4plus1d6minux10() {
        EmbedDefinition res = underTest.getAnswer(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                "d4", -1,
                "d6", 1,
                "m", -10
        )), new SumDiceSetCommand.Config()).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("-1d4 +1d6 -10 = -10");
        assertThat(res.getDescription()).isEqualTo("[-1, 1, -10]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res).isEmpty();
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d6");
        when(event.getMessageContent()).thenReturn("1d6");

        SumDiceSetCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new SumDiceSetCommand.State("+1d6", ImmutableMap.of("d6", 1)));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("+1d6");

        assertThat(res).isEqualTo("sum_dice_set\u0000+1d6");
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.getButtonMessageWithState(new SumDiceSetCommand.State("roll", ImmutableMap.of(
                        "d4", -1,
                        "d6", 1,
                        "m", -10
                )), new SumDiceSetCommand.Config())
                .get().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set\u0000+1d4", "sum_dice_set\u0000-1d4", "sum_dice_set\u0000+1d6", "sum_dice_set\u0000-1d6",
                        "sum_dice_set\u0000x2", "sum_dice_set\u0000+1d8", "sum_dice_set\u0000-1d8", "sum_dice_set\u0000+1d10", "sum_dice_set\u0000-1d10",
                        "sum_dice_set\u0000clear", "sum_dice_set\u0000+1d12", "sum_dice_set\u0000-1d12", "sum_dice_set\u0000+1d20", "sum_dice_set\u0000-1d20",
                        "sum_dice_set\u0000roll", "sum_dice_set\u0000+1", "sum_dice_set\u0000-1", "sum_dice_set\u0000+5", "sum_dice_set\u0000-5", "sum_dice_set\u0000+10");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.getButtonMessage(new SumDiceSetCommand.Config()).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set\u0000+1d4", "sum_dice_set\u0000-1d4", "sum_dice_set\u0000+1d6", "sum_dice_set\u0000-1d6",
                        "sum_dice_set\u0000x2", "sum_dice_set\u0000+1d8", "sum_dice_set\u0000-1d8", "sum_dice_set\u0000+1d10", "sum_dice_set\u0000-1d10",
                        "sum_dice_set\u0000clear", "sum_dice_set\u0000+1d12", "sum_dice_set\u0000-1d12", "sum_dice_set\u0000+1d20", "sum_dice_set\u0000-1d20",
                        "sum_dice_set\u0000roll", "sum_dice_set\u0000+1", "sum_dice_set\u0000-1", "sum_dice_set\u0000+5", "sum_dice_set\u0000-5", "sum_dice_set\u0000+10");
    }
}