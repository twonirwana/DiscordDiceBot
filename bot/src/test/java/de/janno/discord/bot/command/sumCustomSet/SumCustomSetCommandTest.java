package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SumCustomSetCommandTest {
    SumCustomSetCommand underTest;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    SumCustomSetConfig defaultConfig = new SumCustomSetConfig(null, ImmutableList.of(
            new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
            new ButtonIdLabelAndDiceExpression("2_button", "3d6", "add 3d6"),
            new ButtonIdLabelAndDiceExpression("3_button", "4", "4"),
            new ButtonIdLabelAndDiceExpression("4_button", "2d10min10", "min10")
    ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);

    Dice diceMock;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of(), "user1")), "Click the buttons to add dice to the set and then on Roll"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of("1d4"), "user1")), "user1∶ 1d4"),
                Arguments.of(new State<>("1d4", new SumCustomSetStateData(ImmutableList.of("1d4"), null)), "1d4"),
                Arguments.of(new State<>("-1d4", new SumCustomSetStateData(ImmutableList.of("-1d4"), "user1")), "user1∶ -1d4")
        );
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new SumCustomSetCommand(messageDataDAO, diceMock, (minExcl, maxIncl) -> 0);
    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(State<SumCustomSetStateData> state, String expected) {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, state);
        assertThat(res).contains(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getEditButtonMessage_backLast() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_back() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("back", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("clear", new SumCustomSetStateData(ImmutableList.of("+1d6", "+1d6"), "user1")));

        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(defaultConfig).getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
                .orElseThrow().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }


    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("sum_custom_set");
    }

    @Test
    void getStateFromEvent_1d6() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set\u0000+1d6")));
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6", "+1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_1d6plus() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6", "+1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_1d6minus() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000-1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("-1d6", "sum_custom_set\u0000-1d6")));

        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6", "-1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_1d6_differentUser() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user2");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("no action", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_invalidContent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getMessageContent()).thenReturn("user1∶ asdfasfdasf");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(diceMock.detailedRoll(any())).thenThrow(new RuntimeException("test"));

        State<SumCustomSetStateData> res = underTest.getStateFromEvent(event);

        //invalid expression but roll is not possible
        assertThat(res).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("asdfasfdasf", "+1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_1d4_2d6_3d8_4d12_5d20() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getMessageContent()).thenReturn("user1∶ 1d4+2d6+3d8+4d12+5d20");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d4+2d6+3d8+4d12+5d20", "+1d6"), "user1")));
    }


    @Test
    void getStateFromEvent_empty() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(event.getMessageContent()).thenReturn("Click the buttons to add dice to the set and then on Roll");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("+1d6"), "user1")));
    }

    @Test
    void getStateFromEvent_legacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000+1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("+1d6", "sum_custom_set\u0000+1d6")));
        when(event.getMessageContent()).thenReturn("Click on the buttons to add dice to the set");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("+1d6"), "user1")));
    }


    @Test
    void getStateFromEvent_clear() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000clear");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("clear", new SumCustomSetStateData(ImmutableList.of(), null)));
    }


    @Test
    void getStateFromEvent_backEmpty() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("back", new SumCustomSetStateData(ImmutableList.of(), null)));
    }

    @Test
    void getStateFromEvent_back() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6+1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        //don't work correctly with the new config
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("back", new SumCustomSetStateData(ImmutableList.of(), null)));
    }

    @Test
    void getStateFromEvent_backMinus() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6-1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        //don't work correctly with the new config
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("back", new SumCustomSetStateData(ImmutableList.of(), null)));
    }

    @Test
    void getStateFromEvent_roll() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set\u0000roll");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set\u0000x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_se")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set2_button")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set22_button")).isFalse();
    }

    @Test
    void getAnswer_roll_true() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of(), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<RollAnswer> res = underTest.getAnswer(defaultConfig, new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of(), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("+1d6", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).isEmpty();
    }

    @Test
    void getCurrentMessageContentChange_1d6() {
        Optional<String> res = underTest.getCurrentMessageContentChange(defaultConfig, new State<>("+1d6", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")));
        assertThat(res).contains("user1∶ 1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4")
                        .build())
                .build();

        SumCustomSetConfig res = underTest.getConfigFromStartOptions(option);

        assertThat(res).isEqualTo(new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "2d4", "2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full));
    }

    @Test
    void getStartOptionsValidationMessage_valid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_invalid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;2d4;2d6*10")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessageLegacy_valid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("legacy_start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@Label")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("2_button")
                        .stringValue("2d4")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessageLegacy_invalid() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("legacy_start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@Label")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("2_button")
                        .stringValue("2x[2d4]")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The following dice expression is invalid: '2x[2d4]'");
    }


    @Test
    void getConfigValuesFromStartOptions_legacy() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("legacy_start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@Label")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("2_button")
                        .stringValue("2d4")
                        .build())
                .build();
        SumCustomSetConfig res = underTest.getConfigFromStartOptions(option);
        assertThat(res).isEqualTo(new SumCustomSetConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full));
    }


    @Test
    void getConfigFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new ButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set\u00001d6\u0000"),
                new ButtonEventAdaptor.LabelAndCustomId("Label", "sum_custom_set\u0000-1d6\u0000"),
                new ButtonEventAdaptor.LabelAndCustomId("Roll", "sum_custom_set\u0000roll\u0000"),
                new ButtonEventAdaptor.LabelAndCustomId("Clear", "sum_custom_set\u0000clear\u0000"),
                new ButtonEventAdaptor.LabelAndCustomId("Back", "sum_custom_set\u0000back\u0000")
        ));
        when(event.getCustomId()).thenReturn("sum_custom_set\u00001d6\u0000");
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new SumCustomSetConfig(null, ImmutableList.of(
                        new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
                        new ButtonIdLabelAndDiceExpression("2_button", "Label", "-1d6")
                ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full));
    }

    @Test
    void getConfigFromEvent_target() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new ButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set\u00001d6\u0000123"),
                new ButtonEventAdaptor.LabelAndCustomId("Label", "sum_custom_set\u0000-1d6\u0000123"),
                new ButtonEventAdaptor.LabelAndCustomId("Roll", "sum_custom_set\u0000roll\u0000123"),
                new ButtonEventAdaptor.LabelAndCustomId("Clear", "sum_custom_set\u0000clear\u0000123"),
                new ButtonEventAdaptor.LabelAndCustomId("Back", "sum_custom_set\u0000back\u0000123")
        ));
        when(event.getCustomId()).thenReturn("sum_custom_set\u00001d6\u0000123");
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                        new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
                        new ButtonIdLabelAndDiceExpression("2_button", "Label", "-1d6")
                ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full));
    }

    @Test
    void getConfigFromEvent_legacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new ButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set\u00001d6"),
                new ButtonEventAdaptor.LabelAndCustomId("Label", "sum_custom_set\u0000-1d6"),
                new ButtonEventAdaptor.LabelAndCustomId("Roll", "sum_custom_set\u0000roll"),
                new ButtonEventAdaptor.LabelAndCustomId("Clear", "sum_custom_set\u0000clear"),
                new ButtonEventAdaptor.LabelAndCustomId("Back", "sum_custom_set\u0000back")
        ));
        when(event.getCustomId()).thenReturn("sum_custom_set\u00001d6\u0000");
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new SumCustomSetConfig(null, ImmutableList.of(
                        new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
                        new ButtonIdLabelAndDiceExpression("2_button", "Label", "-1d6")
                ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full));
    }


    @Test
    void rollDice_1d6plus10() {


        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6", "+", "10"), "user1")))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6+10 ⇒ 10");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[0]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("buttons", "always_sum_result");
    }

    @Test
    void getLegacyStartOptions() {
        Collection<CommandDefinitionOption> res = underTest.additionalCommandOptions();
        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("legacy_start");
        assertThat(res.stream().flatMap(o -> o.getOptions().stream()).map(CommandDefinitionOption::getName)).containsExactly("1_button", "2_button", "3_button", "4_button", "5_button", "6_button", "7_button", "8_button", "9_button", "10_button", "11_button", "12_button", "13_button", "14_button", "15_button", "16_button", "17_button", "18_button", "19_button", "20_button", "21_button", "target_channel");
    }


    @Test
    void getStateFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set\u00001d6")));
        when(event.getCustomId()).thenReturn("sum_custom_set\u00001d6");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");

        State<SumCustomSetStateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("1_button", new SumCustomSetStateData(ImmutableList.of("1d6", "1d6"), "user1")));
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(defaultConfig, new State<>("roll", new SumCustomSetStateData(ImmutableList.of("1d6"), "user1")))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button",
                        "sum_custom_set2_button",
                        "sum_custom_set3_button",
                        "sum_custom_set4_button",
                        "sum_custom_setroll",
                        "sum_custom_setclear",
                        "sum_custom_setback");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(defaultConfig).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button",
                        "sum_custom_set2_button",
                        "sum_custom_set3_button",
                        "sum_custom_set4_button",
                        "sum_custom_setroll",
                        "sum_custom_setclear",
                        "sum_custom_setback");
    }


    @Test
    void handleComponentInteractEventLegacy() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("sum_custom_set\u0000roll");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.getMessageContent()).thenReturn("1d6");
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(buttonEventAdaptor.getInvokingGuildMemberName()).thenReturn("testUser");

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage(eq("Click the buttons to add dice to the set and then on Roll"), notNull());
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(anyLong(), anyBoolean());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedOrMessageDefinition("1d6 ⇒ 3",
                "[3]", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)), eq(null));
        verify(buttonEventAdaptor, times(5)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, times(2)).getAllButtonIds();
        verify(buttonEventAdaptor, times(1)).getMessageContent();
    }

    @Test
    void handleComponentInteractEventLegacy_pinned() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("sum_custom_set\u0000roll");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.getInvokingGuildMemberName()).thenReturn("testUser");
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.getMessageContent()).thenReturn("1d6");
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click the buttons to add dice to the set and then on Roll"), anyList());
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedOrMessageDefinition("1d6 ⇒ 3",
                "[3]", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)), eq(null));
        verify(messageDataDAO, times(2)).saveMessageData(any());
        verify(messageDataDAO).getAllMessageIdsForConfig(any());


        verify(buttonEventAdaptor, times(5)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, times(2)).getAllButtonIds();
        verify(buttonEventAdaptor, times(1)).getMessageContent();

    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new SumCustomSetCommand(messageDataDAO, diceMock, (minExcl, maxIncl) -> 0);

        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        SumCustomSetConfig config = new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        State<SumCustomSetStateData> state = new State<>("2_button", new SumCustomSetStateData(ImmutableList.of("+2d4"), "testUser"));
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        underTest.updateCurrentMessageStateData(channelId, messageId, config, state);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("+2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "sum_custom_set", "SumCustomSetConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                alwaysSumResult: true
                answerFormatType: compact
                """,
                "SumCustomSetStateData", """
                ---
                diceExpressions:
                - "2d4"
                lockedForUserName: "testUser"
                """);


        ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> configAndState = underTest.deserializeAndUpdateState(savedData, "1_button", "testUser");
        assertThat(configAndState.getConfig()).isEqualTo(new SumCustomSetConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")
        ), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new SumCustomSetStateData(ImmutableList.of("2d4", "+1d6"), "testUser"));
    }


}