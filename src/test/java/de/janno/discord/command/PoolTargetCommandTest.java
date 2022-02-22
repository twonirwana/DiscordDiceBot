package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.api.Answer;
import de.janno.discord.api.IButtonEventAdaptor;
import de.janno.discord.api.Requester;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceUtils;
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
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PoolTargetCommandTest {

    PoolTargetCommand underTest;

    private static Stream<Arguments> getStateFromEvent() {
        return Stream.of(
                //set pool
                Arguments.of("pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY", new PoolTargetCommand.State(15, null, null)),
                Arguments.of("pool_target,15,10,20,10;9,1;2,always,EMPTY,EMPTY", new PoolTargetCommand.State(15, null, null)),

                //set target
                Arguments.of("pool_target,8,10,20,10;9,1;2,ask,15,EMPTY", new PoolTargetCommand.State(15, 8, null)),
                Arguments.of("pool_target,8,10,20,10;9,1;2,always,15,EMPTY", new PoolTargetCommand.State(15, 8, true)),

                //clear
                Arguments.of("pool_target,clear,10,20,10;9,1;2,ask,15,EMPTY", new PoolTargetCommand.State(null, null, null)),
                Arguments.of("pool_target,clear,10,20,10;9,1;2,always,15,EMPTY", new PoolTargetCommand.State(null, null, null)),

                //ask reroll
                Arguments.of("pool_target,do_reroll,10,20,10;9,1;2,ask,15,9", new PoolTargetCommand.State(15, 9, true)),
                Arguments.of("pool_target,no_reroll,10,20,10;9,1;2,ask,15,9", new PoolTargetCommand.State(15, 9, false))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new PoolTargetCommand(new DiceUtils(1, 1, 1, 2, 5, 6, 6, 6, 2, 10, 10, 2, 3, 4, 5, 6, 7, 8));
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("getStateFromEvent")
    void getStateFromEvent(String customButtonId, PoolTargetCommand.State expected) {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn(customButtonId);
        assertThat(underTest.getStateFromEvent(buttonEventAdaptor)).isEqualTo(expected);
    }

    @Test
    void getName() {
        String res = underTest.getName();
        assertThat(res).isEqualTo("pool_target");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("sides", "max_dice", "reroll_set", "botch_set", "reroll_variant");
    }

    @Test
    void getDiceResult_withoutReroll() {
        Answer res = underTest.getAnswer(new PoolTargetCommand.State(6, 3, false),
                new PoolTargetCommand.Config(6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask"));
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 = -1");
        assertThat(res.getContent()).isEqualTo("[**1**,**1**,**1**,2,**5**,**6**] ≥3 = -1");
    }

    @Test
    void getDiceResult_withReroll() {
        Answer res = underTest.getAnswer(new PoolTargetCommand.State(6, 3, true),
                new PoolTargetCommand.Config(6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask"));
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 = 1");
        assertThat(res.getContent()).isEqualTo("[**1**,**1**,**1**,2,2,**5**,**6**,**6**,**6**] ≥3 = 1");
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("pool_target,do_reroll,10,20,10;9,1;2,ask,15,9");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new PoolTargetCommand.Config(
                10,
                20,
                ImmutableSet.of(9, 10),
                ImmutableSet.of(1, 2),
                "ask"));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("pool_target,15,10,15,10,1,ask,EMPTY,EMPTY")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("pool_targe")).isFalse();
    }

    @Test
    void createAnswerMessage_allStateInfoAvailable() {
        assertThat(underTest.createAnswerMessage(
                new PoolTargetCommand.State(10, 8, true),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isTrue();
    }

    @Test
    void createAnswerMessage_dicePoolMissing() {
        assertThat(underTest.createAnswerMessage(
                new PoolTargetCommand.State(null, 8, true),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isFalse();
    }

    @Test
    void createAnswerMessage_targetNumberMissing() {
        assertThat(underTest.createAnswerMessage(
                new PoolTargetCommand.State(10, null, true),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isFalse();
    }

    @Test
    void createAnswerMessage_doRerollMissing() {
        assertThat(underTest.createAnswerMessage(
                new PoolTargetCommand.State(10, 8, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isFalse();
    }

    @Test
    void getButtonMessage_rerollBotchEmpty() {
        String res = underTest.getButtonMessage(new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(), "ask"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_rerollEmpty() {
        String res = underTest.getButtonMessage(new PoolTargetCommand.Config(10, 20, ImmutableSet.of(), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with botch:1,2");
    }

    @Test
    void getButtonMessage_botchEmpty() {
        String res = underTest.getButtonMessage(new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(), "ask"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10");
    }

    @Test
    void getButtonMessage_ask() {
        String res = underTest.getButtonMessage(new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonMessage_always() {
        String res = underTest.getButtonMessage(new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "always"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with always reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonMessageWithState_poolWasSet() {
        String res = underTest.getButtonMessageWithState(
                new PoolTargetCommand.State(10, null, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Click on the target to roll 10d10 against it, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonMessageWithState_targetWasSet() {
        String res = underTest.getButtonMessageWithState(
                new PoolTargetCommand.State(10, 10, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Should 10s,9s in 10d10 against 10 be be rerolled?");
    }

    @Test
    void getButtonMessageWithState_clear() {
        String res = underTest.getButtonMessageWithState(
                new PoolTargetCommand.State(null, null, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonLayoutWithState_missingDoReroll_askForReroll() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(
                new PoolTargetCommand.State(10, 10, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("Reroll", "No reroll");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("pool_target,do_reroll,10,20,10;9,1;2,ask,10,10",
                        "pool_target,no_reroll,10,20,10;9,1;2,ask,10,10");
    }

    @Test
    void getButtonLayoutWithState_statesAreGiven_newButtons() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(
                new PoolTargetCommand.State(10, 10, true),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("pool_target,1,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,2,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,3,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,4,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,5,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,6,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,7,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,8,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,9,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,10,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,11,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,12,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,13,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,14,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,16,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,17,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,18,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,19,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,20,10,20,10;9,1;2,ask,EMPTY,EMPTY");
    }

    @Test
    void getButtonLayoutWithState_missingTarget_askTarget() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(
                new PoolTargetCommand.State(10, null, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("2", "3", "4", "5", "6", "7", "8", "9", "10", "Clear");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("pool_target,2,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,3,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,4,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,5,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,6,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,7,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,8,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,9,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,10,10,20,10;9,1;2,ask,10,EMPTY",
                        "pool_target,clear,10,20,10;9,1;2,ask,EMPTY,EMPTY");
    }

    @Test
    void getButtonLayoutWithState_missingAll_askPool() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(
                new PoolTargetCommand.State(null, null, null),
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("pool_target,1,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,2,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,3,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,4,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,5,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,6,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,7,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,8,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,9,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,10,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,11,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,12,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,13,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,14,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,16,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,17,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,18,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,19,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,20,10,20,10;9,1;2,ask,EMPTY,EMPTY");
    }


    @Test
    void getButtonLayout() {
        List<LayoutComponent> res = underTest.getButtonLayout(
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get()))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get()))
                .containsExactly("pool_target,1,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,2,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,3,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,4,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,5,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,6,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,7,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,8,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,9,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,10,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,11,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,12,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,13,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,14,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,16,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,17,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,18,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,19,10,20,10;9,1;2,ask,EMPTY,EMPTY",
                        "pool_target,20,10,20,10;9,1;2,ask,EMPTY,EMPTY");
    }

    @Test
    void validate_valid() {
        String res = underTest.validate(
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isNull();
    }

    @Test
    void validate_numberInRerollSetToBig() {
        String res = underTest.validate(
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9, 12), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("Reroll set [10, 9, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_numberInBotSetToBig() {
        String res = underTest.validate(
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2, 12), "ask"));

        assertThat(res).isEqualTo("Botch set [1, 2, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_toManyNumberInRerollSet() {
        String res = underTest.validate(
                new PoolTargetCommand.Config(10, 20, ImmutableSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEqualTo("The reroll must not contain all numbers");
    }

    @Test
    void handleComponentInteractEvent_clear() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the target to roll 15d10 against it, with ask reroll:9,10 and botch:1,2"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any());
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 840368694)));
        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_setTargetAsk() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target,8,10,20,10;9,1;2,ask,15,EMPTY");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Should 10s,9s in 15d10 against 8 be be rerolled?"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any());
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 840368694)));
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_setTargetAlways() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target,8,10,20,10;9,1;2,always,15,EMPTY");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the buttons to roll dice, with always reroll:9,10 and botch:1,2"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("15d10 = -4",
                "[**1**,**1**,**1**,**2**,**2**,**2**,3,4,5,5,6,6,6,6,7,**10**,**10**] ≥8 = -4", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, -574285364)));
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_setReroll() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target,do_reroll,10,20,10;9,1;2,ask,15,8");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("15d10 = -4",
                "[**1**,**1**,**1**,**2**,**2**,**2**,3,4,5,5,6,6,6,6,7,**10**,**10**] ≥8 = -4", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 840368694)));
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_clearPinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target,15,10,20,10;9,1;2,ask,EMPTY,EMPTY");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on the target to roll 15d10 against it, with ask reroll:9,10 and botch:1,2"),
                any()
        );
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any());

        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 840368694)));
        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }


    private ApplicationCommandInteractionOption createCommandInteractionOption(String sides,
                                                                               String maxDice,
                                                                               String rerollSet,
                                                                               String botchSet,
                                                                               String rerollVariant) {
        return new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("start")
                .type(ApplicationCommandOption.Type.SUB_COMMAND.getValue())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("sides")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .value(sides)
                        .build())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("max_dice")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .value(maxDice)
                        .build())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("reroll_set")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value(rerollSet)
                        .build())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("botch_set")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value(botchSet)
                        .build())
                .addOption(ApplicationCommandInteractionOptionData.builder()
                        .name("reroll_variant")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .value(rerollVariant)
                        .build())
                .build(), null);
    }

    @Test
    void getStartOptionsValidationMessage() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,10", "1,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo(null);
    }

    @Test
    void getStartOptionsValidationMessage_botchSetZero() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,10", "0,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '0'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetNegative() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,10", "-1,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '-1'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetNotANumber() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,10", "1,a,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: 'a'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetEmpty() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,10", "1,,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: ''");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetZero() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "0,0,9,10", "1,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '0'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetNegative() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "-9,-10", "1,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '-9', '-10'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetNotANumber() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9a,asfd,..,10", "1,2,3", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: '..', '9a', 'asfd'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetEmpty() {
        ApplicationCommandInteractionOption option = createCommandInteractionOption("10", "12", "9,,,,10", "1", "ask");
        String res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEqualTo("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: ''");
    }
}