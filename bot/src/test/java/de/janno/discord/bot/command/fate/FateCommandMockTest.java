package de.janno.discord.bot.command.fate;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class FateCommandMockTest {
    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() {
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @Test
    void roll_simple_full() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4dF ⇒ -1, descriptionOrContent=[▢,＋,−,−], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Roll 4dF, id=fateroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void roll_simple_compact() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4dF ⇒ -1**__  [▢,＋,−,−], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Roll 4dF, id=fateroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void roll_simple_minimal() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "simple", AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("roll");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4dF ⇒ -1, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Roll 4dF, id=fateroll00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_modifier_full() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4dF +3 ⇒ 2, descriptionOrContent=[▢,＋,−,−], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice and add the value of the button, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=-4, id=fate-400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-3, id=fate-300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-2, id=fate-200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-1, id=fate-100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=0, id=fate000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+1, id=fate100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+2, id=fate200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+3, id=fate300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+4, id=fate400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+5, id=fate500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+6, id=fate600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+7, id=fate700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+8, id=fate800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+9, id=fate900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+10, id=fate1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_modifier_compact() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**4dF +3 ⇒ 2**__  [▢,＋,−,−], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice and add the value of the button, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=-4, id=fate-400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-3, id=fate-300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-2, id=fate-200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-1, id=fate-100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=0, id=fate000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+1, id=fate100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+2, id=fate200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+3, id=fate300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+4, id=fate400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+5, id=fate500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+6, id=fate600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+7, id=fate700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+8, id=fate800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+9, id=fate900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+10, id=fate1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_modifier_minimal() {
        FateCommand underTest = new FateCommand(persistenceManager, new DiceUtils(0L));


        FateConfig config = new FateConfig(null, "with_modifier", AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"));
        ButtonEventAdaptorMockFactory<FateConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("fate", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("3");
        underTest.handleComponentInteractEvent(click1).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=4dF +3 ⇒ 2, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click a button to roll four fate dice and add the value of the button, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=-4, id=fate-400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-3, id=fate-300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-2, id=fate-200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=-1, id=fate-100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=0, id=fate000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+1, id=fate100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+2, id=fate200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+3, id=fate300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+4, id=fate400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+5, id=fate500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=+6, id=fate600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+7, id=fate700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+8, id=fate800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+9, id=fate900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=+10, id=fate1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }
}
