package de.janno.discord.bot.command.help;

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
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ComponentDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("invalidId", StateData.empty()), 1L, 2L, 3L);
        assertThat(res).isEmpty();
    }

    @Test
    public void getButtonMessageWithState_dnd5() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("dnd5", StateData.empty()), 1L, 2L, 3L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
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
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("coin", StateData.empty()), 1L, 2L, 3L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click on a button to roll the dice");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
                .containsExactly("custom_dice1_button00000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
                .containsExactly("Coin Toss");
    }

    @Test
    public void getButtonMessageWithState_nWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("nWoD", StateData.empty()), 1L, 2L, 3L);

        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **Number of Dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
                .containsExactly("custom_parameterNumber of Dice-id100000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id200000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id300000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id400000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id500000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id600000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id700000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id800000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id900000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1000000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1100000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1200000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1300000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1400000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1500000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
    }

    @Test
    public void getButtonMessageWithState_oWoD() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("oWoD", StateData.empty()), 1L, 2L, 3L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **Number of Dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
                .containsExactly("custom_parameterNumber of Dice-id100000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id200000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id300000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id400000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id500000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id600000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id700000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id800000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id900000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1000000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1100000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1200000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1300000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1400000000-0000-0000-0000-000000000000",
                        "custom_parameterNumber of Dice-id1500000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");


    }

    @Test
    public void getButtonMessageWithState_Shadowrun() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("shadowrun", StateData.empty()), 1L, 2L, 3L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Please select value for **number of dice**");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
                .containsExactly("custom_parameternumber of dice-id100000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id200000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id300000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id400000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id500000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id600000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id700000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id800000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id900000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1000000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1100000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1200000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1300000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1400000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1500000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1600000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1700000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1800000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id1900000000-0000-0000-0000-000000000000",
                        "custom_parameternumber of dice-id2000000000-0000-0000-0000-000000000000");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
                .containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20");

    }

    @Test
    public void getButtonMessageWithState_diceCalculator() {
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("dice_calculator", StateData.empty()), 1L, 2L, 3L);
        assertThat(res.map(EmbedOrMessageDefinition::getDescriptionOrContent))
                .contains("Click the buttons to add dice to the set and then on Roll");
        assertThat(res.map(EmbedOrMessageDefinition::getComponentRowDefinitions)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId))
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
                .flatMap(s -> s.getComponentDefinitions().stream())
                .map(ComponentDefinition::getLabelOrPlaceholder))
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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"),
                new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>("-", StateData.empty()), 1L, 2L, 3L);
        assertThat(res)
                .isEmpty();

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
        Optional<EmbedOrMessageDefinition> res = underTest.createNewButtonMessage(UUID.fromString("00000000-0000-0000-0000-000000000000"), new RollConfig(null, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null), new State<>(buttonValue, StateData.empty()), 1L, 2L, 3L);
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
    public void getId() {
        assertThat(underTest.getCommandId()).isEqualTo("welcome");
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
                """, null, null);


        ConfigAndState<RollConfig, StateData> configAndState = underTest.deserializeAndUpdateState(savedData, "3");
        assertThat(configAndState.getConfig()).isEqualTo(new RollConfig(123L, AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN, null, null));
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(StateData.empty());
    }

}