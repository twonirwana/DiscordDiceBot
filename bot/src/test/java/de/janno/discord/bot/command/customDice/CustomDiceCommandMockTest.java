package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomDiceCommandMockTest {
    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_diceEvaluator_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 2, description=1d6: [2], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_diceParser_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1")), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 1, description=1: [1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_diceEvaluator_compact() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**Dmg ⇒ 2**__  1d6: [2], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_diceEvaluator_minimal() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=Dmg ⇒ 2, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_pinned() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 2, description=1d6: [2], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactly(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 2, description=1d6: [2], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");

        assertThat(buttonEvent2.getActions()).containsExactly(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_answerChannel() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 2, description=1d6: [2], fieldValues:, answerChannel:2, type:EMBED"
        );
    }

    @Test
    void roll_answerChannelTwice() {
        CustomDiceCommand underTest = new CustomDiceCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0), 1000);
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6")), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactly(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 2, description=1d6: [2], fieldValues:, answerChannel:2, type:EMBED"
        );
        assertThat(buttonEvent2.getActions()).containsExactly(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=Dmg ⇒ 3, description=1d6: [3], fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
