package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CountSuccessesCommandTest {

    CountSuccessesCommand underTest;

    @BeforeEach
    void setup() {
        underTest = new CountSuccessesCommand(new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getCommandDescription() {
        assertThat(underTest.getCommandDescription()).isEqualTo("Configure buttons for dice, with the same side, that counts successes against a target number");
    }

    @Test
    void getName() {
        assertThat(underTest.getName()).isEqualTo("count_successes");
    }


    @Test
    void getButtonMessage_noGlitch() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null);
        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15, null);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15, null);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getButtonMessageWithState_noGlitch() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.createNewButtonMessageWithState(state, config).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessageWithState_halfDiceOne() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15, null);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.createNewButtonMessageWithState(state, config).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessageWithState_countOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.createNewButtonMessageWithState(state, config).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessageWithState_subtractOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15, null);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.createNewButtonMessageWithState(state, config).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getConfigFromEvent_legacyOnlyTwo() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null));
    }

    @Test
    void getConfigFromEvent_legacyOnlyThree() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null));
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null));
    }

    @Test
    void getConfigFromEvent_withTarget() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, 123L));
    }

    @Test
    void getConfigFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("count_successes,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("count_successe")).isFalse();
    }

    @Test
    void rollDice() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "no_glitch", 15, null)).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1");
        assertThat(results.getDescription()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15, null)).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 - Glitch!");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesCommand.State(8), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15, null)).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("8d6 = 3");
        assertThat(results.getDescription()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null)).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 successes and 4 ones");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15, null)).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = -3");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 -1s = -3");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("dice_sides", "target_number", "glitch", "max_dice", "target_channel");
    }


    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,4");

        CountSuccessesCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new CountSuccessesCommand.State(4));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("10", new CountSuccessesCommand.Config(6, 4, "half_dice_one", 12, null));

        assertThat(res).isEqualTo("count_successes\u000010\u00006\u00004\u0000half_dice_one\u000012\u0000");
    }

    @Test
    void handleComponentInteractEvent() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("count_successes\u00006\u00006\u00004\u0000half_dice_one\u000012");
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


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...", null);
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())), eq(null));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 515715116)));
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("count_successes,6,6,4,half_dice_one,12");
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
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click to roll the dice against 4 and check for more then half of dice 1s", null);
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())), eq(null));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 515715116)));
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes\u00001\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00002\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00003\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00004\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00005\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00006\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00007\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00008\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00009\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000010\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000011\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000012\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000013\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000014\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000015\u00006\u00006\u0000count_ones\u000015\u0000");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null)).
                getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes\u00001\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00002\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00003\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00004\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00005\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00006\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00007\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00008\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u00009\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000010\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000011\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000012\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000013\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000014\u00006\u00006\u0000count_ones\u000015\u0000",
                        "count_successes\u000015\u00006\u00006\u0000count_ones\u000015\u0000");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15, null))).isEmpty();
    }
}