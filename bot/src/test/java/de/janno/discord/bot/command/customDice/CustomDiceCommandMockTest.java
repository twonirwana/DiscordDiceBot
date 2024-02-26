package de.janno.discord.bot.command.customDice;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static de.janno.discord.bot.ButtonEventAdaptorMock.CHANNEL_ID;
import static de.janno.discord.bot.ButtonEventAdaptorMock.GUILD_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class CustomDiceCommandMockTest {
    PersistenceManager persistenceManager;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
    }

    @AfterEach
    void cleanUp() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
    }

    @Test
    void legacy_id() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice\u00001d6\u0000");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "reply: The button uses an old format that isn't supported anymore. Please delete it and create a new button message with a slash command.");
    }

    @Test
    void without_configUUID_existingLegacyData() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID();
        persistenceManager = new PersistenceManagerImpl(url, null, null);
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        JdbcConnectionPool connectionPool = JdbcConnectionPool.create(url, null, null);
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long messageId = 0;
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        try (Connection con = connectionPool.getConnection()) {
            try (PreparedStatement preparedStatement =
                         con.prepareStatement("INSERT INTO MESSAGE_DATA(CONFIG_ID, GUILD_ID, CHANNEL_ID, MESSAGE_ID, COMMAND_ID, STATE_CLASS_ID, STATE, CONFIG_CLASS_ID, CONFIG, CREATION_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                preparedStatement.setObject(1, configUUID);
                preparedStatement.setObject(2, GUILD_ID);
                preparedStatement.setObject(3, CHANNEL_ID);
                preparedStatement.setLong(4, messageId);
                preparedStatement.setString(5, "custom_dice");
                preparedStatement.setString(6, "None");
                preparedStatement.setString(7, null);
                preparedStatement.setString(8, "CustomDiceConfig");
                preparedStatement.setString(9, Mapper.serializedObject(config));
                preparedStatement.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
                preparedStatement.execute();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        underTest.createMessageConfig(configUUID, GUILD_ID, CHANNEL_ID, config).ifPresent(persistenceManager::saveMessageConfig);
        AtomicLong messageIdCounter = new AtomicLong(0);
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice", "1_button", messageIdCounter);

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000001, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
        assertThat(persistenceManager.getMessageConfig(configUUID)).isPresent();
        assertThat(persistenceManager.getMessageData(CHANNEL_ID, messageIdCounter.get())).isPresent();
    }


    @Test
    void without_configUUID_missingData() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice", "1_button", new AtomicLong(0));
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        underTest.createMessageConfig(configUUID, GUILD_ID, CHANNEL_ID, config).ifPresent(persistenceManager::saveMessageConfig);


        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "reply: Configuration for the message is missing, please create a new message with the slash command `/custom_dice start`");
    }

    @Test
    void roll_diceEvaluator_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_full_german() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Verarbeite ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Klick auf einen Button um zu würfeln, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_full_ptBR() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.of("pt", "BR"));
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processando ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Clique em um botão para rolar os dados, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_full_with_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_onlyResult_with_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.only_result, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=null, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_onlyResult_multiResult() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6,2d6,3d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.only_result, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                """
                        createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg, descriptionOrContent=**6**
                        **3, 2**
                        **3, 6, 3**, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null""",
        "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }


    @Test
    void roll_diceEvaluator_onlyResult_without_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.only_result, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=null, fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_full_with_images_d100() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));


        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d100", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 73, descriptionOrContent=1d100, fields=[], componentRowDefinitions=[], hasImage=true, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceParser_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1", false, false)), DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 1, descriptionOrContent=1: [1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_compact() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.compact, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=__**Dmg ⇒ 3**__  1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_diceEvaluator_minimal() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.minimal, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Dmg ⇒ 3, fields=[], componentRowDefinitions=[], hasImage=false, type=MESSAGE), targetChannelId: null",
                "deleteMessageById: 0",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_pinned() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void roll_pinnedTwice() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        assertThat(buttonEvent1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        assertThat(buttonEvent2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 4, descriptionOrContent=1d6: [4], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null",
                "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=Dmg, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)",
                "getMessagesState: [0]");
    }

    @Test
    void slash_start() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Attack, id=custom_dice2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10,3d10,3d10, id=custom_dice3_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void slash_start_multiLine() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("d[a\\nb\\nc,\\nd,e\\n];1d20@\\nAttack\\nDown\\n;3d10,3d10,3d10")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d[a b c, d,e ], id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Attack Down, id=custom_dice2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10,3d10,3d10, id=custom_dice3_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        assertThat(buttonEventAdaptorMock.get().getActions()).containsExactlyInAnyOrder(
                "editMessage: message:processing ..., buttonValues=",
                """
                        createResultMessageWithReference: EmbedOrMessageDefinition(title=d[a b c, d,e ] ⇒ e, descriptionOrContent=d[a
                        b
                        c,
                        d,e
                        ]: [e], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: null""",
        "deleteMessageById: 1",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=d[a b c, d,e ], id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Attack Down, id=custom_dice2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10,3d10,3d10, id=custom_dice3_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)"
        );
    }

    @Test
    void slash_start_warn() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10;3;'a'")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder(
                "reply: commandString `3`: did not contain any random element, try for Example `d20` to roll a 20 sided die, `'a'`: did not contain any random element, try for Example `d20` to roll a 20 sided die",
                "createMessageWithoutReference: EmbedOrMessageDefinition(title=null, descriptionOrContent=Click on a button to roll the dice, fields=[], componentRowDefinitions=[ComponentRowDefinition(buttonDefinitions=[ButtonDefinition(label=1d6, id=custom_dice1_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=Attack, id=custom_dice2_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3d10,3d10,3d10, id=custom_dice3_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label=3, id=custom_dice4_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false), ButtonDefinition(label='a', id=custom_dice5_button00000000-0000-0000-0000-000000000000, style=PRIMARY, disabled=false)])], hasImage=false, type=MESSAGE)");
    }

    @Test
    void slash_targetChannelTheSame_error() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("target_channel")
                        .channelIdValue(CHANNEL_ID)
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        assertThat(slashEvent.getActions()).containsExactlyInAnyOrder("reply: The answer target channel must be not the same as the current channel, keep this option empty if the answer should appear in this channel");
    }

    @Test
    void roll_answerChannel() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0));

        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void roll_answerChannelTwice() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 100);
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), cachingDiceEvaluator);

        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        assertThat(buttonEvent1.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 3, descriptionOrContent=1d6: [3], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
        assertThat(buttonEvent2.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 1, descriptionOrContent=1d6: [1], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void channelAlias() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 100);
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), cachingDiceEvaluator);
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("all_users_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "att", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 11, 10, 10, descriptionOrContent=2d20+10: [11, 10], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }

    @Test
    void userChannelAlias() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 100);
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new DiceParser(), cachingDiceEvaluator);
        ChannelConfigCommand channelConfig = new ChannelConfigCommand(persistenceManager);

        SlashEventAdaptorMock slashEvent1 = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("alias")
                .option(CommandInteractionOption.builder()
                        .name("save")
                        .option(CommandInteractionOption.builder().name("name").stringValue("att").build())
                        .option(CommandInteractionOption.builder().name("value").stringValue("2d20+10").build())
                        .build())
                .option(CommandInteractionOption.builder().name("scope").stringValue("current_user_in_this_channel").build())
                .build()));
        channelConfig.handleSlashCommandEvent(slashEvent1, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();


        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "att", false, false)), DiceParserSystem.DICE_EVALUATOR, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        assertThat(buttonEvent.getActions()).containsExactlyInAnyOrder(
                "editMessage: message:Click on a button to roll the dice, buttonValues=1_button",
                "createResultMessageWithReference: EmbedOrMessageDefinition(title=Dmg ⇒ 11, 10, 10, descriptionOrContent=2d20+10: [11, 10], fields=[], componentRowDefinitions=[], hasImage=false, type=EMBED), targetChannelId: 2"
        );
    }
}
