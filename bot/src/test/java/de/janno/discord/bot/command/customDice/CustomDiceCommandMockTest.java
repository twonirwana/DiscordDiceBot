package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.bot.persistance.PersistanceManagerImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomDiceCommandMockTest {
    PersistanceManager persistanceManager;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() throws IOException {
        FileUtils.cleanDirectory(new File("imageCache/"));
        messageIdCounter = new AtomicLong(0);
        persistanceManager = new PersistanceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @AfterEach
    void cleanUp() throws IOException {
        FileUtils.cleanDirectory(new File("imageCache/"));
    }

    @Test
    void legacy_id() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice\u00001d6\u0000");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "reply: The button uses an old format that isn't supported anymore. Please delete it and create a new button message with a slash command.");
    }


    @Test
    void roll_diceEvaluator_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_diceEvaluator_full_with_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.polyhedral_black_and_gold);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 3, description=1d6, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_diceEvaluator_full_with_images_d100() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d100")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.polyhedral_black_and_gold);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 73, description=1d100, fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_diceParser_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 1, description=1: [1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_diceEvaluator_compact() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**Dmg ⇒ 3**__  1d6: [3], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_diceEvaluator_minimal() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=Dmg ⇒ 3, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 0");
    }

    @Test
    void roll_pinned() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, true);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");

        assertThat(buttonEvent2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 4, description=1d6: [4], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessageById: 1",
                "getMessagesState: [0]");
    }

    @Test
    void roll_answerChannel() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:2, type:EMBED"
        );
    }

    @Test
    void roll_answerChannelTwice() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 100);
        CustomDiceCommand underTest = new CustomDiceCommand(persistanceManager, new DiceParser(), cachingDiceEvaluator);
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, ResultImage.none);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistanceManager, false);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        assertThat(buttonEvent1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:2, type:EMBED"
        );
        assertThat(buttonEvent2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 1, description=1d6: [1], fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
