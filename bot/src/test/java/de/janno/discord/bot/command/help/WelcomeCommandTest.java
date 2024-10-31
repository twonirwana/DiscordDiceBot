package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class WelcomeCommandTest {

    WelcomeCommand underTest;
    Expect expect;

    @BeforeEach
    void setup() {
        PersistenceManager persistenceManager = Mockito.mock(PersistenceManager.class);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        underTest = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }


    @Test
    public void getButtonMessageWithState_legacyKey() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("invalidId", StateData.empty()), 1L, 2L);
        assertThat(res).isEmpty();
    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("dnd5", StateData.empty()), 1L, 2L);
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
                .containsExactly("D20",
                        "D20 Advantage",
                        "D20 Disadvantage",
                        "1d4",
                        "1d6",
                        "1d8",
                        "1d10",
                        "1d12",
                        "1d100",
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
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("coin", StateData.empty()), 1L, 2L);
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
                .containsExactly("Coin Toss");
    }

    @Test
    public void getButtonMessageWithState_nWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("nWoD", StateData.empty()), 1L, 2L);

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
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("oWoD", StateData.empty()), 1L, 2L);
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
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("shadowrun", StateData.empty()), 1L, 2L);
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("dice_calculator", StateData.empty()), 1L, 2L);
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
                        "sum_custom_set16_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set17_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set18_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set19_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set20_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set21_button00000000-0000-0000-0000-000000000000",
                        "sum_custom_set22_button00000000-0000-0000-0000-000000000000",
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
                        "(",
                        ")",
                        "1",
                        "2",
                        "3",
                        "d",
                        "r",
                        "0",
                        "00",
                        "d20",
                        "k",
                        "l",
                        "ADV",
                        "DIS",
                        "Roll",
                        "Clear",
                        "Back");

    }

    @Test
    public void getButtonMessageWithState_other() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>("-", StateData.empty()), 1L, 2L);
        assertThat(res)
                .isEmpty();

    }

    @Test
    public void getWelcomeMessage() {
        EmbedOrMessageDefinition res = underTest.getWelcomeMessage().apply(new DiscordConnector.WelcomeRequest(123L, 456L, Locale.ENGLISH));
        assertThat(res.getDescriptionOrContent())
                .isEqualTo("""
                        Welcome to the Button Dice Bot,
                        use one of the example buttons below to start one of the RPG dice systems, use `/quickstart system` to select one of many RPG presets or use the slash command to configure your own custom dice system (see https://github.com/twonirwana/DiscordDiceBot for details or the slash command `/help`).
                        You can also use the slash command `/r` to directly roll dice with.
                        For help or feature request come to the support discord server: https://discord.gg/e43BsqKpFr""");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId))
                .containsExactly(
                        "welcomeDND5_IMAGE00000000-0000-0000-0000-000000000000",
                        "welcomeDND500000000-0000-0000-0000-000000000000",
                        "welcomeFATE_IMAGE00000000-0000-0000-0000-000000000000",
                        "welcomeCOIN00000000-0000-0000-0000-000000000000",
                        "welcomeNWOD00000000-0000-0000-0000-000000000000",
                        "welcomeOWOD00000000-0000-0000-0000-000000000000",
                        "welcomeSHADOWRUN_IMAGE00000000-0000-0000-0000-000000000000",
                        "welcomeDICE_CALCULATOR00000000-0000-0000-0000-000000000000");
        assertThat(res.getComponentRowDefinitions()
                .stream()
                .flatMap(s -> s.getButtonDefinitions().stream())
                .map(ButtonDefinition::getLabel))
                .containsExactly(
                        "Dungeon & Dragons 5e",
                        "Dungeon & Dragons 5e without Dice Images",
                        "Fate",
                        "🪙Coin Toss",
                        "nWod / Chronicles of Darkness",
                        "oWod / Storyteller System",
                        "Shadowrun",
                        "Dice Calculator");

    }

    @ParameterizedTest
    @CsvSource({
            "fate_image",
            "dnd5",
            "dnd5_image",
            "nWoD",
            "oWoD",
            "shadowrun",
            "coin",
            "dice_calculator"
    })
    void createMessageDataForNewMessage(String buttonValue) {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessageWithState(UUID.fromString("00000000-0000-0000-0000-000000000000"), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH), new State<>(buttonValue, StateData.empty()), 1L, 2L);
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

        assertThat(res).isFalse();
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
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        RollConfig config = new RollConfig(123L, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        ConfigAndState<RollConfig, StateData> configAndState = underTest.deserializeAndUpdateState(loaded, "3");

        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState()).isEqualTo(new State<>("3", StateData.empty()));
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "welcome", "Config", """
                ---
                answerTargetChannelId: 123
                answerFormatType: "full"
                configLocale: "de"
                diceStyleAndColor:
                  diceImageStyle: "none"
                  configuredDefaultColor: "none"
                """, null ,null);


        ConfigAndState<RollConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new RollConfig(123L, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        RollConfig config = new RollConfig(123L, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        Optional<MessageConfigDTO> toSave = underTest.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }


}