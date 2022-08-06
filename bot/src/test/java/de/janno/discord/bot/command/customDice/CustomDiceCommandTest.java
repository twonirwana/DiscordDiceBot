package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.dice.IDice;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.ISlashEventAdaptor;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    IDice diceMock;

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(Arguments.of(ImmutableList.of(), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("1d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)),
                Arguments.of(ImmutableList.of("1d6", "1d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)),
                Arguments.of(ImmutableList.of("1d6", "2d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6"), new CustomDiceConfig.LabelAndDiceExpression("2d6", "2d6")), null)),
                Arguments.of(ImmutableList.of("1d6 "), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)),
                Arguments.of(ImmutableList.of(" 1d6 "), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)),
                Arguments.of(ImmutableList.of("2x[1d6]"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("2x[1d6]", "2x[1d6]")), null)),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("Attack", "1d6")), null)),
                Arguments.of(ImmutableList.of("1d6@a,b"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("a,b", "1d6")), null)),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("Attack", "1d6")), null)),
                Arguments.of(ImmutableList.of("a"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("@"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("a@Attack"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("a@"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("@Attack"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("1d6@1d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("1d6@@1d6"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("1d6@@"), new CustomDiceConfig(ImmutableList.of(), null)),
                Arguments.of(ImmutableList.of("@1d6"), new CustomDiceConfig(ImmutableList.of(), null)));
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
        underTest = new CustomDiceCommand(new DiceParserHelper(diceMock));
    }

    @Test
    void createNewButtonMessageWithState() {
        String res = underTest.createNewButtonMessageWithState(new CustomDiceState("1d6"), new CustomDiceConfig(ImmutableList.of(), null)).orElseThrow().getContent();

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.createNewButtonMessage(new CustomDiceConfig(ImmutableList.of(), null)).getContent();

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
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new IButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6\u0000"),
                new IButtonEventAdaptor.LabelAndCustomId("w8", "custom_dice\u00001d8\u0000")
        ));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(ImmutableList.of(
                new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6"),
                new CustomDiceConfig.LabelAndDiceExpression("w8", "1d8")
        ), null));
    }

    @Test
    void getConfigFromEventWithTargetChannel() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new IButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6\u0000123")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), 123L));
    }

    @Test
    void getConfigFromEventLegacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new IButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice\u00001d6")));
        when(event.getCustomId()).thenReturn("custom_dice\u00001d6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null));
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
        EmbedDefinition res = underTest.getAnswer(new CustomDiceState("1d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("1d6", "1d6")), null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 = 3");
        assertThat(res.getDescription()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceState("3x[1d6]"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("3x[1d6]", "3x[1d6]")), null)).orElseThrow();

        assertThat(res).isEqualTo(new EmbedDefinition("Multiple Results", null, ImmutableList.of(new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false))));
    }


    @Test
    void getDiceResult_1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceState("1d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("Label", "1d6")), null)).orElseThrow();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label: 1d6 = 3");
        assertThat(res.getDescription()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        EmbedDefinition res = underTest.getAnswer(new CustomDiceState("3x[1d6]"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("Label", "3x[1d6]")), null)).orElseThrow();

        assertThat(res).isEqualTo(new EmbedDefinition("Label", null, ImmutableList.of(new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false), new EmbedDefinition.Field("1d6 = 6", "[6]", false))));
    }

    @Test
    void getName() {
        String res = underTest.getName();

        assertThat(res).isEqualTo("custom_dice");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("1_button", "2_button", "3_button", "4_button", "5_button", "6_button", "7_button", "8_button", "9_button", "10_button", "11_button", "12_button", "13_button", "14_button", "15_button", "16_button", "17_button", "18_button", "19_button", "20_button", "21_button", "22_button", "23_button", "24_button", "target_channel");
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("custom_dice\u00002d6");

        CustomDiceState res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new CustomDiceState("2d6"));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("2d6", null);

        assertThat(res).isEqualTo("custom_dice\u00002d6\u0000");
    }

    @Test
    void createButtonCustomIdWithTargetId() {
        String res = underTest.createButtonCustomId("2d6", 123L);

        assertThat(res).isEqualTo("custom_dice\u00002d6\u0000123");
    }

    @Test
    void handleComponentInteractEvent() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6\u0000");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res).verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...", null);
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .build());
        verify(buttonEventAdaptor).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("1d6 = 3", "[3]", ImmutableList.of())), eq(null));
        assertThat(underTest.getButtonMessageCache()).containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 6019)));

        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice\u00001d6");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res).verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click on a button to roll the dice"), anyList());
        verify(buttonEventAdaptor).createButtonMessage(MessageDefinition.builder()
                .content("Click on a button to roll the dice")
                .build());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("1d6 = 3", "[3]", ImmutableList.of())), eq(null));
        assertThat(underTest.getButtonMessageCache()).containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 6019)));

        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CustomDiceState("2d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("2d6", "2d6"), new CustomDiceConfig.LabelAndDiceExpression("Attack", "1d20")), null))
                .orElseThrow().getComponentRowDefinitions();
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice\u00002d6\u0000", "custom_dice\u00001d20\u0000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("2d6", "2d6"), new CustomDiceConfig.LabelAndDiceExpression("Attack", "1d20")), null))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel)).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId)).containsExactly("custom_dice\u00002d6\u0000", "custom_dice\u00001d20\u0000");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getCurrentMessageContentChange(new CustomDiceState("2d6"), new CustomDiceConfig(ImmutableList.of(new CustomDiceConfig.LabelAndDiceExpression("2d6", "2d6"), new CustomDiceConfig.LabelAndDiceExpression("Attack", "1d20")), null))).isEmpty();
    }

    @Test
    void handleSlashCommandEvent() {
        ISlashEventAdaptor event = mock(ISlashEventAdaptor.class);
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
        when(event.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(event.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(event.reply(any())).thenReturn(Mono.just(mock(Void.class)));


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
                                .id("custom_dice\u00001d6\u0000")
                                .label("1d6")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u00001d20\u0000")
                                .label("Attack")
                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id("custom_dice\u00003x[3d10]\u0000")
                                .label("3x[3d10]")
                                .build())
                        .build()))
                .build());

    }
}