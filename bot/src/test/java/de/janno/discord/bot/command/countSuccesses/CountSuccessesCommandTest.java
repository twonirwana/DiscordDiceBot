package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CountSuccessesCommandTest {

    CountSuccessesCommand underTest;
    PersistenceManager persistenceManager = mock(PersistenceManager.class);

    @BeforeEach
    void setup() {
        underTest = new CountSuccessesCommand(persistenceManager, new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
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
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        assertThat(underTest.createNewButtonMessage(UUID.randomUUID(), config).getContent()).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(UUID.randomUUID(), config).getContent()).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(UUID.randomUUID(), config).getContent()).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(UUID.randomUUID(), config).getContent()).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getButtonMessageWithState_noGlitch() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(UUID.randomUUID(), config, state, 1L, 2L).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessageWithState_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(UUID.randomUUID(), config, state, 1L, 2L).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessageWithState_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(UUID.randomUUID(), config, state, 1L, 2L).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessageWithState_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(UUID.randomUUID(), config, state, 1L, 2L).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void matchingComponentCustomId_match_legacy() {
        assertThat(underTest.matchingComponentCustomId("count_successes,x")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch_legacy() {
        assertThat(underTest.matchingComponentCustomId("count_successe")).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("count_successes5")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("count_successes25")).isFalse();
    }

    @Test
    void rollDice() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1 - Glitch!");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("8", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("8d6 ⇒ 3");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1 successes and 4 ones");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()), 0, 0)
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ -3");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = -3, remove success for: [1]");
    }

    @Test
    void getStartOptions() {
        List<CommandDefinitionOption> res = underTest.getStartOptions();

        assertThat(res.stream().map(CommandDefinitionOption::getName)).containsExactly("dice_sides",
                "target_number",
                "glitch",
                "max_dice",
                "min_dice_count",
                "reroll_set",
                "botch_set");
    }

    @Test
    void getButtonLayoutWithState() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(uuid, new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()), 1L, 2L)
                .orElseThrow().getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes100000000-0000-0000-0000-000000000000",
                        "count_successes200000000-0000-0000-0000-000000000000",
                        "count_successes300000000-0000-0000-0000-000000000000",
                        "count_successes400000000-0000-0000-0000-000000000000",
                        "count_successes500000000-0000-0000-0000-000000000000",
                        "count_successes600000000-0000-0000-0000-000000000000",
                        "count_successes700000000-0000-0000-0000-000000000000",
                        "count_successes800000000-0000-0000-0000-000000000000",
                        "count_successes900000000-0000-0000-0000-000000000000",
                        "count_successes1000000000-0000-0000-0000-000000000000",
                        "count_successes1100000000-0000-0000-0000-000000000000",
                        "count_successes1200000000-0000-0000-0000-000000000000",
                        "count_successes1300000000-0000-0000-0000-000000000000",
                        "count_successes1400000000-0000-0000-0000-000000000000",
                        "count_successes1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getButtonLayout() {
        UUID uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(uuid, new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none)).
                getComponentRowDefinitions();

        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6");
        assertThat(res.stream().flatMap(l -> l.getButtonDefinitions().stream()).map(ButtonDefinition::getId))
                .containsExactly("count_successes100000000-0000-0000-0000-000000000000",
                        "count_successes200000000-0000-0000-0000-000000000000",
                        "count_successes300000000-0000-0000-0000-000000000000",
                        "count_successes400000000-0000-0000-0000-000000000000",
                        "count_successes500000000-0000-0000-0000-000000000000",
                        "count_successes600000000-0000-0000-0000-000000000000",
                        "count_successes700000000-0000-0000-0000-000000000000",
                        "count_successes800000000-0000-0000-0000-000000000000",
                        "count_successes900000000-0000-0000-0000-000000000000",
                        "count_successes1000000000-0000-0000-0000-000000000000",
                        "count_successes1100000000-0000-0000-0000-000000000000",
                        "count_successes1200000000-0000-0000-0000-000000000000",
                        "count_successes1300000000-0000-0000-0000-000000000000",
                        "count_successes1400000000-0000-0000-0000-000000000000",
                        "count_successes1500000000-0000-0000-0000-000000000000");
    }

    @Test
    void getCurrentMessageContentChange() {
        assertThat(underTest.getCurrentMessageContentChange(new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))).isEmpty();
    }

    @Test
    void getConfigValuesFromStartOptions() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("dice_sides")
                        .longValue(12L)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("target_number")
                        .longValue(8L)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("glitch")
                        .stringValue("half_dice_one")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("max_dice")
                        .longValue(13L)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("min_dice_count")
                        .longValue(2L)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("reroll_set")
                        .stringValue("11, 12")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("botch_set")
                        .stringValue("1,2")
                        .build())
                .build();
        CountSuccessesConfig res = underTest.getConfigFromStartOptions(option);
        assertThat(res).isEqualTo(new CountSuccessesConfig(null, 12, 8, "half_dice_one", 13, 2, Set.of(12, 11), Set.of(1, 2), AnswerFormatType.full, ResultImage.polyhedral_3d_red_and_white));
    }


    @Test
    void getStartOptionsValidationMessage() {
        CommandInteractionOption option = CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("dice_sides")
                        .longValue(5L)
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("reroll_set")
                        .stringValue("1,2,3")
                        .build())
                .build();
        Optional<String> res = underTest.getStartOptionsValidationMessage(option, 0L, 0L);
        assertThat(res).contains("The reroll set must be smaller then half the number of dice sides");
    }

    @Test
    void checkConfigPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);

        UUID configUUID = UUID.randomUUID();
        CountSuccessesConfig config = new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.minimal, ResultImage.none);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState()).isEqualTo(new State<>("3", StateData.empty()));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 6
                target: 5
                glitchOption: "no_glitch"
                maxNumberOfButtons: 12
                minDiceCount: 2
                rerollSet:
                - 2
                - 1
                botchSet:
                - 10
                - 9
                answerFormatType: compact
                """);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 6
                target: 5
                glitchOption: "no_glitch"
                maxNumberOfButtons: 12
                minDiceCount: 2
                rerollSet:
                - 2
                - 1
                botchSet:
                - 10
                - 9
                """);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 6
                target: 5
                glitchOption: "no_glitch"
                maxNumberOfButtons: 12
                """);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }
}