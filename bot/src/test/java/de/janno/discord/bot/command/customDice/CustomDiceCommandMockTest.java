package de.janno.discord.bot.command.customDice;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.ButtonEventAdaptorMockFactory;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import io.avaje.config.Config;
import org.apache.commons.io.FileUtils;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

import static de.janno.discord.bot.ButtonEventAdaptorMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class CustomDiceCommandMockTest {
    PersistenceManager persistenceManager;
    Expect expect;

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
        Config.setProperty("diceEvaluator.cacheSize", "0");
    }

    @Test
    void legacy_id() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice\u00001d6\u0000");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void without_configUUID_existingLegacyData() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID();
        persistenceManager = new PersistenceManagerImpl(url, null, null);
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        JdbcConnectionPool connectionPool = JdbcConnectionPool.create(url, null, null);
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        long messageId = 0;
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
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
        underTest.createMessageConfig(configUUID, GUILD_ID, CHANNEL_ID, USER_ID, config).ifPresent(persistenceManager::saveMessageConfig);
        AtomicLong messageIdCounter = new AtomicLong(0);
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice", "1_button", messageIdCounter);

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
        assertThat(persistenceManager.getMessageConfig(configUUID)).isPresent();
        assertThat(persistenceManager.getMessageData(CHANNEL_ID, messageIdCounter.get())).isPresent();
    }


    @Test
    void without_configUUID_missingData() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock("custom_dice", "1_button", new AtomicLong(0));
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        underTest.createMessageConfig(configUUID, GUILD_ID, CHANNEL_ID, USER_ID, config).ifPresent(persistenceManager::saveMessageConfig);


        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_full() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_missingPermission() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");
        buttonEvent.setPermissionCheck("Missing MESSAGE_SEND");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_full_german() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.GERMAN, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_full_ptBR() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.of("pt", "BR"), null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_full_with_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_onlyResult_with_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.only_result, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_onlyResult_multiResult() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6,2d6,3d6", false, false, null)), AnswerFormatType.only_result, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_onlyDice_multiResult() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6,2d6,3d6", false, false, null)), AnswerFormatType.only_dice, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }


    @Test
    void roll_diceEvaluator_onlyResult_without_images() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));
        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.only_result, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_full_with_images_d100() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));


        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d100", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v1, "black_and_gold"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_compact() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.compact, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_diceEvaluator_minimal() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.minimal, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_pinned() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, true);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_pinnedTwice() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        CustomDiceConfig config = new CustomDiceConfig(null, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, true);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();

        expect.scenario("event1").toMatchSnapshot(buttonEvent1.getSortedActions());

        expect.scenario("event2").toMatchSnapshot(buttonEvent2.getSortedActions());
    }

    @Test
    void slash_start() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void slash_start_multiLine() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("d[a\\nb\\nc,\\nd,e\\n];1d20@\\nAttack\\nDown\\n;3d10,3d10,3d10")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();
        underTest.handleComponentInteractEvent(buttonEventAdaptorMock.get()).block();
        expect.scenario("event2").toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
    }

    @Test
    void slash_start_warn() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("start")
                .option(CommandInteractionOption.builder()
                        .name("buttons")
                        .stringValue("1d6;1d20@Attack;3d10,3d10,3d10;3;'a'")
                        .build())
                .build()));
        underTest.handleSlashCommandEvent(slashEvent, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH).block();

        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void slash_targetChannelTheSame_error() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

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

        expect.toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void roll_answerChannel() {
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, new CachingDiceEvaluator(new RandomNumberSupplier(0)));

        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void roll_answerChannelTwice() {
        Config.setProperty("diceEvaluator.cacheSize", "1");
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);

        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "1d6", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);

        ButtonEventAdaptorMock buttonEvent1 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent1).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        ButtonEventAdaptorMock buttonEvent2 = factory.getButtonClickOnLastButtonMessage("1_button");
        underTest.handleComponentInteractEvent(buttonEvent2).block();
        assertThat(cachingDiceEvaluator.getCacheSize()).isEqualTo(1);
        expect.scenario("event1").toMatchSnapshot(buttonEvent1.getSortedActions());
        expect.scenario("event2").toMatchSnapshot(buttonEvent2.getSortedActions());
    }

    @Test
    void channelAlias() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
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


        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "att", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }

    @Test
    void userChannelAlias() {
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand underTest = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
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


        CustomDiceConfig config = new CustomDiceConfig(2L, ImmutableList.of(new ButtonIdLabelAndDiceExpression("1_button", "Dmg", "att", false, false, null)), AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, null, null);
        ButtonEventAdaptorMockFactory<CustomDiceConfig, StateData> factory = new ButtonEventAdaptorMockFactory<>("custom_dice", underTest, config, persistenceManager, false);
        ButtonEventAdaptorMock buttonEvent = factory.getButtonClickOnLastButtonMessage("1_button");

        underTest.handleComponentInteractEvent(buttonEvent).block();

        expect.toMatchSnapshot(buttonEvent.getSortedActions());
    }
}
