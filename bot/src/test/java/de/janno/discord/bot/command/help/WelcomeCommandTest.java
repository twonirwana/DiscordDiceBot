package de.janno.discord.bot.command.help;

import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WelcomeCommandTest {

    WelcomeCommand underTest;

    @BeforeEach
    void setup() {
        PersistenceManager persistenceManager = Mockito.mock(PersistenceManager.class);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        underTest = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }


    @Test
    public void getButtonMessageWithState_fate() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("fate", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **Modifier**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_parameterid100000000-0000-0000-0000-000000000000",
                        "custom_parameterid200000000-0000-0000-0000-000000000000",
                        "custom_parameterid300000000-0000-0000-0000-000000000000",
                        "custom_parameterid400000000-0000-0000-0000-000000000000",
                        "custom_parameterid500000000-0000-0000-0000-000000000000",
                        "custom_parameterid600000000-0000-0000-0000-000000000000",
                        "custom_parameterid700000000-0000-0000-0000-000000000000",
                        "custom_parameterid800000000-0000-0000-0000-000000000000",
                        "custom_parameterid900000000-0000-0000-0000-000000000000",
                        "custom_parameterid1000000000-0000-0000-0000-000000000000",
                        "custom_parameterid1100000000-0000-0000-0000-000000000000",
                        "custom_parameterid1200000000-0000-0000-0000-000000000000",
                        "custom_parameterid1300000000-0000-0000-0000-000000000000",
                        "custom_parameterid1400000000-0000-0000-0000-000000000000",
                        "custom_parameterid1500000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("-4", "-3", "-2", "-1", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("dnd5", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000",
                        "custom_dice2_button00000000-0000-0000-0000-000000000000",
                        "custom_dice3_button00000000-0000-0000-0000-000000000000",
                        "custom_dice4_button00000000-0000-0000-0000-000000000000",
                        "custom_dice5_button00000000-0000-0000-0000-000000000000",
                        "custom_dice6_button00000000-0000-0000-0000-000000000000",
                        "custom_dice7_button00000000-0000-0000-0000-000000000000",
                        "custom_dice8_button00000000-0000-0000-0000-000000000000",
                        "custom_dice9_button00000000-0000-0000-0000-000000000000",
                        "custom_dice10_button00000000-0000-0000-0000-000000000000",
                        "custom_dice11_button00000000-0000-0000-0000-000000000000",
                        "custom_dice12_button00000000-0000-0000-0000-000000000000",
                        "custom_dice13_button00000000-0000-0000-0000-000000000000",
                        "custom_dice14_button00000000-0000-0000-0000-000000000000",
                        "custom_dice15_button00000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1d4",
                        "1d6",
                        "1d8",
                        "1d10",
                        "1d12",
                        "1d20",
                        "1d100",
                        "D20 Advantage",
                        "D20 Disadvantage",
                        "2d4",
                        "2d6",
                        "2d8",
                        "2d10",
                        "2d12",
                        "2d20");
    }


    @Test
    public void getButtonMessageWithState_coin() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("coin", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("Coin Toss \uD83E\uDE99");
    }

    @Test
    public void getButtonMessageWithState_nWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("nWoD", StateData.empty()), 1L, 2L);

        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **Number of Dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_parameterid100000000-0000-0000-0000-000000000000",
                        "custom_parameterid200000000-0000-0000-0000-000000000000",
                        "custom_parameterid300000000-0000-0000-0000-000000000000",
                        "custom_parameterid400000000-0000-0000-0000-000000000000",
                        "custom_parameterid500000000-0000-0000-0000-000000000000",
                        "custom_parameterid600000000-0000-0000-0000-000000000000",
                        "custom_parameterid700000000-0000-0000-0000-000000000000",
                        "custom_parameterid800000000-0000-0000-0000-000000000000",
                        "custom_parameterid900000000-0000-0000-0000-000000000000",
                        "custom_parameterid1000000000-0000-0000-0000-000000000000",
                        "custom_parameterid1100000000-0000-0000-0000-000000000000",
                        "custom_parameterid1200000000-0000-0000-0000-000000000000",
                        "custom_parameterid1300000000-0000-0000-0000-000000000000",
                        "custom_parameterid1400000000-0000-0000-0000-000000000000",
                        "custom_parameterid1500000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
    }

    @Test
    public void getButtonMessageWithState_oWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("oWoD", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **Number of Dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_parameterid100000000-0000-0000-0000-000000000000",
                        "custom_parameterid200000000-0000-0000-0000-000000000000",
                        "custom_parameterid300000000-0000-0000-0000-000000000000",
                        "custom_parameterid400000000-0000-0000-0000-000000000000",
                        "custom_parameterid500000000-0000-0000-0000-000000000000",
                        "custom_parameterid600000000-0000-0000-0000-000000000000",
                        "custom_parameterid700000000-0000-0000-0000-000000000000",
                        "custom_parameterid800000000-0000-0000-0000-000000000000",
                        "custom_parameterid900000000-0000-0000-0000-000000000000",
                        "custom_parameterid1000000000-0000-0000-0000-000000000000",
                        "custom_parameterid1100000000-0000-0000-0000-000000000000",
                        "custom_parameterid1200000000-0000-0000-0000-000000000000",
                        "custom_parameterid1300000000-0000-0000-0000-000000000000",
                        "custom_parameterid1400000000-0000-0000-0000-000000000000",
                        "custom_parameterid1500000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");


    }

    @Test
    public void getButtonMessageWithState_Shadowrun() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("shadowrun", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **number of dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("custom_parameterid100000000-0000-0000-0000-000000000000",
                        "custom_parameterid200000000-0000-0000-0000-000000000000",
                        "custom_parameterid300000000-0000-0000-0000-000000000000",
                        "custom_parameterid400000000-0000-0000-0000-000000000000",
                        "custom_parameterid500000000-0000-0000-0000-000000000000",
                        "custom_parameterid600000000-0000-0000-0000-000000000000",
                        "custom_parameterid700000000-0000-0000-0000-000000000000",
                        "custom_parameterid800000000-0000-0000-0000-000000000000",
                        "custom_parameterid900000000-0000-0000-0000-000000000000",
                        "custom_parameterid1000000000-0000-0000-0000-000000000000",
                        "custom_parameterid1100000000-0000-0000-0000-000000000000",
                        "custom_parameterid1200000000-0000-0000-0000-000000000000",
                        "custom_parameterid1300000000-0000-0000-0000-000000000000",
                        "custom_parameterid1400000000-0000-0000-0000-000000000000",
                        "custom_parameterid1500000000-0000-0000-0000-000000000000",
                        "custom_parameterid1600000000-0000-0000-0000-000000000000",
                        "custom_parameterid1700000000-0000-0000-0000-000000000000",
                        "custom_parameterid1800000000-0000-0000-0000-000000000000",
                        "custom_parameterid1900000000-0000-0000-0000-000000000000",
                        "custom_parameterid2000000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");

    }

    @Test
    public void getButtonMessageWithState_diceCalculator() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("dice_calculator", StateData.empty()), 1L, 2L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click the buttons to add dice to the set and then on Roll");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly("sum_custom_set1_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set2_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set3_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set4_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set5_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set6_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set7_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set8_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set9_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set10_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set11_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set12_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set13_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set14_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set15_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_setroll00000000-0000-0000-0000-000000000000",
                        "sum_custom_setclear00000000-0000-0000-0000-000000000000",
                        "sum_custom_setback00000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly("7",
                        "8",
                        "9",
                        "+",
                        "-",
                        "4",
                        "5",
                        "6",
                        "d",
                        "k",
                        "1",
                        "2",
                        "3",
                        "0",
                        "l",
                        "Roll",
                        "Clear",
                        "Back");

    }

    @Test
    public void getButtonMessageWithState_other() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("-", StateData.empty()), 1L, 2L);
        assertThat(res)
                .isEmpty();

    }

    @Test
    public void getWelcomeMessage() {
        EmbedOrMessageDefinition res = underTest.getWelcomeMessage().apply(new DiscordConnector.WelcomeRequest(123L, 456L, Locale.ENGLISH));
        assertThat(res.getDescriptionOrContent())
                .isEqualTo("""
                        Welcome to the Button Dice Bot,
                        use one of the example buttons below to start one of the RPG dice systems or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).
                        You can also use the slash command `/r` to directly roll dice with.
                        For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr""");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly(
                        "welcomefate00000000-0000-0000-0000-000000000000",
                        "welcomefate_image00000000-0000-0000-0000-000000000000",
                        "welcomednd500000000-0000-0000-0000-000000000000",
                        "welcomednd5_image00000000-0000-0000-0000-000000000000",
                        "welcomenWoD00000000-0000-0000-0000-000000000000",
                        "welcomeoWoD00000000-0000-0000-0000-000000000000",
                        "welcomeshadowrun00000000-0000-0000-0000-000000000000",
                        "welcomecoin00000000-0000-0000-0000-000000000000",
                        "welcomedice_calculator00000000-0000-0000-0000-000000000000");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly(
                        "Fate",
                        "Fate with dice images",
                        "D&D5e",
                        "D&D5e with dice images",
                        "nWoD",
                        "oWoD",
                        "Shadowrun",
                        "Coin Toss 🪙",
                        "Dice Calculator");

    }

    @ParameterizedTest
    @CsvSource({
            "fate",
            "dnd5",
            "nWoD",
            "oWoD",
            "shadowrun",
            "coin",
            "dice_calculator"
    })
    void createMessageDataForNewMessage(String buttonValue) {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>(buttonValue, StateData.empty()), 1L, 2L);
        assertThat(res).isPresent();
    }

    @Test
    public void shouldKeepExistingButtonMessage() {
        assertThat(underTest.shouldKeepExistingButtonMessage(Mockito.mock(ButtonEventAdaptor.class))).isTrue();
    }

    @Test
    public void getAnswer() {
        assertThat(underTest.getAnswer(null, null, 0L, 0L)).isEmpty();
    }


    @Test
    public void matchingComponentCustomId() {
        boolean res = underTest.matchingComponentCustomId("welcome,fate");

        assertThat(res).isTrue();
    }

    @Test
    public void matchingComponentCustomId_notMatch() {
        boolean res = underTest.matchingComponentCustomId("welcome2,fate");

        assertThat(res).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("welcomefate")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("welcome2fate")).isFalse();
    }

    @Test
    void getName() {
        assertThat(underTest.getCommandId()).isEqualTo("welcome");
    }


    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        assertThat(res.toString()).isEqualTo("CommandDefinition(name=welcome, description=Displays the welcome message, nameLocales=[], descriptionLocales=[], options=[CommandDefinitionOption(type=SUB_COMMAND, name=start, nameLocales=[], description=Displays the welcome message, descriptionLocales=[], required=false, choices=[], options=[CommandDefinitionOption(type=STRING, name=language, nameLocales=[], description=The language of the bot messages, descriptionLocales=[], required=false, choices=[CommandDefinitionOptionChoice(name=English, value=en, nameLocales=[]), CommandDefinitionOptionChoice(name=German, value=de, nameLocales=[])], options=[], minValue=null, maxValue=null, autoComplete=false)], minValue=null, maxValue=null, autoComplete=false), CommandDefinitionOption(type=SUB_COMMAND, name=help, nameLocales=[LocaleValue[locale=de, value=hilfe]], description=Get help for /welcome, descriptionLocales=[LocaleValue[locale=de, value=Get help for /[welcome]]], required=false, choices=[], options=[], minValue=null, maxValue=null, autoComplete=false)])");

    }
}