package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomDiceCommandMockTest {
    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;
    CustomDiceCommand underTest;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new CustomDiceCommand(messageDataDAO);
    }

    @Test
    void roll() {
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "5", "5")), DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessage: 0");
    }

    @Test
    void roll_pinned() {
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "5", "5")), DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");
    }

    @Test
    void roll_pinnedTwice() {
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "5", "5")), DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button");

        assertThat(buttonEvent2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:null",
                "createButtonMessage: content=Click on a button to roll the dice, buttonValues=1_button",
                "deleteMessage: 1");
    }

    @Test
    void roll_answerChannel() {
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "5", "5")), DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:2");
    }

    @Test
    void roll_answerChannelTwice() {
        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "5", "5")), DiceParserSystem.DICE_EVALUATOR);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:2");
        assertThat(buttonEvent2.getActions()).containsExactly(
                "acknowledge",
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createAnswer: title=5 ⇒ 5, description=[], fieldValues:, answerChannel:2");
    }
}
