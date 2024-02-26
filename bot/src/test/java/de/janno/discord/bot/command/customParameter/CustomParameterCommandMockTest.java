package de.janno.discord.bot.command.customParameter;

import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
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

import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
    void roll_direct() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


        CustomParameterConfig config = new CustomParameterConfig(null, "{n}d{s:4/6/10/20@!20}+{modi:1/2/3}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 1d{s}+{modi}=\n" +
                        "Please select value for **s**, buttonValues=id1,id2,id3,id4,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=1d20+''= ⇒ 13, descriptionOrContent=[13], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={n}d{s}+{modi}=\nPlease select value for **n**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11, id=custom_parameterid1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12, id=custom_parameterid1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13, id=custom_parameterid1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14, id=custom_parameterid1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15, id=custom_parameterid1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_ignore_direct() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


        CustomParameterConfig config = new CustomParameterConfig(null, "{n}d{s:4/6/10/20@!20}+{modi:1/2/3}=", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();
        ButtonEventAdaptorMock click3 = factory.getButtonClickOnLastButtonMessage("id1");
        underTest.handleComponentInteractEvent(click3).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 1d{s}+{modi}=\n" +
                        "Please select value for **s**, buttonValues=id1,id2,id3,id4,clear");
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 1d10+{modi}=\n" +
                        "Please select value for **modi**, buttonValues=id1,id2,id3,clear");
        assertThat(click3.getActions()).containsExactlyInAnyOrder("editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=1d10+1= ⇒ 4, descriptionOrContent=[3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={n}d{s}+{modi}=\nPlease select value for **n**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=11, id=custom_parameterid1100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=12, id=custom_parameterid1200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=13, id=custom_parameterid1300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=14, id=custom_parameterid1400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=15, id=custom_parameterid1500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void slash_start_multiLine() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue("d[a\\nb\\nc,\\nd,e\\n]+{bonus:0@None/3@Small\\nBonus/5@\\nBig\\nBonus\\n}")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **bonus**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=None, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Small Bonus, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label= Big Bonus , id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        assertThat(buttonEventAdaptorMock.get().getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                """
                        createResultMessageWithReference: EmbedOrMessageDefinition(title=bonus: None ⇒ a
                        b
                        c, 0, descriptionOrContent=[a
                        b
                        c], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null""",
                "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **bonus**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=None, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Small Bonus, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label= Big Bonus , id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void slash_start_directRoll() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator((min, max) -> max, 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("expression")
                        .stringValue("{n:1d20@!d20/1d6/2d8}+{b:1<=>5}=")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **n**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d20, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=SUCCESS, disabled=false), ButtonDefinition(label=1d6, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d8, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        assertThat(buttonEventAdaptorMock.get().getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=n: d20 ⇒ 20, descriptionOrContent=, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED), targetChannelId: null",
                "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Please select value for **n**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d20, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=SUCCESS, disabled=false), ButtonDefinition(label=1d6, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2d8, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_fullGerman() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nBitte wähle den Wert für **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear"
        );
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Verarbeite ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nBitte wähle den Wert für **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_full_ptBR() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


        CustomParameterConfig config = new CustomParameterConfig(null, "{numberOfDice:1<=>10}d{sides:1/4/6/8/10/12/20/100}", DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.of("pt", "BR"));
        ButtonEventAdaptorMockFactory<CustomParameterConfig, CustomParameterStateData> factory = new ButtonEventAdaptorMockFactory<>("custom_parameter", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock click1 = factory.getButtonClickOnLastButtonMessage("id4");
        underTest.handleComponentInteractEvent(click1).block();
        ButtonEventAdaptorMock click2 = factory.getButtonClickOnLastButtonMessage("id3");
        underTest.handleComponentInteractEvent(click2).block();

        assertThat(click1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:invokingUser: 4d{sides}\nPor favor selecione um valor para **sides**, buttonValues=id1,id2,id3,id4,id5,id6,id7,id8,clear"
        );
        assertThat(click2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processando ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=4d6 ⇒ 1, 1, 6, 3, descriptionOrContent=[1, 1, 6, 3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent={numberOfDice}d{sides}\nPor favor selecione um valor para **numberOfDice**, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1, id=custom_parameterid100000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=2, id=custom_parameterid200000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_parameterid300000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=4, id=custom_parameterid400000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=5, id=custom_parameterid500000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)]), ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=6, id=custom_parameterid600000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=7, id=custom_parameterid700000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=8, id=custom_parameterid800000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=9, id=custom_parameterid900000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=10, id=custom_parameterid1000000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_textLegacy() {
        CustomParameterCommand underTest = new CustomParameterCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


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
