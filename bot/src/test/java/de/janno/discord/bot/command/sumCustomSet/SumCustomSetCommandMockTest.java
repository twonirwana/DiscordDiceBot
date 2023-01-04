package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class SumCustomSetCommandMockTest {

    MessageDataDAO messageDataDAO;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        messageDataDAO = new MessageDataDAOImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6+2 ⇒ 3, description=[1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessageById: 0");
    }

    @Test
    void roll_compact() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.compact);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=null, description=__**+1d6+2 ⇒ 3**__  [1], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessageById: 0");
    }

    @Test
    void roll_minimal() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.minimal);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=null, description=+1d6+2 ⇒ 3, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessageById: 0");
    }

    @Test
    void clear() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
    }

    @Test
    void backBack() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("back");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
    }

    @Test
    void roll_pinned() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6+2 ⇒ 3, description=[1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
    }

    @Test
    void roll_answerChannel() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6+2 ⇒ 3, description=[1], fieldValues:, answerChannel:2, type:EMBED"
        );
    }

    @Test
    void roll_pinnedTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click4).block();
        ButtonEventAdaptorMock click5 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click5).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6+2 ⇒ 3, description=[1], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click5.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6 ⇒ 2, description=[2], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "deleteMessageById: 1",
                "getMessagesState: [0]");
    }

    @Test
    void roll_answerChannelTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(messageDataDAO, new DiceParser(), new RandomNumberSupplier(0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, messageDataDAO, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click4).block();
        ButtonEventAdaptorMock click5 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click5).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6+2 ⇒ 3, description=[1], fieldValues:, answerChannel:2, type:EMBED"
        );
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click5.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createAnswer: title=+1d6 ⇒ 2, description=[2], fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
