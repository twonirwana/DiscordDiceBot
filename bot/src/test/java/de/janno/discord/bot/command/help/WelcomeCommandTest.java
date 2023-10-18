package de.janno.discord.bot.command.help;

import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

class WelcomeCommandTest {

    WelcomeCommand underTest;

    @BeforeEach
    void setup() {
        PersistenceManager persistenceManager = Mockito.mock(PersistenceManager.class);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);
        CountSuccessesCommand countSuccessesCommand = new CountSuccessesCommand(persistenceManager);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        FateCommand fateCommand = new FateCommand(persistenceManager);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        PoolTargetCommand poolTargetCommand = new PoolTargetCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, fateCommand, customDiceCommand, countSuccessesCommand, sumCustomSetCommand, poolTargetCommand);
        underTest = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }


    @Test
    public void getButtonMessageWithState_fate() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("fate", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click a button to roll four fate dice and add the value of the button");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getId))
                .containsExactly("fate-400000000-0000-0000-0000-000000000000",
                        "fate-300000000-0000-0000-0000-000000000000",
                        "fate-200000000-0000-0000-0000-000000000000",
                        "fate-100000000-0000-0000-0000-000000000000",
                        "fate000000000-0000-0000-0000-000000000000",
                        "fate100000000-0000-0000-0000-000000000000",
                        "fate200000000-0000-0000-0000-000000000000",
                        "fate300000000-0000-0000-0000-000000000000",
                        "fate400000000-0000-0000-0000-000000000000",
                        "fate500000000-0000-0000-0000-000000000000",
                        "fate600000000-0000-0000-0000-000000000000",
                        "fate700000000-0000-0000-0000-000000000000",
                        "fate800000000-0000-0000-0000-000000000000",
                        "fate900000000-0000-0000-0000-000000000000",
                        "fate1000000000-0000-0000-0000-000000000000");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getLabel))
                .containsExactly("-4", "-3", "-2", "-1", "0", "+1", "+2", "+3", "+4", "+5", "+6", "+7", "+8", "+9", "+10");

    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("dnd5", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
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
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("coin", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getId))
                .containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getLabel))
                .containsExactly("Coin Toss \uD83E\uDE99");
    }

    @Test
    public void getButtonMessageWithState_nWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("nWoD", StateData.empty()), 1L, 2L);

        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click to roll the dice against 8, reroll for: [10]");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getId))
                .containsExactly("count_successes100000000-0000-0000-0000-000000000000",
                        "count_successes200000000-0000-0000-0000-000000000000",
                        "count_successes300000000-0000-0000-0000-000000000000",
                        "count_successes400000000-0000-0000-0000-000000000000",
                        "count_successes500000000-0000-0000-0000-000000000000",
                        "count_successes600000000-0000-0000-0000-000000000000",
                        "count_successes700000000-0000-0000-0000-000000000000",
                        "count_successes800000000-0000-0000-0000-000000000000",
                        "count_successes900000000-0000-0000-0000-000000000000",
                        "count_successes1000000000-0000-0000-0000-000000000000",
                        "count_successes1100000000-0000-0000-0000-000000000000",
                        "count_successes1200000000-0000-0000-0000-000000000000",
                        "count_successes1300000000-0000-0000-0000-000000000000",
                        "count_successes1400000000-0000-0000-0000-000000000000",
                        "count_successes1500000000-0000-0000-0000-000000000000");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10", "11d10", "12d10", "13d10", "14d10", "15d10");
    }

    @Test
    public void getButtonMessageWithState_oWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("oWoD", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on the buttons to roll dice, with ask reroll:10 and botch:1");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getId))
                .containsExactly("pool_target100000000-0000-0000-0000-000000000000",
                        "pool_target200000000-0000-0000-0000-000000000000",
                        "pool_target300000000-0000-0000-0000-000000000000",
                        "pool_target400000000-0000-0000-0000-000000000000",
                        "pool_target500000000-0000-0000-0000-000000000000",
                        "pool_target600000000-0000-0000-0000-000000000000",
                        "pool_target700000000-0000-0000-0000-000000000000",
                        "pool_target800000000-0000-0000-0000-000000000000",
                        "pool_target900000000-0000-0000-0000-000000000000",
                        "pool_target1000000000-0000-0000-0000-000000000000",
                        "pool_target1100000000-0000-0000-0000-000000000000",
                        "pool_target1200000000-0000-0000-0000-000000000000",
                        "pool_target1300000000-0000-0000-0000-000000000000",
                        "pool_target1400000000-0000-0000-0000-000000000000",
                        "pool_target1500000000-0000-0000-0000-000000000000");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getLabel))
                .containsExactly("1d10", "2d10", "3d10", "4d10", "5d10", "6d10", "7d10", "8d10", "9d10", "10d10",
                        "11d10", "12d10", "13d10", "14d10", "15d10");

    }

    @Test
    public void getButtonMessageWithState_Shadowrun() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("shadowrun", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click to roll the dice against 5 and check for more then half of dice 1s");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getId))
                .containsExactly("count_successes100000000-0000-0000-0000-000000000000",
                        "count_successes200000000-0000-0000-0000-000000000000",
                        "count_successes300000000-0000-0000-0000-000000000000",
                        "count_successes400000000-0000-0000-0000-000000000000",
                        "count_successes500000000-0000-0000-0000-000000000000",
                        "count_successes600000000-0000-0000-0000-000000000000",
                        "count_successes700000000-0000-0000-0000-000000000000",
                        "count_successes800000000-0000-0000-0000-000000000000",
                        "count_successes900000000-0000-0000-0000-000000000000",
                        "count_successes1000000000-0000-0000-0000-000000000000",
                        "count_successes1100000000-0000-0000-0000-000000000000",
                        "count_successes1200000000-0000-0000-0000-000000000000",
                        "count_successes1300000000-0000-0000-0000-000000000000",
                        "count_successes1400000000-0000-0000-0000-000000000000",
                        "count_successes1500000000-0000-0000-0000-000000000000",
                        "count_successes1600000000-0000-0000-0000-000000000000",
                        "count_successes1700000000-0000-0000-0000-000000000000",
                        "count_successes1800000000-0000-0000-0000-000000000000",
                        "count_successes1900000000-0000-0000-0000-000000000000",
                        "count_successes2000000000-0000-0000-0000-000000000000");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                        .stream()
                        .flatMap(Collection::stream)
                        .flatMap(s -> s.getButtonDefinitions().stream())
                        .map(ButtonDefinition::getLabel))
                .containsExactly("1d6", "2d6", "3d6", "4d6", "5d6", "6d6", "7d6", "8d6", "9d6", "10d6", "11d6", "12d6", "13d6", "14d6", "15d6", "16d6", "17d6", "18d6", "19d6", "20d6");

    }

    @Test
    public void getButtonMessageWithState_diceCalculator() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("dice_calculator", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click the buttons to add dice to the set and then on Roll");
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
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
        Assertions.assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), null, new State<>("-", StateData.empty()), 1L, 2L);
        Assertions.assertThat(res)
                .isEmpty();

    }

    @Test
    public void getWelcomeMessage() {
        EmbedOrMessageDefinition res = underTest.getWelcomeMessage();
        Assertions.assertThat(res.getDescriptionOrContent())
                .isEqualTo("""
                        Welcome to the Button Dice Bot,
                        use one of the example buttons below to start one of the RPG dice systems or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).\s
                        You can also use the slash command `/r` to directly roll dice with.
                        For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr""");
        Assertions.assertThat(res.getComponentRowDefinitions()
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
        Assertions.assertThat(res.getComponentRowDefinitions()
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Config(null, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none")), new State<>(buttonValue, StateData.empty()), 1L, 2L);
        Assertions.assertThat(res).isPresent();
    }

    @Test
    public void shouldKeepExistingButtonMessage() {
        Assertions.assertThat(underTest.shouldKeepExistingButtonMessage(Mockito.mock(ButtonEventAdaptor.class))).isTrue();
    }

    @Test
    public void getAnswer() {
        Assertions.assertThat(underTest.getAnswer(null, null, 0L, 0L)).isEmpty();
    }


    @Test
    public void matchingComponentCustomId() {
        boolean res = underTest.matchingComponentCustomId("welcome,fate");

        Assertions.assertThat(res).isTrue();
    }

    @Test
    public void matchingComponentCustomId_notMatch() {
        boolean res = underTest.matchingComponentCustomId("welcome2,fate");

        Assertions.assertThat(res).isFalse();
    }

    @Test
    void matchingComponentCustomId_match() {
        Assertions.assertThat(underTest.matchingComponentCustomId("welcomefate")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        Assertions.assertThat(underTest.matchingComponentCustomId("welcome2fate")).isFalse();
    }

    @Test
    void getName() {
        Assertions.assertThat(underTest.getCommandId()).isEqualTo("welcome");
    }


    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        Assertions.assertThat(res).isEqualTo(CommandDefinition.builder()
                .name("welcome")
                .description("Displays the welcome message")
                .option(CommandDefinitionOption.builder()
                        .name("start")
                        .description("Start")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .option(CommandDefinitionOption.builder()
                        .name("help")
                        .description("Help")
                        .type(CommandDefinitionOption.Type.SUB_COMMAND)
                        .build())
                .build());

    }
}