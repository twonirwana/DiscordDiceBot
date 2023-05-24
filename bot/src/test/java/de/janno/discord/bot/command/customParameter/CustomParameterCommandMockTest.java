package de.janno.discord.bot.command.customParameter;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomParameterCommandMockTest {

    PersistenceManager persistenceManager;
    AtomicLong messageIdCounter;

    @BeforeEach
    void setup() {
        messageIdCounter = new AtomicLong(0);
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void roll_textLegacy() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("d['-','0','1']");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: d['-','0','1']+{bonus}\nPlease select value for **bonus**, buttonValues=1,2,3,4,5,6,7,8,9,10,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=d['-','0','1']+4 ⇒ '1', 4, description=['1'], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={dice}+{bonus}\nPlease select value for **dice**, buttonValues=1,2",
                "deleteMessageById: 0");
    }

    @Test
    void roll_text() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: d['-','0','1']+{bonus}\nPlease select value for **bonus**, buttonValues=1,2,3,4,5,6,7,8,9,10,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=d['-','0','1']+4 ⇒ '1', 4, description=['1'], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={dice}+{bonus}\nPlease select value for **dice**, buttonValues=1,2",
                "deleteMessageById: 0");
    }

    @Test
    void roll_full_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}@Roll\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Roll ⇒ 1, 1, 6, 3, description=4d6: [1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={numberOfDice}d{sides}@Roll\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void roll_withoutExpression() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Please select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void roll_withoutExpression_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=Roll ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content=Please select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void roll_compact() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=__**numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3**__  4d6: [1, 1, 6, 3], fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Please select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void roll_minimal() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Please select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void lockedToUser_block() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4", "user1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3", "user2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("3", "user1");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:user1: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:user1: Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=null, description=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, fieldValues:, answerChannel:null, type:MESSAGE",
                "createButtonMessage: content=Please select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 0");
    }

    @Test
    void clear() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_pinned() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10");
    }

    @Test
    void roll_answerChannel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\n" +
                        "Please select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:2, type:EMBED"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createAnswer: title=4d6 ⇒ 3, 2, 4, 4, description=[3, 2, 4, 4], fieldValues:, answerChannel:null, type:EMBED",
                "createButtonMessage: content={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "deleteMessageById: 1",
                "getMessagesState: [0]");
    }

    @Test
    void roll_answerChannelTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 1, 1, 6, 3, description=[1, 1, 6, 3], fieldValues:, answerChannel:2, type:EMBED"
        );
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=1,2,3,4,5,6,7,8,clear");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=1,2,3,4,5,6,7,8,9,10",
                "createAnswer: title=4d6 ⇒ 3, 2, 4, 4, description=[3, 2, 4, 4], fieldValues:, answerChannel:2, type:EMBED"
        );
    }
}
