package de.janno.discord.bot.command.sumDiceSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SumDiceSetCommandTest {
    SumDiceSetCommand underTest;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of())), "1d4"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of())), "1d6"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of())), "1d8"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of())), "1d10"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of())), "1d12"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of())), "1d20"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of())), "1"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of())), "5"),
                Arguments.of(new State<>("+10", new SumDiceSetStateData(ImmutableList.of())), "10"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d8", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d10", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d12", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d20", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d8", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d10", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d12", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d20", -1)))), "Click on the buttons to add dice to the set"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -1)))), "Click on the buttons to add dice to the set"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of())), "-1d4"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of())), "-1d6"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of())), "-1d8"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of())), "-1d10"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of())), "-1d12"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of())), "-1d20"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of())), "-1"),
                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of())), "-5"),

                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 10)))), "5"),
                Arguments.of(new State<>("-5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", 2)))), "-3"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -2)))), "3"),
                Arguments.of(new State<>("+5", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("m", -10)))), "-5"),

                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "2d4 +1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +2d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +2d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +2d10 +1d12 +1d20"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +2d12 +1d20"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +2d20"),
                Arguments.of(new State<>("+1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 +1"),

                Arguments.of(new State<>("-1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d6 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d8 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d10 +1d12 +1d20"),
                Arguments.of(new State<>("-1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d12 +1d20"),
                Arguments.of(new State<>("-1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d20"),
                Arguments.of(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12"),
                Arguments.of(new State<>("-1", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 1), new DiceKeyAndValue("d6", 1), new DiceKeyAndValue("d8", 1), new DiceKeyAndValue("d10", 1), new DiceKeyAndValue("d12", 1), new DiceKeyAndValue("d20", 1)))), "1d4 +1d6 +1d8 +1d10 +1d12 +1d20 -1"),

                Arguments.of(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d8", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d10", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d12", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20"),
                Arguments.of(new State<>("+1d20", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 100), new DiceKeyAndValue("d6", 100), new DiceKeyAndValue("d8", 100), new DiceKeyAndValue("d10", 100), new DiceKeyAndValue("d12", 100), new DiceKeyAndValue("d20", 100)))), "100d4 +100d6 +100d8 +100d10 +100d12 +100d20")
        );
    }

    @BeforeEach
    void setup() {
        underTest = new SumDiceSetCommand(mock(MessageDataDAO.class), new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("clear", new SumDiceSetStateData(ImmutableList.of(
                new DiceKeyAndValue("d4", 1),
                new DiceKeyAndValue("d6", 1),
                new DiceKeyAndValue("d8", 1),
                new DiceKeyAndValue("d10", 1),
                new DiceKeyAndValue("d12", 1),
                new DiceKeyAndValue("d20", 1)))));
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(
                        new DiceKeyAndValue("d4", 1),
                        new DiceKeyAndValue("d6", 1),
                        new DiceKeyAndValue("d8", 1),
                        new DiceKeyAndValue("d10", 1),
                        new DiceKeyAndValue("d12", 1),
                        new DiceKeyAndValue("d20", 1)))))
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void updateDiceSet_NegativeModifier_x2() {
        List<DiceKeyAndValue> res = underTest.updateDiceSet(ImmutableList.of(
                new DiceKeyAndValue("d4", -1),
                new DiceKeyAndValue("d6", -2),
                new DiceKeyAndValue("d8", -3),
                new DiceKeyAndValue("d10", -4),
                new DiceKeyAndValue("d12", 5),
                new DiceKeyAndValue("m", -10)), "x2");
        assertThat(res).isEqualTo(ImmutableList.of(
                new DiceKeyAndValue("d4", -2),
                new DiceKeyAndValue("d6", -4),
                new DiceKeyAndValue("d8", -6),
                new DiceKeyAndValue("d10", -8),
                new DiceKeyAndValue("d12", 10),
                new DiceKeyAndValue("m", -20)));
    }

    @Test
    void updateDiceSet_limit() {
        List<DiceKeyAndValue> res = underTest.updateDiceSet(ImmutableList.of(new DiceKeyAndValue("d4", 51)), "x2");
        assertThat(res).isEqualTo(ImmutableList.of(new DiceKeyAndValue("d4", 100)));
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(new Config(null)).getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d4", 51)))))
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getCurrentMessageContentChange(State<SumDiceSetStateData> state, String expected) {
        State<SumDiceSetStateData> updated = underTest.updateState(state.getButtonValue(), state.getData());
        Optional<String> res = underTest.getCurrentMessageContentChange(new Config(null), updated);
        assertThat(res).contains(expected);
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("sum_dice_set");
    }

    @Test
    void getStateFromEvent_1d6() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d6");
        when(event.getMessageContent()).thenReturn("1d6");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 2)))));
    }

    @Test
    void getStateFromEvent_1d4_2d6_3d8_4d12_5d20() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000-1d20");
        when(event.getMessageContent()).thenReturn("1d4 +2d6 +3d8 +4d12 +5d20");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("-1d20", new SumDiceSetStateData(ImmutableList.of(
                new DiceKeyAndValue("d12", 4),
                new DiceKeyAndValue("d20", 4),
                new DiceKeyAndValue("d4", 1),
                new DiceKeyAndValue("d6", 2),
                new DiceKeyAndValue("d8", 3)
        ))));
    }

    @Test
    void getStateFromEvent_legacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d4");
        when(event.getMessageContent()).thenReturn("1d4 + 2d6");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("+1d4", new SumDiceSetStateData(ImmutableList.of(
                new DiceKeyAndValue("d4", 2),
                new DiceKeyAndValue("d6", 2)
        ))));
    }

    @Test
    void getStateFromEvent_empty() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d6");
        when(event.getMessageContent()).thenReturn("Click on the buttons to add dice to the set");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 1)))));
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set\u0000x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_se")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set+1")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_dice_set2+1")).isFalse();
    }

    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        assertThat(res).isEqualTo(CommandDefinition.builder()
                .name("sum_dice_set")
                .description("Configure a variable set of d4 to d20 dice")
                .option(CommandDefinitionOption.builder()
                        .name("start")
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .option(CommandDefinitionOption.builder()
                                .name("target_channel")
                                .description("The channel where the answer will be given")
                                .type(CommandDefinitionOption.Type.CHANNEL)
                                .build())
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name("help")
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .build());
    }

    @Test
    void getAnswer_roll_true() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of())));
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new Config(null), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of())));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_1d6() {
        Optional<String> res = underTest.getCurrentMessageContentChange(new Config(null), new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                "d6", 1
        )))));
        assertThat(res).contains("1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        Config res = underTest.getConfigFromStartOptions(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(null)
                        .build())
                .build());

        assertThat(res).isEqualTo(new Config(null));
    }


    @Test
    void rollDice_1d4plus1d6plus10() {
        EmbedDefinition res = underTest.getAnswer(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                        "d4", 1),
                new DiceKeyAndValue("d6", 1),
                new DiceKeyAndValue("m", 10)
        )))).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d4 +1d6 +10 = 12");
        assertThat(res.getDescription()).isEqualTo("[1, 1, 10]");
    }

    @Test
    void rollDice_minus1d4plus1d6minux10() {
        EmbedDefinition res = underTest.getAnswer(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue(
                        "d4", -1),
                new DiceKeyAndValue("d6", 1),
                new DiceKeyAndValue("m", -10)
        )))).orElseThrow();

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
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_dice_set\u0000+1d6");
        when(event.getMessageContent()).thenReturn("1d6");

        State<SumDiceSetStateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 2)))));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("+1d6");

        assertThat(res).isEqualTo("sum_dice_set\u001E+1d6");
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new Config(null), new State<>("roll", new SumDiceSetStateData(ImmutableList.of(
                        new DiceKeyAndValue("d4", -1),
                        new DiceKeyAndValue("d6", 1),
                        new DiceKeyAndValue("m", -10)
                ))))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set+1d4",
                        "sum_dice_set-1d4",
                        "sum_dice_set+1d6",
                        "sum_dice_set-1d6",
                        "sum_dice_setx2",
                        "sum_dice_set+1d8",
                        "sum_dice_set-1d8",
                        "sum_dice_set+1d10",
                        "sum_dice_set-1d10",
                        "sum_dice_setclear",
                        "sum_dice_set+1d12",
                        "sum_dice_set-1d12",
                        "sum_dice_set+1d20",
                        "sum_dice_set-1d20",
                        "sum_dice_setroll",
                        "sum_dice_set+1",
                        "sum_dice_set-1",
                        "sum_dice_set+5",
                        "sum_dice_set-5",
                        "sum_dice_set+10");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new Config(null)).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("+1d4", "-1d4", "+1d6", "-1d6", "x2", "+1d8", "-1d8", "+1d10", "-1d10", "Clear", "+1d12", "-1d12", "+1d20", "-1d20", "Roll", "+1", "-1", "+5", "-5", "+10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_dice_set+1d4",
                        "sum_dice_set-1d4",
                        "sum_dice_set+1d6",
                        "sum_dice_set-1d6",
                        "sum_dice_setx2",
                        "sum_dice_set+1d8",
                        "sum_dice_set-1d8",
                        "sum_dice_set+1d10",
                        "sum_dice_set-1d10",
                        "sum_dice_setclear",
                        "sum_dice_set+1d12",
                        "sum_dice_set-1d12",
                        "sum_dice_set+1d20",
                        "sum_dice_set-1d20",
                        "sum_dice_setroll",
                        "sum_dice_set+1",
                        "sum_dice_set-1",
                        "sum_dice_set+5",
                        "sum_dice_set-5",
                        "sum_dice_set+10");
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + this.getClass().getSimpleName(), null, null);
        underTest = new SumDiceSetCommand(messageDataDAO, mock(DiceUtils.class));

        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        Config config = new Config(123L);
        State<SumDiceSetStateData> state = new State<>("+1d6", new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 3), new DiceKeyAndValue("m", -4))));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());
        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<Config, SumDiceSetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "+1d6");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 4), new DiceKeyAndValue("m", -4))));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1660644934298L, 1660644934298L, "sum_dice_set", "Config", """
                ---
                answerTargetChannelId: 123
                """, "SumDiceSetStateData", """
                ---
                diceSet:
                - diceKey: "d6"
                  value: 3
                - diceKey: "m"
                  value: -4
                """);


        ConfigAndState<Config, SumDiceSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "+1d6");
        assertThat(configAndState.getConfig()).isEqualTo(new Config(123L));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumDiceSetStateData(ImmutableList.of(new DiceKeyAndValue("d6", 4), new DiceKeyAndValue("m", -4))));
    }

}