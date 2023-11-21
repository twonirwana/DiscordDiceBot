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
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomParameterCommandMockTest {

    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_textLegacy() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("d['-','0','1']");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: d['-','0','1']+{bonus}\nPlease select value for **bonus**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=d['-','0','1']+4 ⇒ '1', 4, descriptionOrContent=['1'], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={dice}+{bonus}\nPlease select value for **dice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d4, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Fate, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void roll_textLegacy2() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{bonus:-5<=>5}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("2");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=2 ⇒ 2, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={bonus}\nPlease select value for **bonus**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=-5, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-4, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-2, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-1, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=0, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=1, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=5, id=custom_parameterid1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_text() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{dice:d4/d['-','0','1']@Fate}+{bonus:1<=>10}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id2");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: d['-','0','1']+{bonus}\nPlease select value for **bonus**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=d['-','0','1']+4 ⇒ '1', 4, descriptionOrContent=['1'], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={dice}+{bonus}\nPlease select value for **dice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d4, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Fate, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_full_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}@Roll\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Roll ⇒ 1, 1, 6, 3, descriptionOrContent=4d6: [1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}@Roll\nPlease select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_withoutExpression() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_withoutExpression_withLabel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}@Roll", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.without_expression, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Roll ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_compact() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3**__  4d6: [1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_minimal() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void lockedToUser_block() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4", "user1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3", "user2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id3", "user1");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:user1: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:user1: Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=numberOfDice: 4, sides: 6 ⇒ 1, 1, 6, 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void clear() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("clear");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10");
    }

    @Test
    void roll_pinned() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_answerChannel() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\n" +
                        "Please select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"

        );
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 3, 2, 4, 4, descriptionOrContent=[3, 2, 4, 4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nPlease select value for **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "getMessagesState: [0]"
        );
    }

    @Test
    void roll_answerChannelTwice() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        CustomParameterConfig config = new CustomParameterConfig(2L, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPlease select value for **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:{numberOfDice}d{sides}\nPlease select value for **numberOfDice**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,id9,id10",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 3, 2, 4, 4, descriptionOrContent=[3, 2, 4, 4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }
}
