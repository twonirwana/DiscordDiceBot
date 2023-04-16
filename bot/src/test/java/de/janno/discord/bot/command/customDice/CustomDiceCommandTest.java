package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
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

import java.time.Duration;
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
    PersistenceManager persistenceManager = mock(PersistenceManager.class);

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(Arguments.of(ImmutableList.of(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6", "1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"), new ButtonIdLabelAndDiceExpression("2_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6", "2d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6"), new ButtonIdLabelAndDiceExpression("2_button", "2d6", "2d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6 "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of(" 1d6 "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6,1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6,1d6", "1d6,1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@a,b"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "a,b", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("'a'"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "'a'", "'a'")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("a@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("@Attack"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("1d6@@"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)),
                Arguments.of(ImmutableList.of("@1d6"), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)));
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
        assertThat(underTest.getConfigOptionStringList(idAndExpressions, null, AnswerFormatType.full, ResultImage.none)).isEqualTo(expected);
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new CustomDiceCommand(persistenceManager, diceMock, new CachingDiceEvaluator((minExcl, maxIncl) -> 3, 10, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(UUID.randomUUID(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("1d6", StateData.empty()), 1L, 2L).orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.randomUUID(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none)).getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
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
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_diceParser_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "3x[1d6]", "3x[1d6]")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, ResultImage.none), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Multiple Results", null, ImmutableList.of(
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false)
        ), null, EmbedOrMessageDefinition.Type.EMBED));
    }

    @Test
    void getDiceResult_diceEvaluator_3x1d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "3x1d6", "3x1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("3x1d6", null, ImmutableList.of(
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false)
        ), null, EmbedOrMessageDefinition.Type.EMBED));
    }


    @Test
    void getDiceResult_1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("1d6: [3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6,1d6,1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Label", null, ImmutableList.of(new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false)), null, EmbedOrMessageDefinition.Type.EMBED));
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
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("2d6", StateData.empty()), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000", "custom_dice2_button00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000", "custom_dice2_button00000000-0000-0000-0000-000000000000");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getCurrentMessageContentChange(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6"), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none), new State<>("2d6", StateData.empty()))).isEmpty();
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

        Mono<Void> res = underTest.handleSlashCommandEvent(event, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
        StepVerifier.create(res).verifyComplete();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event).getOption(any());
        verify(event).reply(any(), anyBoolean());
        verify(event).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinitions(ImmutableList.of(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice1_button00000000-0000-0000-0000-000000000000")
                                .label("1d6")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice2_button00000000-0000-0000-0000-000000000000")
                                .label("Attack")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice3_button00000000-0000-0000-0000-000000000000")
                                .label("3d10,3d10,3d10")
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

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L);

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

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L);

        assertThat(res).contains("The following expression is invalid: '2d6*10'. The error is: '*' requires as left input a single integer but was '[3, 3]'. Try to sum the numbers together like (2d6=). Use /custom_dice help to get more information on how to use the command.");
    }


    @Test
    void handleSlashCommandEvent_help() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice help");
        when(event.getOption("help")).thenReturn(Optional.of(CommandInteractionOption.builder()
                .name("help")
                .build()));
        when(event.replyEmbed(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));

        assertThat(res).isNotNull();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event, times(2)).getOption(any());
        verify(event).replyEmbed(EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates up to 25 buttons with custom dice expression.\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/custom_dice start buttons:3d6;10d10;3d20`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);

    }

    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        assertThat(res).isEqualTo(CommandDefinition.builder()
                .name("custom_dice")
                .description("Configure dice buttons like: 1d6;2d8=;1d10+10=")
                .option(CommandDefinitionOption.builder()
                        .name("start")
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .option(CommandDefinitionOption.builder()
                                .name("buttons")
                                .description("Define one or more buttons separated by ';'")
                                .type(CommandDefinitionOption.Type.STRING)
                                .required(true)
                                .build())
                        .option(CommandDefinitionOption.builder()
                                .name("target_channel")
                                .description("Another channel where the answer will be given")
                                .type(CommandDefinitionOption.Type.CHANNEL)
                                .build())
                        .option(CommandDefinitionOption.builder()
                                .name("answer_format")
                                .description("How the answer will be displayed")
                                .type(CommandDefinitionOption.Type.STRING)
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("full")
                                        .value("full")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("without_expression")
                                        .value("without_expression")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("only_dice")
                                        .value("only_dice")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("compact")
                                        .value("compact")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("minimal")
                                        .value("minimal")
                                        .build())
                                .build())
                        .option(CommandDefinitionOption.builder()
                                .name("result_image")
                                .description("If and in what style the dice throw should be shown as image")
                                .type(CommandDefinitionOption.Type.STRING)
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("none")
                                        .value("none")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("polyhedral_3d_red_and_white")
                                        .value("polyhedral_3d_red_and_white")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("fate_black")
                                        .value("fate_black")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("polyhedral_black_and_gold")
                                        .value("polyhedral_black_and_gold")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("polyhedral_alies_blue_and_silver")
                                        .value("polyhedral_alies_blue_and_silver")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("polyhedral_green_and_gold")
                                        .value("polyhedral_green_and_gold")
                                        .build())
                                .choice(CommandDefinitionOptionChoice.builder()
                                        .name("polyhedral_red_and_gold")
                                        .value("polyhedral_red_and_gold")
                                        .build())
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
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        UUID configUUID = UUID.randomUUID();
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState()).isEqualTo(new State<>("3", StateData.empty()));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
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
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_3() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
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
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
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
                resultImage: none
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }
}