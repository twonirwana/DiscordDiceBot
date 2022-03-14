package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.CountSuccessesCommand;
import de.janno.discord.connector.api.*;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceUtils;
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
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "no_glitch", 15);
        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15);

        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "count_ones", 15);

        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15);

        assertThat(underTest.getButtonMessage(config)).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getButtonMessageWithState_noGlitch() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "no_glitch", 15);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.getButtonMessageWithState(state, config)).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessageWithState_halfDiceOne() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.getButtonMessageWithState(state, config)).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessageWithState_countOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "count_ones", 15);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.getButtonMessageWithState(state, config)).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessageWithState_subtractOnes() {
        CountSuccessesCommand.Config config = new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15);
        CountSuccessesCommand.State state = new CountSuccessesCommand.State(6);

        assertThat(underTest.getButtonMessageWithState(state, config)).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getConfigFromEvent_legacyOnlyTwo() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent_legacyOnlyThree() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch,15");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));
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
        Answer results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "no_glitch", 15));

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1");
        assertThat(results.getContent()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        Answer results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15));

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 - Glitch!");
        assertThat(results.getContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        Answer results = underTest.getAnswer(new CountSuccessesCommand.State(8), new CountSuccessesCommand.Config(6, 6, "half_dice_one", 15));

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("8d6 = 3");
        assertThat(results.getContent()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        Answer results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15));

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 successes and 4 ones");
        assertThat(results.getContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        Answer results = underTest.getAnswer(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "subtract_ones", 15));

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = -3");
        assertThat(results.getContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 -1s = -3");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("dice_sides", "target_number", "glitch", "max_dice");
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
        String res = underTest.createButtonCustomId("10", new CountSuccessesCommand.Config(6, 4, "half_dice_one", 12));

        assertThat(res).isEqualTo("count_successes,10,6,4,half_dice_one,12");
    }

    @Test
    void handleComponentInteractEvent() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("count_successes,6,6,4,half_dice_one,12");
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
                eq("Click to roll the dice against 4 and check for more then half of dice 1s"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, -259414907)));
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("count_successes,6,6,4,half_dice_one,12");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click to roll the dice against 4 and check for more then half of dice 1s");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click to roll the dice against 4 and check for more then half of dice 1s"),
                any()
        );
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, -259414907)));
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.getButtonLayoutWithState(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15));

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes,1,6,6,count_ones,15", "count_successes,2,6,6,count_ones,15", "count_successes,3,6,6,count_ones,15", "count_successes,4,6,6,count_ones,15", "count_successes,5,6,6,count_ones,15", "count_successes,6,6,6,count_ones,15", "count_successes,7,6,6,count_ones,15", "count_successes,8,6,6,count_ones,15", "count_successes,9,6,6,count_ones,15", "count_successes,10,6,6,count_ones,15", "count_successes,11,6,6,count_ones,15", "count_successes,12,6,6,count_ones,15", "count_successes,13,6,6,count_ones,15", "count_successes,14,6,6,count_ones,15", "count_successes,15,6,6,count_ones,15");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.getButtonLayout(new CountSuccessesCommand.Config(6, 6, "count_ones", 15));

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes,1,6,6,count_ones,15", "count_successes,2,6,6,count_ones,15", "count_successes,3,6,6,count_ones,15", "count_successes,4,6,6,count_ones,15", "count_successes,5,6,6,count_ones,15", "count_successes,6,6,6,count_ones,15", "count_successes,7,6,6,count_ones,15", "count_successes,8,6,6,count_ones,15", "count_successes,9,6,6,count_ones,15", "count_successes,10,6,6,count_ones,15", "count_successes,11,6,6,count_ones,15", "count_successes,12,6,6,count_ones,15", "count_successes,13,6,6,count_ones,15", "count_successes,14,6,6,count_ones,15", "count_successes,15,6,6,count_ones,15");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getEditButtonMessage(new CountSuccessesCommand.State(6), new CountSuccessesCommand.Config(6, 6, "count_ones", 15))).isNull();
    }
}