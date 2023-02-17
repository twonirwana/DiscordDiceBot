package de.janno.discord.bot.command.poolTarget;

import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.*;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PoolTargetCommandTest {

    PoolTargetCommand underTest;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);

    @BeforeEach
    void setup() {
        underTest = new PoolTargetCommand(persistenceManager, new DiceUtils(1, 1, 1, 2, 5, 6, 6, 6, 2, 10, 10, 2, 3, 4, 5, 6, 7, 8));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

    }

    @Test
    void getName() {
        String res = underTest.getCommandId();
        assertThat(res).isEqualTo("pool_target");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("sides", "max_dice", "reroll_set", "botch_set", "reroll_variant");
    }

    @Test
    void getDiceResult_withoutReroll() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new PoolTargetConfig(null, 6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask", AnswerFormatType.full, ResultImage.none), new State<>("6", new PoolTargetStateData(6, 3, false)))
                .orElseThrow());
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ≥3 ⇒ -1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,2,**5**,**6**]");
    }

    @Test
    void getDiceResult_withReroll() {
        EmbedOrMessageDefinition res = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new PoolTargetConfig(null, 6, 15, ImmutableSet.of(6), ImmutableSet.of(1), "ask", AnswerFormatType.full, ResultImage.none), new State<>("6", new PoolTargetStateData(6, 3, true)))
                .orElseThrow());
        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("6d6 ≥3 ⇒ 1");
        assertThat(res.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,2,2,**5**,**6**,**6**,**6**]");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("pool_target\u000015,10,15,10,1,ask,EMPTY,EMPTY")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("pool_targe")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("pool_target1")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("pool_targe1")).isFalse();
    }

    @Test
    void getAnswer_allStateInfoAvailable() {
        assertThat(underTest.getAnswer(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, 8, true)))
        ).isNotEmpty();
    }

    @Test
    void getAnswer_dicePoolMissing() {
        assertThat(underTest.getAnswer(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always", AnswerFormatType.full, ResultImage.none), new State<>("clear", new PoolTargetStateData(null, 8, true)))
        ).isEmpty();
    }

    @Test
    void getAnswer_targetNumberMissing() {
        assertThat(underTest.getAnswer(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, null, true)))
        ).isEmpty();
    }

    @Test
    void getAnswer_doRerollMissing() {
        assertThat(underTest.getAnswer(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "always", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, 8, null)))
        ).isEmpty();
    }

    @Test
    void getButtonMessage_rerollBotchEmpty() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(), "ask", AnswerFormatType.full, ResultImage.none))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice");
    }

    @Test
    void getButtonMessage_rerollEmpty() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with botch:1,2");
    }

    @Test
    void getButtonMessage_botchEmpty() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(), "ask", AnswerFormatType.full, ResultImage.none))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10");
    }

    @Test
    void getButtonMessage_ask() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getButtonMessage_always() {
        String res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "always", AnswerFormatType.full, ResultImage.none))
                .getContent();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with always reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageContentChange_poolWasSet() {
        String res = underTest.getCurrentMessageContentChange(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, null, null)))
                .orElseThrow();

        assertThat(res).isEqualTo("Click on the target to roll 10d10 against it, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageContentChange_targetWasSet() {
        String res = underTest.getCurrentMessageContentChange(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, 10, null)))
                .orElseThrow();

        assertThat(res).isEqualTo("Should 10s,9s in 10d10 against 10 be be rerolled?");
    }

    @Test
    void getCurrentMessageContentChange_clear() {
        String res = underTest.getCurrentMessageContentChange(new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("clear", new PoolTargetStateData(null, null, null)))
                .orElseThrow();

        assertThat(res).isEqualTo("Click on the buttons to roll dice, with ask reroll:9,10 and botch:1,2");
    }

    @Test
    void getCurrentMessageComponentChange_missingDoReroll_askForReroll() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, 10, null)))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("Reroll", "No reroll");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_targetdo_reroll00000000-0000-0000-0000-000000000000", "pool_targetno_reroll00000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayoutWithState_statesAreGiven_newButtons() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, 10, true)), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target100000000-0000-0000-0000-000000000000",
                        "pool_target200000000-0000-0000-0000-000000000000",
                        "pool_target300000000-0000-0000-0000-000000000000",
                        "pool_target400000000-0000-0000-0000-000000000000",
                        "pool_target500000000-0000-0000-0000-000000000000",
                        "pool_target600000000-0000-0000-0000-000000000000",
                        "pool_target700000000-0000-0000-0000-000000000000",
                        "pool_target800000000-0000-0000-0000-000000000000",
                        "pool_target900000000-0000-0000-0000-000000000000",
                        "pool_target1000000000-0000-0000-0000-000000000000",
                        "pool_target1100000000-0000-0000-0000-000000000000",
                        "pool_target1200000000-0000-0000-0000-000000000000",
                        "pool_target1300000000-0000-0000-0000-000000000000",
                        "pool_target1400000000-0000-0000-0000-000000000000",
                        "pool_target1500000000-0000-0000-0000-000000000000",
                        "pool_target1600000000-0000-0000-0000-000000000000",
                        "pool_target1700000000-0000-0000-0000-000000000000",
                        "pool_target1800000000-0000-0000-0000-000000000000",
                        "pool_target1900000000-0000-0000-0000-000000000000",
                        "pool_target2000000000-0000-0000-0000-000000000000");
    }

    @Test
    void getCurrentMessageComponentChange_missingTarget_askTarget() {
        List<ComponentRowDefinition> res = underTest.getCurrentMessageComponentChange(UUID.fromString("00000000-0000-0000-0000-000000000000"), new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none), new State<>("10", new PoolTargetStateData(10, null, null)))
                .orElseThrow();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("2", "3", "4", "5", "6", "7", "8", "9", "10", "Clear");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target200000000-0000-0000-0000-000000000000",
                        "pool_target300000000-0000-0000-0000-000000000000",
                        "pool_target400000000-0000-0000-0000-000000000000",
                        "pool_target500000000-0000-0000-0000-000000000000",
                        "pool_target600000000-0000-0000-0000-000000000000",
                        "pool_target700000000-0000-0000-0000-000000000000",
                        "pool_target800000000-0000-0000-0000-000000000000",
                        "pool_target900000000-0000-0000-0000-000000000000",
                        "pool_target1000000000-0000-0000-0000-000000000000",
                        "pool_targetclear00000000-0000-0000-0000-000000000000");
    }

    @Test
    void createNewButtonMessage() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target100000000-0000-0000-0000-000000000000",
                        "pool_target200000000-0000-0000-0000-000000000000",
                        "pool_target300000000-0000-0000-0000-000000000000",
                        "pool_target400000000-0000-0000-0000-000000000000",
                        "pool_target500000000-0000-0000-0000-000000000000",
                        "pool_target600000000-0000-0000-0000-000000000000",
                        "pool_target700000000-0000-0000-0000-000000000000",
                        "pool_target800000000-0000-0000-0000-000000000000",
                        "pool_target900000000-0000-0000-0000-000000000000",
                        "pool_target1000000000-0000-0000-0000-000000000000",
                        "pool_target1100000000-0000-0000-0000-000000000000",
                        "pool_target1200000000-0000-0000-0000-000000000000",
                        "pool_target1300000000-0000-0000-0000-000000000000",
                        "pool_target1400000000-0000-0000-0000-000000000000",
                        "pool_target1500000000-0000-0000-0000-000000000000",
                        "pool_target1600000000-0000-0000-0000-000000000000",
                        "pool_target1700000000-0000-0000-0000-000000000000",
                        "pool_target1800000000-0000-0000-0000-000000000000",
                        "pool_target1900000000-0000-0000-0000-000000000000",
                        "pool_target2000000000-0000-0000-0000-000000000000");
    }


    @Test
    void getButtonLayout() {
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                        new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none))
                .getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10", "16d10", "17d10", "18d10", "19d10", "20d10");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("pool_target100000000-0000-0000-0000-000000000000",
                        "pool_target200000000-0000-0000-0000-000000000000",
                        "pool_target300000000-0000-0000-0000-000000000000",
                        "pool_target400000000-0000-0000-0000-000000000000",
                        "pool_target500000000-0000-0000-0000-000000000000",
                        "pool_target600000000-0000-0000-0000-000000000000",
                        "pool_target700000000-0000-0000-0000-000000000000",
                        "pool_target800000000-0000-0000-0000-000000000000",
                        "pool_target900000000-0000-0000-0000-000000000000",
                        "pool_target1000000000-0000-0000-0000-000000000000",
                        "pool_target1100000000-0000-0000-0000-000000000000",
                        "pool_target1200000000-0000-0000-0000-000000000000",
                        "pool_target1300000000-0000-0000-0000-000000000000",
                        "pool_target1400000000-0000-0000-0000-000000000000",
                        "pool_target1500000000-0000-0000-0000-000000000000",
                        "pool_target1600000000-0000-0000-0000-000000000000",
                        "pool_target1700000000-0000-0000-0000-000000000000",
                        "pool_target1800000000-0000-0000-0000-000000000000",
                        "pool_target1900000000-0000-0000-0000-000000000000",
                        "pool_target2000000000-0000-0000-0000-000000000000");
    }

    @Test
    void validate_valid() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none));

        assertThat(res).isEmpty();
    }

    @Test
    void validate_numberInRerollSetToBig() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9, 12), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none));

        assertThat(res).contains("Reroll set [10, 9, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_numberInBotSetToBig() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9), ImmutableSet.of(1, 2, 12), "ask", AnswerFormatType.full, ResultImage.none));

        assertThat(res).contains("Botch set [1, 2, 12] contains a number bigger then the sides of the die 10");
    }

    @Test
    void validate_toManyNumberInRerollSet() {
        Optional<String> res = underTest.validate(
                new PoolTargetConfig(null, 10, 20, ImmutableSet.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), ImmutableSet.of(1, 2), "ask", AnswerFormatType.full, ResultImage.none));

        assertThat(res).contains("The reroll must not contain all numbers");
    }

    @Test
    void handleComponentInteractEvent_missingDBEntriy() {
        ButtonEventAdaptor buttonEventAdaptor = mock(ButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("pool_target8");
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any(), eq(null))).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));
        when(buttonEventAdaptor.getInvokingGuildMemberName()).thenReturn("testUser");
        when(persistenceManager.getMessageData(1L, 1L)).thenReturn(Optional.empty());


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        assertThat(res).isNotNull();
        verify(buttonEventAdaptor).reply("Configuration for the message is missing, please create a new message with the slash command `/pool_target start`", false);
        verify(buttonEventAdaptor, never()).editMessage(anyString(), anyList());
        verify(buttonEventAdaptor, never()).createButtonMessage(any());
        verify(buttonEventAdaptor, never()).deleteMessageById(anyLong());
        verify(buttonEventAdaptor, never()).createResultMessageWithEventReference(any(), any());
        verify(buttonEventAdaptor, times(3)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor, never()).isPinned();
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
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new PoolTargetCommand(persistenceManager, mock(DiceUtils.class));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        PoolTargetConfig config = new PoolTargetConfig(123L, 10, 12, ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), "ask", AnswerFormatType.full, ResultImage.none);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        PoolTargetStateData stateData = new PoolTargetStateData(5, null, null);

        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "pool_target", "PoolTargetStateData", Mapper.serializedObject(stateData));
        ConfigAndState<PoolTargetConfig, PoolTargetStateData> configAndState = underTest.deserializeAndUpdateState(loaded, messageDataDTO, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new PoolTargetStateData(5, 3, null));
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "pool_target", "PoolTargetConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 10
                maxNumberOfButtons: 12
                rerollSet:
                - 7
                - 8
                - 9
                - 10
                botchSet:
                - 1
                rerollVariant: "ask"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "pool_target",
                "PoolTargetStateData", """
                ---
                dicePool: 5
                targetNumber: null
                doReroll: null
                """);

        ConfigAndState<PoolTargetConfig, PoolTargetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new PoolTargetConfig(123L, 10, 12, ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), "ask", AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new PoolTargetStateData(5, 3, null));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "pool_target", "PoolTargetConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 10
                maxNumberOfButtons: 12
                rerollSet:
                - 7
                - 8
                - 9
                - 10
                botchSet:
                - 1
                rerollVariant: "ask"
                answerFormatType: compact
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "pool_target",
                "PoolTargetStateData", """
                ---
                dicePool: 5
                targetNumber: null
                doReroll: null
                """);

        ConfigAndState<PoolTargetConfig, PoolTargetStateData> configAndState = underTest.deserializeAndUpdateState(messageConfigDTO, messageDataDTO, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new PoolTargetConfig(123L, 10, 12, ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), "ask", AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new PoolTargetStateData(5, 3, null));
    }

}