package de.janno.discord.bot.command.countSuccesses;

import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.bot.persistance.MessageDataDTO;
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
    MessageDataDAO messageDataDAO = mock(MessageDataDAO.class);

    @BeforeEach
    void setup() {
        underTest = new CountSuccessesCommand(messageDataDAO, new DiceUtils(1, 1, 1, 1, 5, 6, 6, 6));
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
        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessage_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessage_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessage_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);

        assertThat(underTest.createNewButtonMessage(config).getContent()).isEqualTo("Click to roll the dice against 6 minus 1s");
    }

    @Test
    void getButtonMessageWithState_noGlitch() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6");
    }


    @Test
    void getButtonMessageWithState_halfDiceOne() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and check for more then half of dice 1s");
    }

    @Test
    void getButtonMessageWithState_countOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 and count the 1s");
    }

    @Test
    void getButtonMessageWithState_subtractOnes() {
        CountSuccessesConfig config = new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none);
        State<StateData> state = new State<>("6", StateData.empty());

        assertThat(underTest.createNewButtonMessageWithState(config, state).map(MessageDefinition::getContent)).contains("Click to roll the dice against 6 minus 1s");
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
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "no_glitch", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[1,1,1,1,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_halfDiceOne_glitch() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1 - Glitch!");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1 and more then half of all dice show 1s");
    }

    @Test
    void rollDice_halfDiceOne_noGlitch() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "half_dice_one", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("8", StateData.empty()))
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("8d6 ⇒ 3");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[1,1,1,1,5,**6**,**6**,**6**] ≥6 = 3");
    }

    @Test
    void rollDice_countOnes() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))
                .orElseThrow());

        assertThat(results.getFields()).hasSize(0);
        assertThat(results.getTitle()).isEqualTo("6d6 ⇒ 1 successes and 4 ones");
        assertThat(results.getDescriptionOrContent()).isEqualTo("[**1**,**1**,**1**,**1**,5,**6**] ≥6 = 1");
    }

    @Test
    void rollDice_subtractOnes() {
        EmbedOrMessageDefinition results = RollAnswerConverter.toEmbedOrMessageDefinition(underTest.getAnswer(new CountSuccessesConfig(null, 6, 6, "subtract_ones", 15, 1, Set.of(), Set.of(1), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))
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
        List<ComponentRowDefinition> res = underTest.createNewButtonMessageWithState(new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none), new State<>("6", StateData.empty()))
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
        List<ComponentRowDefinition> res = underTest.createNewButtonMessage(new CountSuccessesConfig(null, 6, 6, "count_ones", 15, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none)).
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
        assertThat(res).isEqualTo(new CountSuccessesConfig(null, 12, 8, "half_dice_one", 13, 2, Set.of(12, 11), Set.of(1, 2), AnswerFormatType.full, ResultImage.alies_black_gold));
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
        Optional<String> res = underTest.getStartOptionsValidationMessage(option);
        assertThat(res).contains("The reroll set must be smaller then half the number of dice sides");
    }

    @Test
    void checkPersistence() {
        MessageDataDAO messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        UUID configUUID = UUID.randomUUID();
        CountSuccessesConfig config = new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.minimal, ResultImage.none);
        State<StateData> state = new State<>("5", StateData.empty());
        Optional<MessageDataDTO> toSave = underTest.createMessageDataForNewMessage(configUUID, 1L, channelId, messageId, config, state);
        messageDataDAO.saveMessageData(toSave.orElseThrow());

        MessageDataDTO loaded = messageDataDAO.getDataForMessage(channelId, messageId).orElseThrow();

        assertThat(toSave.orElseThrow()).isEqualTo(loaded);
        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(state.getData());
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
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
                """, "None", null);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.compact, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy2() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
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
                """, "None", null);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 2, Set.of(1, 2), Set.of(9, 10), AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void deserialization_legacy() {
        UUID configUUID = UUID.randomUUID();
        MessageDataDTO savedData = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "count_successes", "CountSuccessesConfig", """
                ---
                answerTargetChannelId: 123
                diceSides: 6
                target: 5
                glitchOption: "no_glitch"
                maxNumberOfButtons: 12
                """, "None", null);


        ConfigAndState<CountSuccessesConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12, 1, Set.of(), Set.of(), AnswerFormatType.full, ResultImage.none));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }
}