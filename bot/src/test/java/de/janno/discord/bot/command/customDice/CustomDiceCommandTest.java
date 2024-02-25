package de.janno.discord.bot.command.customDice;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SnapshotExtension.class)
class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    Dice diceMock;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);
    private Expect expect;

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(Arguments.of("", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6;1d6", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false), new ButtonIdLabelAndDiceExpression("2_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6;2d6", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false), new ButtonIdLabelAndDiceExpression("2_button", "2d6", "2d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6 ", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of(" 1d6 ", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6,1d6", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6,1d6", "1d6,1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@Attack", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@a,b", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "a,b", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of(" 1d6 @ Attack ", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("'a'", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "'a'", "'a'", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("@", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@Attack", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("a@", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("@Attack", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@1d6", new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@1d6@1d6", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@@1d6", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("1d6@@", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)),
                Arguments.of("@1d6", new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH)));
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateConfigOptionStringList")
    void getConfigOptionStringList(String buttons, CustomDiceConfig expected) {
        when(diceMock.detailedRoll(any())).thenAnswer(a -> {
            String expression = a.getArgument(0);
            return dev.diceroll.parser.Dice.detailedRoll(expression);
        });
        assertThat(underTest.getConfigOptionStringList(ButtonHelper.parseString(buttons), null, AnswerFormatType.full, DiceImageStyle.none, "none", Locale.ENGLISH)).isEqualTo(expected);
    }

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new CustomDiceCommand(persistenceManager, diceMock, new CachingDiceEvaluator((minExcl, maxIncl) -> 3, 10, 0));

    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(UUID.randomUUID(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1d6", StateData.empty()), 1L, 2L).orElseThrow().getDescriptionOrContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(UUID.randomUUID(), new CustomDiceConfig(null, ImmutableList.of(), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), 1L).getDescriptionOrContent();

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
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "1d6", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_diceParser_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "3x[1d6]", "3x[1d6]", false, false)), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Multiple Results", null, ImmutableList.of(
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 6", "[6]", false)
        ), null, List.of(), EmbedOrMessageDefinition.Type.EMBED));
    }

    @Test
    void getDiceResult_diceEvaluator_3x1d6() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "3x1d6", "3x1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("3x1d6", null, ImmutableList.of(
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false),
                new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false)
        ), null, List.of(), EmbedOrMessageDefinition.Type.EMBED));
    }

    @Test
    void getDiceResult_1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label ⇒ 3");
        assertThat(res.getDescriptionOrContent()).isEqualTo("1d6: [3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Label", "1d6,1d6,1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("1_button", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(res).isEqualTo(new EmbedOrMessageDefinition("Label", null, ImmutableList.of(new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false), new EmbedOrMessageDefinition.Field("1d6 ⇒ 3", "[3]", false)), null, List.of(), EmbedOrMessageDefinition.Type.EMBED));
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
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6", false, false), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("2d6", StateData.empty()), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000", "custom_dice2_button00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6", false, false), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), 1L)
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000", "custom_dice2_button00000000-0000-0000-0000-000000000000");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getCurrentMessageContentChange(new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "2d6", "2d6", false, false), new ButtonIdLabelAndDiceExpression("2_button", "Attack", "1d20", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("2d6", StateData.empty()))).isEmpty();
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

        when(event.createMessageWithoutReference(any())).thenReturn(Mono.just(2L));
        when(event.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(event.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]", Locale.ENGLISH));
        when(event.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(diceMock.detailedRoll(any())).thenAnswer(a -> new DiceParser().detailedRoll(a.getArgument(0)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);
        StepVerifier.create(res).verifyComplete();


        verify(event).checkPermissions(Locale.ENGLISH);
        verify(event).getCommandString();
        verify(event).getOption(any());
        verify(event).reply(any(), anyBoolean());
        verify(event).createMessageWithoutReference(EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent("Click on a button to roll the dice")
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

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_invalidLayout() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6@Label;;;2d6")
                        .build())
                .build();

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);
        assertThat(res).contains("Empty rows is not allowed");
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

        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L, Locale.ENGLISH);
        assertThat(res).contains("The following expression is invalid: `2d6*10`. The error is: '*' requires as left input a single decimal but was '[3, 3]'. Try to sum the numbers together like (2d6=). Use /custom_dice help to get more information on how to use the command.");
    }

    @Test
    void handleSlashCommandEvent_help() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice help");
        when(event.getOption("help")).thenReturn(Optional.of(CommandInteractionOption.builder()
                .name("help")
                .build()));
        when(event.getRequester()).thenReturn(new Requester("userName", "channelName", "guildName", "shard 1/1", Locale.ENGLISH));
        when(event.replyWithEmbedOrMessageDefinition(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);

        assertThat(res).isNotNull();


        verify(event).checkPermissions(Locale.ENGLISH);
        verify(event).getCommandString();
        verify(event, times(2)).getOption(any());
        verify(event).replyWithEmbedOrMessageDefinition(EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates up to 25 buttons with custom dice expression.\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/custom_dice start buttons:3d6;10d10;3d20`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for News, Help and Feature Requests", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);

    }

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }

    @Test
    public void testToCommandString() {
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);

        assertThat(config.toCommandOptionsString()).isEqualTo("buttons: +1d6@Label;;+2d4 answer_format: compact dice_image_style: polyhedral_alies_v2 dice_image_color: blue_and_silver target_channel: <#123>");
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        UUID configUUID = UUID.randomUUID();
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy3() {
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy4() {
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
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy5() {
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
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.ENGLISH));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy6() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                configLocale: "de"
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy7() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                configLocale: "de"
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                  newLine: false
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                  newLine: true
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                configLocale: "de"
                buttonIdLabelAndDiceExpressions:
                - buttonId: "1_button"
                  label: "Label"
                  diceExpression: "+1d6"
                  newLine: false
                  directRoll: false
                - buttonId: "2_button"
                  label: "+2d4"
                  diceExpression: "+2d4"
                  newLine: true
                  directRoll: false
                diceParserSystem: "DICE_EVALUATOR"
                answerFormatType: compact
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """);


        ConfigAndState<CustomDiceConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new ButtonIdLabelAndDiceExpression("1_button", "Label", "+1d6", false, false),
                new ButtonIdLabelAndDiceExpression("2_button", "+2d4", "+2d4", true, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }
}