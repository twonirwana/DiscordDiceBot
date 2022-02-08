package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.api.Answer;
import de.janno.discord.api.IButtonEventAdaptor;
import de.janno.discord.api.Requester;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.NumberExpression;
import dev.diceroll.parser.ResultTree;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
                Arguments.of(new SumCustomSetCommand.State("1d4", "", "user1"), "Click on the buttons to add dice to the set"),
                Arguments.of(new SumCustomSetCommand.State("1d4", "1d4", "user1"), "user1∶ 1d4"),
                Arguments.of(new SumCustomSetCommand.State("1d4", "1d4", null), "1d4"),
                Arguments.of(new SumCustomSetCommand.State("-1d4", "-1d4", "user1"), "user1∶ -1d4")
        );
    }

    @BeforeEach
    void setup() {
        diceMock = Mockito.mock(IDice.class);
        underTest = new SumCustomSetCommand(new DiceParserHelper(diceMock));
    }


    @ParameterizedTest(name = "{index} config={0}, buttonId={1} -> {2}")
    @MethodSource("generateGetEditButtonMessageData")
    void getEditButtonMessage(SumCustomSetCommand.State state, String expected) {
        String res = underTest.getEditButtonMessage(state, defaultConfig);
        assertThat(res).isEqualTo(expected);
    }

    @Test
    void getButtonMessageWithState_clear() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("clear", "1d6", "user1"), defaultConfig);
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessageWithState_roll() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getEditButtonMessage_backLast() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("back", "1d6", "user1"), defaultConfig);

        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getEditButtonMessage_back() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("back", "1d6+1d6", "user1"), defaultConfig);

        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getEditButtonMessage_clear() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("clear", "1d6+1d6", "user1"), defaultConfig);

        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage(defaultConfig);
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
    }

    @Test
    void getButtonMessageWithState() {
        String res = underTest.getButtonMessageWithState(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isEqualTo("Click on the buttons to add dice to the set");
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
    void createNewMessage_roll_true() {
        boolean res = underTest.createAnswerMessage(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isTrue();
    }

    @Test
    void createNewMessage_rollNoConfig_false() {
        boolean res = underTest.createAnswerMessage(new SumCustomSetCommand.State("roll", "", "user1"), defaultConfig);
        assertThat(res).isFalse();
    }

    @Test
    void createNewMessage_modifyMessage_false() {
        boolean res = underTest.createAnswerMessage(new SumCustomSetCommand.State("1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_roll_true() {
        boolean res = underTest.copyButtonMessageToTheEnd(new SumCustomSetCommand.State("roll", "1d6", "user1"), defaultConfig);
        assertThat(res).isTrue();
    }

    @Test
    void copyButtonMessageToTheEnd_rollNoConfig_false() {
        boolean res = underTest.copyButtonMessageToTheEnd(new SumCustomSetCommand.State("roll", "", "user1"), defaultConfig);
        assertThat(res).isFalse();
    }

    @Test
    void copyButtonMessageToTheEnd_modifyMessage_false() {
        boolean res = underTest.copyButtonMessageToTheEnd(new SumCustomSetCommand.State("+1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).isFalse();
    }

    @Test
    void getEditButtonMessage_1d6() {
        String res = underTest.getEditButtonMessage(new SumCustomSetCommand.State("+1d6", "1d6", "user1"), defaultConfig);
        assertThat(res).isEqualTo("user1∶ 1d6");
    }

    @Test
    void getConfigValuesFromStartOptions() {
        ApplicationCommandInteractionOption option = new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("start")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("1_button")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value("1d6@Label")
                        .build())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("2_button")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value("2d4")
                        .build())
                .build(), null);
        SumCustomSetCommand.Config res = underTest.getConfigFromStartOptions(option);
        assertThat(res).isEqualTo(new SumCustomSetCommand.Config(ImmutableList.of(
                new SumCustomSetCommand.LabelAndDiceExpression("Label", "+1d6"),
                new SumCustomSetCommand.LabelAndDiceExpression("+2d4", "+2d4")
        )));
    }

    @Test
    void getStartOptionsValidationMessage() {
        ApplicationCommandInteractionOption option = new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("start")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("1_button")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value("1d6@Label")
                        .build())
                .build(), null);
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo(null);
    }

    @Test
    void getStartOptionsValidationMessage_multiRoll() {
        ApplicationCommandInteractionOption option = new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("start")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("1_button")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value("3x[2d6]")
                        .build())
                .build(), null);
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("This command doesn't support multiple rolls, the following expression are not allowed: 3x[2d6]");
    }

    @Test
    void getStartOptionsValidationMessage_equal() {
        ApplicationCommandInteractionOption option = new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("start")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("1_button")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value("1d6@∶ test")
                        .build())
                .build(), null);
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("This command doesn't allow '∶ ' in the dice expression and label, the following expression are not allowed: 1d6@∶ test");
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
        Answer res = underTest.getAnswer(new SumCustomSetCommand.State("roll", "1d6+10", "user1"), defaultConfig);

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6+10 = 13");
        assertThat(res.getContent()).isEqualTo("[3, 10]");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("1_button",
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
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(new SumCustomSetCommand.State("roll", "", "user1"), defaultConfig);

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
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
        List<LayoutComponent> res = underTest.getButtonLayout(defaultConfig);

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d6", "3d6", "4", "2d10min10", "Roll", "Clear", "Back");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
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
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("Click on the buttons to add dice to the set");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the buttons to add dice to the set"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3",
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
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click on the buttons to add dice to the set");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the buttons to add dice to the set"),
                any()
        );
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3",
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

    }

}