package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.EmptyData;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.dice.IDice;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import dev.diceroll.parser.Dice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    IDice diceMock;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(Arguments.of(ImmutableList.of(), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6", "1d6"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6", "2d6"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6"), new LabelAndDiceExpression("2d6", "2d6")))),
                Arguments.of(ImmutableList.of("1d6 "), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of(" 1d6 "), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("2x[1d6]"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("2x[1d6]", "2x[1d6]")))),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("Attack", "1d6")))),
                Arguments.of(ImmutableList.of("1d6@a,b"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("a,b", "1d6")))),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("Attack", "1d6")))),
                Arguments.of(ImmutableList.of("a"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("@"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("a@Attack"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("a@"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("@Attack"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@@1d6"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@@"), new CustomDiceConfig(null, ImmutableList.of())),
                Arguments.of(ImmutableList.of("@1d6"), new CustomDiceConfig(null, ImmutableList.of())));
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateConfigOptionStringList")
    void getConfigOptionStringList(List<String> optionValue, CustomDiceConfig expected) {
        when(diceMock.roll(any())).thenAnswer(a -> {
            String expression = a.getArgument(0);
            return Dice.roll(expression);
        });
        assertThat(underTest.getConfigOptionStringList(optionValue, null)).isEqualTo(expected);
    }

    @BeforeEach
    void setup() {
        diceMock = mock(IDice.class);
        underTest = new CustomDiceCommand(messageDataDAO, new DiceParserHelper(diceMock));
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(new CustomDiceConfig(null, ImmutableList.of()), new State<>("1d6", new EmptyData())).orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of())).getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getStartOptionsValidationMessage_length_withTarget_failed() {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder().options(ImmutableList.of(
                CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(931533666990059521L)
                        .build(),
                CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghij@test")
                        .build())
        ).build());

        assertThat(res).contains("The following dice expression is to long: '1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghij'. The expression must be 69 or less characters long");
    }

    @Test
    void getStartOptionsValidationMessage_length_withTarget_success() {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder().options(ImmutableList.of(
                CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(931533666990059521L)
                        .build(),
                CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghi@test")
                        .build())
        ).build());

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_length_withoutTarget_failed() {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder().options(ImmutableList.of(
                CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(null)
                        .build(),
                CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzab@test")
                        .build())
        ).build());

        assertThat(res).contains("The following dice expression is to long: '1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzab'. The expression must be 87 or less characters long");
    }

    @Test
    void getStartOptionsValidationMessage_length_withoutTarget_success() {
        Optional<String> res = underTest.getStartOptionsValidationMessage(CommandInteractionOption.builder().options(ImmutableList.of(
                CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(null)
                        .build(),
                CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6>3?a:abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyza@test")
                        .build())
        ).build());

        assertThat(res).isEmpty();
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
                new LabelAndDiceExpression("1d6", "1d6"),
                new LabelAndDiceExpression("w8", "1d8")
        )));
    }

    @Test
    void getConfigFromEventWithTargetChannel() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6\u0000123")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6"))));
    }

    @Test
    void getConfigFromEventLegacy() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new ButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6"))));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_dice\u00001;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_dice")).isFalse();
    }

    @Test
    void getDiceResult_1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("1d6", "1d6"))), new State<>("1d6", new EmptyData())).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 = 3");
        assertThat(res.getDescription()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("3x[1d6]", "3x[1d6]"))), new State<>("3x[1d6]", new EmptyData())).orElseThrow();

        assertThat(res).isEqualTo(new EmbedDefinition("Multiple Results", null, ImmutableList.of(new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false))));
    }


    @Test
    void getDiceResult_1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("Label", "1d6"))), new State<>("1d6", new EmptyData())).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label: 1d6 = 3");
        assertThat(res.getDescription()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("Label", "3x[1d6]"))), new State<>("3x[1d6]", new EmptyData())).orElseThrow();

        assertThat(res).isEqualTo(new EmbedDefinition("Label", null, ImmutableList.of(new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false))));
    }

    @Test
    void getName() {
        String res = underTest.getCommandId();

        assertThat(res).isEqualTo("custom_dice");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("1_button", "2_button", "3_button", "4_button", "5_button", "6_button", "7_button", "8_button", "9_button", "10_button", "11_button", "12_button", "13_button", "14_button", "15_button", "16_button", "17_button", "18_button", "19_button", "20_button", "21_button", "22_button", "23_button", "24_button");
    }

    @Test
    void getStateFromEvent() {
        ButtonEventAdaptor event = mock(ButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("custom_dice\u00002d6");

        State<EmptyData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("2d6", new EmptyData()));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("2d6");

        assertThat(res).isEqualTo("custom_dice\u001E2d6");
    }

    @Test
    void handleComponentInteractEvent() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6\u0000");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res).verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...", null);
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .build());
        verify(buttonEventAdaptor).deleteMessage(anyLong(), anyBoolean());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("1d6 = 3", "[3]", ImmutableList.of())), eq(null));
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res).verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click on a button to roll the dice"), anyList());
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .build());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("1d6 = 3", "[3]", ImmutableList.of())), eq(null));
        verify(messageDataDAO).saveMessageData(any());
        verify(messageDataDAO).getAllMessageIdsForConfig(any());
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("2d6", "2d6"), new LabelAndDiceExpression("Attack", "1d20"))), new State<>("2d6", new EmptyData()))
                .orElseThrow().getComponentRowDefinitions();
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice2d6", "custom_dice1d20");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("2d6", "2d6"), new LabelAndDiceExpression("Attack", "1d20"))))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice2d6", "custom_dice1d20");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getCurrentMessageContentChange(new CustomDiceConfig(null, ImmutableList.of(new LabelAndDiceExpression("2d6", "2d6"), new LabelAndDiceExpression("Attack", "1d20"))), new State<>("2d6", new EmptyData()))).isEmpty();
    }

    @Test
    void handleSlashCommandEvent() {
        SlashEventAdaptor event = mock(SlashEventAdaptor.class);
        when(event.getCommandString()).thenReturn("/custom_dice start 1_button:1d6 2_button:1d20@Attack 3_button:3x[3d10]");
        when(event.getOption("start")).thenReturn(Optional.of(CommandInteractionOption.builder()
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
        when(event.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(event.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(event.reply(any())).thenReturn(Mono.just(mock(Void.class)));
        when(diceMock.detailedRoll(any())).thenAnswer(a -> new DiceParser().detailedRoll(a.getArgument(0)));

        Mono<Void> res = underTest.handleSlashCommandEvent(event);
        StepVerifier.create(res).verifyComplete();


        verify(event).checkPermissions();
        verify(event).getCommandString();
        verify(event).getOption(any());
        verify(event).reply(any());
        verify(event).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .componentRowDefinitions(ImmutableList.of(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u001E1d6")
                                .label("1d6")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u001E1d20")
                                .label("Attack")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u001E3x[3d10]")
                                .label("3x[3d10]")
                                .build())
                        .build()))
                .build());

    }


    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + this.getClass().getSimpleName(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        CustomDiceConfig config = new CustomDiceConfig(123L, ImmutableList.of(
                new LabelAndDiceExpression("Label", "+1d6"),
                new LabelAndDiceExpression("+2d4", "+2d4")));
        State<EmptyData> state = new State<>("5", new EmptyData());
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave.orElseThrow()).isEqualTo(loaded);
        ConfigAndState<CustomDiceConfig, EmptyData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(state.getData());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1660644934298L, 1660644934298L, "custom_dice", "CustomDiceConfig", """
                ---
                answerTargetChannelId: 123
                labelAndExpression:
                - label: "Label"
                  diceExpression: "+1d6"
                - label: "+2d4"
                  diceExpression: "+2d4"
                """, "None", null);


        ConfigAndState<CustomDiceConfig, EmptyData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CustomDiceConfig(123L, ImmutableList.of(
                new LabelAndDiceExpression("Label", "+1d6"),
                new LabelAndDiceExpression("+2d4", "+2d4"))));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new EmptyData());
    }
}