package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class SumCustomSetCommandMockTest {

    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_full() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }


    @Test
    void roll_fullGerman() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "editMessage: message:Klick auf einen Button um die Würfel hinzuzufügen und dann auf Würfeln, buttonValues=1_button,2_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Klick auf einen Button um die Würfel hinzuzufügen und dann auf Würfeln, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Würfeln, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Löschen, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Zurück, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"        );
    }

    @Test
    void roll_compact() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**+1d6+2 ⇒ 3**__  [1], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_minimal() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=+1d6+2 ⇒ 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_locked() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("2_button", "user2");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("2_button");
        underTest.handleComponentInteractEvent(click3).block();
        ButtonEventAdaptorMock click4 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click4).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click3.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6+2, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=+1d6+2 ⇒ 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void clear() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, true);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_answerChannel() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void roll_pinnedTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, true);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click5.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6 ⇒ 4, descriptionOrContent=[4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=bonus, id=sum_custom_set2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "getMessagesState: [0]");
    }

    @Test
    void roll_answerChannelTwice() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        SumCustomSetConfig config = new SumCustomSetConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "+1d6"),
                new ButtonIdLabelAndDiceExpression("2_button", "bonus", "+2")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);

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
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6+2 ⇒ 3, descriptionOrContent=[1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
        assertThat(click4.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: +1d6, buttonValues=1_button,2_button,roll,clear,back");
        assertThat(click5.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,2_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=+1d6 ⇒ 4, descriptionOrContent=[4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void channelAlias() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "att")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: att, buttonValues=1_button,roll,clear,back"
        );
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=2d20+10 ⇒ 31, descriptionOrContent=[11, 10], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Attack, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void userChannelAlias() {
        SumCustomSetCommand underTest = new SumCustomSetCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 10000));
        underTest.setMessageDataDeleteDuration(Duration.ofMillis(10));

        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("user_channel_alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        SumCustomSetConfig config = new SumCustomSetConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Attack", "att")), DiceParserSystem.DICE_EVALUATOR, true, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<SumCustomSetConfig, SumCustomSetStateData> factory = new ButtonEventAdaptorMockFactory<>("sum_custom_st", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: att, buttonValues=1_button,roll,clear,back"
        );
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click the buttons to add dice to the set and then on Roll, buttonValues=1_button,roll,clear,back",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=2d20+10 ⇒ 31, descriptionOrContent=[11, 10], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click the buttons to add dice to the set and then on Roll, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Attack, id=sum_custom_set1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Roll, id=sum_custom_setroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=true), ButtonDefinition(label=Clear, id=sum_custom_setclear00000000-0000-0000-0000-000000000000, style=DANGER, disabled=false), ButtonDefinition(label=Back, id=sum_custom_setback00000000-0000-0000-0000-000000000000, style=SECONDARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }
}
