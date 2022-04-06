package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.dice.IDice;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.NumberExpression;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SumCustomSetCommandTest {
    SumCustomSetCommand underTest;
    SumCustomSetCommand.Config defaultConfig = new SumCustomSetCommand.Config(ImmutableList.of(
            new SumCustomSetCommand.LabelAndDiceExpression("1d6", "1d6"),
            new SumCustomSetCommand.LabelAndDiceExpression("3d6", "add 3d6"),
            new SumCustomSetCommand.LabelAndDiceExpression("4", "4"),
            new SumCustomSetCommand.LabelAndDiceExpression("2d10min10", "min10")
    ));

    IDice diceMock;

    static Stream<Arguments> generateGetEditButtonMessageData() {
        return Stream.of(
                Arguments.of(new SumCustomSetCommand.State("1d4", "", "user1"), "Click the buttons to add dice to the set and then on Roll"),
                Arguments.of(new SumCustomSetCommand.State("1d4", "1d4", "user1"), "user1∶ 1d4"),
                Arguments.of(new SumCustomSetCommand.State("1d4", "1d4", null), "1d4"),
                Arguments.of(new SumCustomSetCommand.State("-1d4", "-1d4", "user1"), "user1∶ -1d4")
        );
    }

    @BeforeEach
    void setup() {
        diceMock = mock(IDice.class);
        underTest = new SumCustomSetCommand(new DiceParserHelper(diceMock));
    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(SumCustomSetCommand.State state, String expected) {
        Optional<String> res = underTest.getEditButtonMessage(state, defaultConfig);
        assertThat(res).contains(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("clear", "1d6", "user1"), defaultConfig);
        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig)
                .get().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getEditButtonMessage_backLast() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("back", "1d6", "user1"), defaultConfig);

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_back() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("back", "1d6+1d6", "user1"), defaultConfig);

        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_clear() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("clear", "1d6+1d6", "user1"), defaultConfig);

        assertThat(res).isEmpty();
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage(defaultConfig).getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }

    @Test
    void getButtonMessageWithState() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig)
                .get().getContent();
        assertThat(res).isEqualTo("Click the buttons to add dice to the set and then on Roll");
    }


    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("sum_custom_set");
    }

    @Test
    void getStateFromEvent_1d6() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d6+1d21", "user1"));
    }

    @Test
    void getStateFromEvent_1d6plus() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,+1d21");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d6+1d21", "user1"));
    }

    @Test
    void getStateFromEvent_1d6minus() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,-1d21");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d6-1d21", "user1"));
    }

    @Test
    void getStateFromEvent_1d6_differentUser() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user2");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("no action", "1d6", "user1"));
    }

    @Test
    void getStateFromEvent_invalidContent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getMessageContent()).thenReturn("user1∶ asdfasfdasf");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(diceMock.roll(any())).thenThrow(new RuntimeException("test"));
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("no action", "", null));
    }

    @Test
    void getStateFromEvent_1d4_2d6_3d8_4d12_5d20() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getMessageContent()).thenReturn("user1∶ 1d4+2d6+3d8+4d12+5d20");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d4+2d6+3d8+4d12+5d20+1d21", "user1"));
    }


    @Test
    void getStateFromEvent_empty() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(event.getMessageContent()).thenReturn("Click the buttons to add dice to the set and then on Roll");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d21", "user1"));
    }

    @Test
    void getStateFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d21");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        when(event.getMessageContent()).thenReturn("Click on the buttons to add dice to the set");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("1d21", "1d21", "user1"));
    }


    @Test
    void getStateFromEvent_clear() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,clear");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("clear", "", null));
    }


    @Test
    void getStateFromEvent_backEmpty() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("back", "", "user1"));
    }

    @Test
    void getStateFromEvent_back() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6+1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("back", "1d6", "user1"));
    }

    @Test
    void getStateFromEvent_backMinus() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,back");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6-1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("back", "1d6", "user1"));
    }

    @Test
    void getStateFromEvent_roll() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,roll");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");
        assertThat(underTest.getStateFromEvent(event)).isEqualTo(new SumCustomSetCommand.State("roll", "1d6", "user1"));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_set,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("sum_custom_se")).isFalse();
    }

    @Test
    void getAnswer_roll_true() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isNotEmpty();
    }

    @Test
    void getAnswer_rollNoConfig_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumCustomSetCommand.State("roll", "", "user1"), defaultConfig);
        assertThat(res).isEmpty();
    }

    @Test
    void getAnswer_modifyMessage_false() {
        Optional<EmbedDefinition> res = underTest.getAnswer(new SumCustomSetCommand.State("1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isNotEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "", "user1"), defaultConfig);
        assertThat(res).isEmpty();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        Optional<MessageDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("+1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).isEmpty();
    }

    @Test
    void getEditButtonMessage_1d6() {
        Optional<String> res = underTest.getEditButtonMessage(new SumCustomSetCommand.State("+1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).contains("user1∶ 1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@Label")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("2_button")
                        .stringValue("2d4")
                        .build())
                .build();
        SumCustomSetCommand.Config res = underTest.getConfigFromStartOptions(option);
        assertThat(res).isEqualTo(new SumCustomSetCommand.Config(ImmutableList.of(
                new SumCustomSetCommand.LabelAndDiceExpression("Label", "+1d6"),
                new SumCustomSetCommand.LabelAndDiceExpression("+2d4", "+2d4")
        )));
    }

    @Test
    void getStartOptionsValidationMessage() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@Label")
                        .build())
                .build();
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_multiRoll() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("3x[2d6]")
                        .build())
                .build();
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("This command doesn't support multiple rolls, the following expression are not allowed: 3x[2d6]");
    }

    @Test
    void getStartOptionsValidationMessage_equal() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("1_button")
                        .stringValue("1d6@∶ test")
                        .build())
                .build();
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("This command doesn't allow '∶ ' in the dice expression and label, the following expression are not allowed: 1d6@∶ test");
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(
                new IButtonEventAdaptor.LabelAndCustomId("1d6", "sum_custom_set,1d6"),
                new IButtonEventAdaptor.LabelAndCustomId("Label", "sum_custom_set,-1d6"),
                new IButtonEventAdaptor.LabelAndCustomId("Roll", "sum_custom_set,roll"),
                new IButtonEventAdaptor.LabelAndCustomId("Clear", "sum_custom_set,clear"),
                new IButtonEventAdaptor.LabelAndCustomId("Back", "sum_custom_set,back")
        ));
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new SumCustomSetCommand.Config(ImmutableList.of(
                        new SumCustomSetCommand.LabelAndDiceExpression("1d6", "1d6"),
                        new SumCustomSetCommand.LabelAndDiceExpression("Label", "-1d6")
                )));
    }


    @Test
    void rollDice_1d6plus10() {
        when(diceMock.detailedRoll("1d6+10")).thenReturn(new ResultTree(new NDice(6, 1), 13, ImmutableList.of(
                new ResultTree(new NDice(6, 1), 3, ImmutableList.of()),
                new ResultTree(new NumberExpression(10), 10, ImmutableList.of()))));
        EmbedDefinition res = underTest.getAnswer(new SumCustomSetCommand.State("roll", "1d6+10", "user1"), defaultConfig).get();

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6+10 = 13");
        assertThat(res.getDescription()).isEqualTo("[3, 10]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("1_button",
                "2_button",
                "3_button",
                "4_button",
                "5_button",
                "6_button",
                "7_button",
                "8_button",
                "9_button",
                "10_button",
                "11_button",
                "12_button",
                "13_button",
                "14_button",
                "15_button",
                "16_button",
                "17_button",
                "18_button",
                "19_button",
                "20_button",
                "21_button",
                "22_button");
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("sum_custom_set,1d6");
        when(event.getMessageContent()).thenReturn("user1∶ 1d6");
        when(event.getInvokingGuildMemberName()).thenReturn("user1");

        SumCustomSetCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new SumCustomSetCommand.State("1d6", "1d6+1d6", "user1"));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("1d6");

        assertThat(res).isEqualTo("sum_custom_set,1d6");
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig)
                .get().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set,1d6",
                        "sum_custom_set,add 3d6",
                        "sum_custom_set,4",
                        "sum_custom_set,min10",
                        "sum_custom_set,roll",
                        "sum_custom_set,clear",
                        "sum_custom_set,back");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.getButtonMessage(defaultConfig).getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set,1d6",
                        "sum_custom_set,add 3d6",
                        "sum_custom_set,4",
                        "sum_custom_set,min10",
                        "sum_custom_set,roll",
                        "sum_custom_set,clear",
                        "sum_custom_set,back");
    }


    @Test
    void handleComponentInteractEvent() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("sum_custom_set,roll");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.getMessageContent()).thenReturn("1d6");
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(ArgumentMatchers.anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("Click the buttons to add dice to the set and then on Roll");
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(ArgumentMatchers.anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new EmbedDefinition("1d6 = 3",
                "[3]", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 60)));

        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, times(1)).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("sum_custom_set,roll");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.getMessageContent()).thenReturn("1d6");
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(ArgumentMatchers.anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click the buttons to add dice to the set and then on Roll");
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(ArgumentMatchers.anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new EmbedDefinition("1d6 = 3",
                "[3]", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 60)));

        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, times(1)).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();

    }

}