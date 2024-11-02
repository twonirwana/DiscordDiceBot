package de.janno.discord.bot.command.starter;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.IncrementingUUIDSupplier;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.DiscordConnector;
import de.janno.discord.connector.api.OptionValue;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class StarterCommandTest {

    PersistenceManager persistenceManager;
    Expect expect;

    StarterCommand underTest;

    CustomDiceCommand customDiceCommand;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        underTest = new StarterCommand(persistenceManager, IncrementingUUIDSupplier.create(), customParameterCommand, customDiceCommand, sumCustomSetCommand);
    }

    @Test
    void testCommandId() {
        assertThat(underTest.getCommandId()).isEqualTo("starter");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EN", "DE", "FR", "pt_BR"})
    void testWelcomeMessage(String locale) {
        expect.scenario(locale).toMatchSnapshot(underTest.getWelcomeMessage().apply(new DiscordConnector.WelcomeRequest(null, 1, Locale.of(locale))));
    }

    @Test
    void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    void checkPersistence() {
        Supplier<UUID> incrementingUUIDs = IncrementingUUIDSupplier.create();
        UUID configUUID = incrementingUUIDs.get();
        StarterConfig config = new StarterConfig(configUUID, List.of(
                new StarterConfig.Command("dnd", incrementingUUIDs.get()),
                new StarterConfig.Command("calc", incrementingUUIDs.get())),
                Locale.ENGLISH,
                "starter message",
                "starter name",
                false);
        MessageConfigDTO toSave = underTest.createMessageConfig(configUUID, 2L, 1L, 0L, config);


        persistenceManager.saveMessageConfig(toSave);
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave).isEqualTo(loaded);
        StarterConfig loadedConfig = underTest.deserializeStarterMessage(loaded).orElseThrow();

        assertThat(loadedConfig).isEqualTo(config);
    }


    @Test
    void getAutoCompleteAnswer_OnlyPreset() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_1", "", List.of()), Locale.ENGLISH, 1L, 2L, 3L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_PresetFilter() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_1", "DnD", List.of()), Locale.ENGLISH, 1L, 2L, 3L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_NoDuplicate() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_2", "DnD", List.of(new OptionValue("command_name_2", "Dungeon & Dragons 5e"))), Locale.ENGLISH, 1L, 2L, 3L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_noAlias() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_2", "alias", List.of(new OptionValue("command_name_2", "Dungeon & Dragons 5e"))), Locale.ENGLISH, 1L, 2L, 3L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_NamedFromGuild() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000001"), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!_config_guild")
        ).orElseThrow());
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000002"), 2L, 2L, 2L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey2!_config_user")
        ).orElseThrow());
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_1", "", List.of()), Locale.ENGLISH, 3L, 1L, 3L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_NamedFromUser() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000001"), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!_config_guild")
        ).orElseThrow());
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000002"), 2L, 2L, 2L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey2!_config_user")
        ).orElseThrow());

        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_1", "", List.of()), Locale.ENGLISH, 3L, 3L, 2L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void getAutoCompleteAnswer_doubleName() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000001"), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "Dungeon & Dragons 5e")
        ).orElseThrow());
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(UUID.fromString("00000000-0000-0000-0000-000000000002"), 2L, 2L, 2L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "Dungeon & Dragons 5e")
        ).orElseThrow());
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("command_name_1", "Dungeon & Dragons 5e", List.of()), Locale.ENGLISH, 3L, 1L, 2L);

        expect.toMatchSnapshot(res);
    }

    @Test
    void deserialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        MessageConfigDTO savedData = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "starter", "StarterConfig",
                """
                        ---
                        id: "00000000-0000-0000-0000-000000000000"
                        commands:
                        - name: "dnd"
                          configUUID: "00000000-0000-0000-0001-000000000001"
                        - name: "calc"
                          configUUID: "00000000-0000-0000-0001-000000000002"
                        configLocale: "en"
                        message: "starter message"
                        name: "starter name"
                        startInNewMessage: false
                        """,
                "Name",
                1L
        );


        StarterConfig loadedConfig = underTest.deserializeStarterMessage(savedData).orElseThrow();
        assertThat(loadedConfig).isEqualTo(new StarterConfig(configUUID, List.of(
                new StarterConfig.Command("dnd", UUID.fromString("00000000-0000-0000-0001-000000000001")),
                new StarterConfig.Command("calc", UUID.fromString("00000000-0000-0000-0001-000000000002"))),
                Locale.ENGLISH,
                "starter message",
                "starter name",
                false));
    }

    @Test
    void configSerialization() {
        Supplier<UUID> incrementingUUIDs = IncrementingUUIDSupplier.create();

        UUID configUUID = incrementingUUIDs.get();
        StarterConfig config = new StarterConfig(configUUID, List.of(
                new StarterConfig.Command("dnd", incrementingUUIDs.get()),
                new StarterConfig.Command("calc", incrementingUUIDs.get())),
                Locale.ENGLISH,
                "starter message",
                "starter name",
                false);
        MessageConfigDTO toSave = underTest.createMessageConfig(configUUID, 1L, 2L, 3L, config);
        System.out.println(toSave);
        expect.toMatchSnapshot(toSave);
    }


}