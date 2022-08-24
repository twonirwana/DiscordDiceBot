package de.janno.discord.bot.command.countSuccesses;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.EmptyData;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CountSuccessesCommandTest {

    CountSuccessesCommand underTest;
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    @BeforeEach
    void setup() {
        underTest = new CountSuccessesCommand(messageDataDAO, new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
    }

    @Test
    void getCommandDescription() {
        assertThat(underTest.getCommandDescription()).isEqualTo("Configure buttons for dice, with the same side, that counts successes against a target number");
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("count_successes");
    }


    @Test
    void getButtonMessage_noGlitch() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "no_glitch", 15);
        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getButtonMessageWithState_noGlitch() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "no_glitch", 15);
        State<EmptyData> state = new State<>("6", new EmptyData());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessageWithState_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15);
        State<EmptyData> state = new State<>("6", new EmptyData());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessageWithState_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15);
        State<EmptyData> state = new State<>("6", new EmptyData());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessageWithState_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15);
        State<EmptyData> state = new State<>("6", new EmptyData());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getConfigFromEvent_legacyOnlyTwo() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent_legacyOnlyThree() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,1,6,6,no_glitch");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015\u0000");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent_withTarget() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015\u0000123");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesConfig(123L, 6, 6, "no_glitch", 15));
    }

    @Test
    void getConfigFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00001\u00006\u00006\u0000no_glitch\u000015");
        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15));
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
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15), new State<>("6", new EmptyData())).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1");
        assertThat(results.getDescription()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15), new State<>("6", new EmptyData())).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 - Glitch!");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15), new State<>("8", new EmptyData())).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("8d6 = 3");
        assertThat(results.getDescription()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "count_ones", 15), new State<>("6", new EmptyData())).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = 1 successes and 4 ones");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        EmbedDefinition results = underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15), new State<>("6", new EmptyData())).orElseThrow();

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 = -3");
        assertThat(results.getDescription()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 -1s = -3");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("dice_sides", "target_number", "glitch", "max_dice");
    }


    @Test
    void getStateFromEvent_legacyV1() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes,4");

        State<EmptyData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("4", new EmptyData()));
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("count_successes\u00004");

        State<EmptyData> res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new State<>("4", new EmptyData()));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("10");

        assertThat(res).isEqualTo("count_successes\u001e10");
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
        when(buttonEventAdaptor.deleteMessage(anyLong(),anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...", null);
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(1L, false);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())), eq(null));
        //todo check persistance
        verify(buttonEventAdaptor, times(4)).getCustomId();
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
        when(buttonEventAdaptor.deleteMessage(anyLong(),anyBoolean())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click to roll the dice against 4 and check for more then half of dice 1s"), anyList());
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("6d6 = 2 - Glitch!",
                "[**1**,**1**,**1**,**1**,**5**,**6**] ≥4 = 2 and more then half of all dice show 1s", ImmutableList.of())), eq(null));
        //todo check persistance
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void getButtonLayoutWithState() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CountSuccessesConfig(null, 6, 6, "count_ones", 15), new State<>("6", new EmptyData()))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes1",
                        "count_successes2",
                        "count_successes3",
                        "count_successes4",
                        "count_successes5",
                        "count_successes6",
                        "count_successes7",
                        "count_successes8",
                        "count_successes9",
                        "count_successes10",
                        "count_successes11",
                        "count_successes12",
                        "count_successes13",
                        "count_successes14",
                        "count_successes15");
    }

    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CountSuccessesConfig(null, 6, 6, "count_ones", 15)).
                getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes1",
                        "count_successes2",
                        "count_successes3",
                        "count_successes4",
                        "count_successes5",
                        "count_successes6",
                        "count_successes7",
                        "count_successes8",
                        "count_successes9",
                        "count_successes10",
                        "count_successes11",
                        "count_successes12",
                        "count_successes13",
                        "count_successes14",
                        "count_successes15");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new CountSuccessesConfig(null, 6, 6, "count_ones", 15), new State<>("6", new EmptyData()))).isEmpty();
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + this.getClass().getSimpleName(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        CountSuccessesConfig config = new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12);
        State<EmptyData> state = new State<>("5", new EmptyData());
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave.orElseThrow()).isEqualTo(loaded);
        ConfigAndState<CountSuccessesConfig, EmptyData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(state.getData());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1660644934298L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 6
                target: 5
                glitchOption: "no_glitch"
                maxNumberOfButtons: 12
                """, "None", null);


        ConfigAndState<CountSuccessesConfig, EmptyData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new EmptyData());
    }
}