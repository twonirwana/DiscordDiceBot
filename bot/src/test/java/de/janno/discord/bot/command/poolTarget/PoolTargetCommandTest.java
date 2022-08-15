package de.janno.discord.bot.command.poolTarget;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.EmptyData;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PoolTargetCommandTest {

    PoolTargetCommand underTest;

    private static Stream<Arguments> getStateFromEvent() {
        return Stream.of(
                //set pool
                Arguments.of("pool_target\u000015\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000", new State<>("15", new PoolTargetStateData(15, null, null))),
                Arguments.of("pool_target\u000015,10,20,10;9,1;2,always,EMPTY,EMPTY", new State<>("15", new PoolTargetStateData(15, null, null))),

                //set target
                Arguments.of("pool_target\u00008,10,20,10;9,1;2,ask,15,EMPTY", new State<>("8", new PoolTargetStateData(15, 8, null))),
                Arguments.of("pool_target\u00008,10,20,10;9,1;2,always,15,EMPTY", new State<>("8", new PoolTargetStateData(15, 8, true))),

                //clear
                Arguments.of("pool_target\u0000clear,10,20,10;9,1;2,ask,15,EMPTY", new State<>("clear", new PoolTargetStateData(null, null, null))),
                Arguments.of("pool_target\u0000clear,10,20,10;9,1;2,always,15,EMPTY", new State<>("clear", new PoolTargetStateData(null, null, null))),

                //ask reroll
                Arguments.of("pool_target\u0000do_reroll,10,20,10;9,1;2,ask,15,9", new State<>("do_reroll", new PoolTargetStateData(15, 9, true))),
                Arguments.of("pool_target\u0000no_reroll,10,20,10;9,1;2,ask,15,9", new State<>("no_reroll", new PoolTargetStateData(15, 9, false)))
        );
    }

    @BeforeEach
    void setup() {
        underTest = new PoolTargetCommand(mock(MessageDataDAO.class), new DiceUtils(1, 1, 1, 2, 5, 6, 6, 6, 2, 10, 10, 2, 3, 4, 5, 6, 7, 8));
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("getStateFromEvent")
    void getStateFromEvent(String customButtonId, State<PoolTargetStateData> expected) {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn(customButtonId);
        assertThat(underTest.getStateFromEvent(buttonEventAdaptor)).isEqualTo(expected);
    }

    @Test
    void getName() {
        String res = underTest.getCommandId();
        assertThat(res).isEqualTo("pool_target");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("sides", "max_dice", "reroll_set", "botch_set", "reroll_variant", "target_channel");
    }

    @Test
    void getDiceResult_withoutReroll() {
        EmbedDefinition res = underTest.getAnswer(new State<>("6", new PoolTargetStateData(6, 3, false)),
                new PoolTargetConfig(null, 6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask")).orElseThrow();
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 = -1");
        assertThat(res.getDescription()).isEqualTo("[**1**,**1**,**1**,2,**5**,**6**] ≥3 = -1");
    }

    @Test
    void getDiceResult_withReroll() {
        EmbedDefinition res = underTest.getAnswer(new State<>("6", new PoolTargetStateData(6, 3, true)),
                new PoolTargetConfig(null, 6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask")).orElseThrow();
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 = 1");
        assertThat(res.getDescription()).isEqualTo("[**1**,**1**,**1**,2,2,**5**,**6**,**6**,**6**] ≥3 = 1");
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("pool_target\u0000do_reroll\u000010\u000020\u000010;9\u00001;2\u0000ask\u000015\u00009\u0000");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new PoolTargetConfig(
                null,
                10,
                20,
                ImmutableSet.of(9, 10),
                ImmutableSet.of(1, 2),
                "ask"));
    }

    @Test
    void getConfigFromEvent_target() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("pool_target\u0000do_reroll\u000010\u000020\u000010;9\u00001;2\u0000ask\u000015\u00009\u0000123");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new PoolTargetConfig(
                123L,
                10,
                20,
                ImmutableSet.of(9, 10),
                ImmutableSet.of(1, 2),
                "ask"));
    }

    @Test
    void getConfigFromEvent_legacy() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("pool_target\u0000do_reroll\u000010\u000020\u000010;9\u00001;2\u0000ask\u000015\u00009");

        assertThat(underTest.getConfigFromEvent(event)).isEqualTo(new PoolTargetConfig(
                null,
                10,
                20,
                ImmutableSet.of(9, 10),
                ImmutableSet.of(1, 2),
                "ask"));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("pool_target\u000015,10,15,10,1,ask,EMPTY,EMPTY")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("pool_targe")).isFalse();
    }

    @Test
    void getAnswer_allStateInfoAvailable() {
        assertThat(underTest.getAnswer(
                new State<>("10", new PoolTargetStateData(10, 8, true)),
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isNotEmpty();
    }

    @Test
    void getAnswer_dicePoolMissing() {
        assertThat(underTest.getAnswer(
                new State<>("clear", new PoolTargetStateData(null, 8, true)),
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isEmpty();
    }

    @Test
    void getAnswer_targetNumberMissing() {
        assertThat(underTest.getAnswer(
                new State<>("10", new PoolTargetStateData(10, null, true)),
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isEmpty();
    }

    @Test
    void getAnswer_doRerollMissing() {
        assertThat(underTest.getAnswer(
                new State<>("10", new PoolTargetStateData(10, 8, null)),
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always"))
        ).isEmpty();
    }

    @Test
    void getButtonMessage_rerollBotchEmpty() {
        String res = underTest.createNewButtonMessage(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "ask"))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_rerollEmpty() {
        String res = underTest.createNewButtonMessage(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(1, 2), "ask"))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with botch:1,2");
    }

    @Test
    void getButtonMessage_botchEmpty() {
        String res = underTest.createNewButtonMessage(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(), "ask"))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10");
    }

    @Test
    void getButtonMessage_ask() {
        String res = underTest.createNewButtonMessage(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonMessage_always() {
        String res = underTest.createNewButtonMessage(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "always"))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with always reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageContentChange_poolWasSet() {
        String res = underTest.getCurrentMessageContentChange(
                        new State<>("10", new PoolTargetStateData(10, null, null)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow();

        assertThat(res).isEqualTo("Click on the target to roll 10d10 against it, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageContentChange_targetWasSet() {
        String res = underTest.getCurrentMessageContentChange(
                        new State<>("10", new PoolTargetStateData(10, 10, null)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow();

        assertThat(res).isEqualTo("Should 10s,9s in 10d10 against 10 be be rerolled?");
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        String res = underTest.getCurrentMessageContentChange(
                        new State<>("clear", new PoolTargetStateData(null, null, null)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageComponentChange_missingDoReroll_askForReroll() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(
                        new State<>("10", new PoolTargetStateData(10, 10, null)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("Reroll", "No reroll");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target\u0000do_reroll\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u000010\u0000",
                        "pool_target\u0000no_reroll\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u000010\u0000");
    }

    @Test
    void getButtonLayoutWithState_statesAreGiven_newButtons() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(
                        new State<>("10", new PoolTargetStateData(10, 10, true)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target\u00001\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00002\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00003\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00004\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00005\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00006\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00007\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00009\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000010\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000011\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000012\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000013\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000014\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000015\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000016\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000017\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000018\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000019\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000020\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
    }

    @Test
    void getCurrentMessageComponentChange_missingTarget_askTarget() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(
                        new State<>("10", new PoolTargetStateData(10, null, null)),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("2", "3", "4", "5", "6", "7", "8", "9", "10", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target\u00002\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00003\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00004\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00005\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00006\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00007\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u00009\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u000010\u000010\u000020\u000010;9\u00001;2\u0000ask\u000010\u0000EMPTY\u0000",
                        "pool_target\u0000clear\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
    }

    @Test
    void createNewButtonMessage() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target\u00001\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00002\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00003\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00004\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00005\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00006\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00007\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00009\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000010\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000011\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000012\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000013\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000014\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000015\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000016\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000017\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000018\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000019\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000020\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
    }


    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target\u00001\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00002\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00003\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00004\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00005\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00006\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00007\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u00009\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000010\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000011\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000012\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000013\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000014\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000015\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000016\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000017\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000018\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000019\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000",
                        "pool_target\u000020\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
    }

    @Test
    void validate_valid() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).isEmpty();
    }

    @Test
    void validate_numberInRerollSetToBig() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9, 12), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).contains("Reroll set [10, 9, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_numberInBotSetToBig() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2, 12), "ask"));

        assertThat(res).contains("Botch set [1, 2, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_toManyNumberInRerollSet() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), ImmutableSet.of(1, 2), "ask"));

        assertThat(res).contains("The reroll must not contain all numbers");
    }

    @Test
    void handleComponentInteractEvent_clear() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target\u0000clear\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage(eq("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2"), anyList());
        verify(buttonEventAdaptor, never()).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any(), eq(null));
        //todo check persistance
        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_setTargetAsk() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000ask\u000015\u0000EMPTY\u0000");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage(eq("Should 10s,9s in 15d10 against 8 be be rerolled?"), anyList());
        verify(buttonEventAdaptor, never()).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any(), eq(null));
        //todo check persistance
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_setTargetAlways() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target\u00008\u000010\u000020\u000010;9\u00001;2\u0000always\u000015\u0000EMPTY\u0000");
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

        verify(buttonEventAdaptor).editMessage(eq("processing ..."), anyList());
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("15d10 = -4",
                "[**1**,**1**,**1**,**2**,**2**,**2**,3,4,5,5,6,6,6,6,7,**10**,**10**] ≥8 = -4", ImmutableList.of())), eq(null));
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
    void handleComponentInteractEvent_setReroll() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target\u0000do_reroll,10,20,10;9,1;2,ask,15,8");
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

        verify(buttonEventAdaptor).editMessage(eq("processing ..."), anyList());
        verify(buttonEventAdaptor).createButtonMessage(any());
        verify(buttonEventAdaptor).deleteMessage(1L);
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new EmbedDefinition("15d10 = -4",
                "[**1**,**1**,**1**,**2**,**2**,**2**,3,4,5,5,6,6,6,6,7,**10**,**10**] ≥8 = -4", ImmutableList.of())), eq(null));
        //todo check persitance
        verify(buttonEventAdaptor, times(4)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }

    @Test
    void handleComponentInteractEvent_clearPinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target\u0000clear\u000010\u000020\u000010;9\u00001;2\u0000ask\u0000EMPTY\u0000EMPTY\u0000");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any(), any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.acknowledge()).thenReturn(Mono.just(mock(Void.class)));

        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage(eq("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2"), anyList());
        verify(buttonEventAdaptor, never()).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any(), eq(null));

        //todo check persistance
        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor, never()).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
        verify(buttonEventAdaptor).acknowledge();
    }


    private CommandInteractionOption createCommandInteractionOption(Long sides,
                                                                    Long maxDice,
                                                                    String rerollSet,
                                                                    String botchSet,
                                                                    String rerollVariant) {
        return CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("sides")
                        .longValue(sides)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("max_dice")
                        .longValue(maxDice)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("reroll_set")
                        .stringValue(rerollSet)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("botch_set")
                        .stringValue(botchSet)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("reroll_variant")
                        .stringValue(rerollVariant)
                        .build())
                .build();
    }

    @Test
    void getStartOptionsValidationMessage() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,10", "1,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).isEmpty();
    }

    @Test
    void getStartOptionsValidationMessage_botchSetZero() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,10", "0,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '0'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetNegative() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,10", "-1,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '-1'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetNotANumber() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,10", "1,a,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: 'a'");
    }

    @Test
    void getStartOptionsValidationMessage_botchSetEmpty() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,10", "1,,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: ''");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetZero() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "0,0,9,10", "1,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '0'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetNegative() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "-9,-10", "1,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers greater zero, seperated by ','. The following parameter where not greater zero: '-9', '-10'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetNotANumber() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9a,asfd,..,10", "1,2,3", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: '..', '9a', 'asfd'");
    }

    @Test
    void getStartOptionsValidationMessage_rerollSetEmpty() {
        CommandInteractionOption option = createCommandInteractionOption(10L, 12L, "9,,,,10", "1", "ask");
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);

        assertThat(res).contains("The parameter need to have numbers, seperated by ','. The following parameter where not numbers: ''");
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:file:./persistence/" + this.getClass().getSimpleName(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        MessageDataDTO toSave = underTest.createMessageDataForNewMessage(UUID.randomUUID(), channelId, messageId,
                new PoolTargetConfig(123L, 10, 12, ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), "ask"),
                new State<>("3", new PoolTargetStateData(5, 7, true)));

        messageDataDAO.saveMessageData(toSave);

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave).isEqualTo(loaded);
    }
}