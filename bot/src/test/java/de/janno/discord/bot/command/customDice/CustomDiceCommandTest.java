package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    Dice diceMock;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(Arguments.of(ImmutableList.of(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6", "1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"), new ButtonIdLabelAndDiceExpression("2_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6", "2d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"), new ButtonIdLabelAndDiceExpression("2_button", "2d6", "2d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6 "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of(" 1d6 "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6,1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6,1d6", "1d6,1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@a,b"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "a,b", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("a"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "a", "a")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("a@Attack"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "a")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("a@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("@Attack"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("1d6@@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)),
                Arguments.of(ImmutableList.of("@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)));
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateConfigOptionStringList")
    void getConfigOptionStringList(List<String> optionValue, CustomDiceConfig expected) {
        when(diceMock.detailedRoll(any())).thenAnswer(a -> {
            String expression = a.getArgument(0);
            return dev.diceroll.parser.Dice.detailedRoll(expression);
        });
        AtomicInteger counter = new AtomicInteger(1);
        final List<CustomDiceCommand.ButtonIdAndExpression> idAndExpressions = optionValue.stream()
                .map(e -> new CustomDiceCommand.ButtonIdAndExpression(counter.getAndIncrement() + "_button", e))
                .toList();
        assertThat(underTest.getConfigOptionStringList(idAndExpressions, null, DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)).isEqualTo(expected);
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new CustomDiceCommand(messageDataDAO, diceMock, (minExcl, maxIncl) -> 3, 10);
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("1d6", StateData.empty())).orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full)).getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getConfigFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6\u0000"),
                new ButtonEventAdaptor.LabelAndCustomId("w8", "custom_dice\u00001d8\u0000")
        ));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(null, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "w8", "1d8")
        ), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full));
    }

    @Test
    void getConfigFromEventWithTargetChannel() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6\u0000123")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full));
    }

    @Test
    void getConfigFromEventLegacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full));
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("custom_dice\u00001;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("custom_dice")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_dice5")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_dice25")).isFalse();
    }

    @Test
    void getDiceResult_1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("1_button", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "3x[1d6]", "3x[1d6]")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("1_button", StateData.empty()))
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Error in `3x[1d6]`", "There need to be an operator or a separator between two values", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED));
    }


    @Test
    void getDiceResult_1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("1_button", StateData.empty()))
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("1d6: [3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6,1d6,1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("1_button", StateData.empty()))
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Label", null, ImmutableList.of(new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false)), EmbedOrMessageDefinition.Type.EMBED));
    }

    @Test
    void getName() {
        String res = underTest.getCommandId();

        assertThat(res).isEqualTo("custom_dice");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("buttons");
    }

    @Test
    void getLegacyStartOptions() {
        Collection<CommandDefinitionOption> res = underTest.additionalCommandOptions();
        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("legacy_start");
        assertThat(res.stream().flatMap(o -> o.getOptions().stream()).map(CommandDefinitionOption::getName)).containsExactly("1_button", "2_button", "3_button", "4_button", "5_button", "6_button", "7_button", "8_button", "9_button", "10_button", "11_button", "12_button", "13_button", "14_button", "15_button", "16_button", "17_button", "18_button", "19_button", "20_button", "21_button", "22_button", "23_button", "24_button", "target_channel");
    }

    @Test
    void getStateFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("custom_dice\u00002d6");
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("2d6", "custom_dice\u00002d6")));

        State<StateData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("1_button", StateData.empty()));
    }

    @Test
    void handleComponentInteractEventLegacy() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6\u0000");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(buttonEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(buttonEventAdaptor.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6")));
        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res).verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...", null);
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u001E1_button")
                                .label("1d6")
                                .build())
                        .build())
                .build());
        verify(buttonEventAdaptor).deleteMessageById(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedOrMessageDefinition("1d6 ⇒ 3", "[3]", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)), eq(null));
        verify(buttonEventAdaptor, times(5)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, times(2)).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEventLegacy_pinned() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(buttonEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(buttonEventAdaptor.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6")));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res).verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click on a button to roll the dice"), anyList());
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinition(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u001E1_button")
                                .label("1d6")
                                .build())
                        .build())
                .build());
        verify(buttonEventAdaptor, never()).deleteMessageById(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedOrMessageDefinition("1d6 ⇒ 3", "[3]", ImmutableList.of(), EmbedOrMessageDefinition.Type.EMBED)), eq(null));
        verify(messageDataDAO, times(2)).saveMessageData(any());
        verify(messageDataDAO).getAllMessageIdsForConfig(any());
        verify(buttonEventAdaptor, times(5)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, times(2)).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("2d6", StateData.empty()))
                .orElseThrow().getComponentRowDefinitions();
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button", "custom_dice2_button");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button", "custom_dice2_button");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getCurrentMessageContentChange(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full), new State<>("2d6", StateData.empty()))).isEmpty();
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice start buttons:1d6;1d20@Attack;3d10,3d10,3d10");
        when(event.getOption("start")).thenReturn(Optional.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10")
                        .build())
                .build()));

        when(event.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(event.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(event.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(event.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(diceMock.detailedRoll(any())).thenAnswer(a -> new DiceParser().detailedRoll(a.getArgument(0)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event);
        StepVerifier.create(res).verifyComplete();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event, times(2)).getOption(any());
        verify(event).reply(any(), anyBoolean());
        verify(event).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinitions(ImmutableList.of(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice1_button")
                                .label("1d6")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice2_button")
                                .label("Attack")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice3_button")
                                .label("3d10,3d10,3d10")
                                .build())
                        .build()))
                .build());

    }


    @Test
    void handleSlashCommandEvent_legacy() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice legacy_start 1_button:1d6 2_button:1d20@Attack 3_button:3x[3d10]");
        when(event.getOption("legacy_start")).thenReturn(Optional.of(CommandInteractionOption.builder()
                .name("legacy_start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("2_button")
                        .stringValue("1d20@Attack")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("3_button")
                        .stringValue("3x[3d10]")
                        .build())
                .build()));

        when(event.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(event.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(event.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(event.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(diceMock.detailedRoll(any())).thenAnswer(a -> new DiceParser().detailedRoll(a.getArgument(0)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event);
        StepVerifier.create(res).verifyComplete();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event).getOption(any());
        verify(event).reply(any(), anyBoolean());
        verify(event).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinitions(ImmutableList.of(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice1_button")
                                .label("1d6")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice2_button")
                                .label("Attack")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice3_button")
                                .label("3x[3d10]")
                                .build())
                        .build()))
                .build());

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

        assertThat(res).contains("The following expression is invalid: '2d6*10'. The error is: '*' requires as left input a single integer but was '[3, 3]'. Use /custom_dice help to get more information on how to use the command.");
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
    void handleSlashCommandEvent_help() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice help");
        when(event.getOption("help")).thenReturn(Optional.of(CommandInteractionOption.builder()
                .name("help")
                .build()));
        when(event.replyEmbed(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event);

        assertThat(res).isNotNull();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event, times(3)).getOption(any());
        verify(event).replyEmbed( EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates up to 25 buttons with custom dice expression.\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/custom_dice start buttons:3d6;10d10;3d20`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);

    }


    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        State<StateData> state = new State<>("5", StateData.empty());
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave.orElseThrow()).isEqualTo(loaded);
        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(state.getData());
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                """, "None", null);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                """, "None", null);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                """, "None", null);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }
}