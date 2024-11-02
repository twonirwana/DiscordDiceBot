package de.janno.discord.bot.command.starter;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ButtonIdLabelAndDiceExpression;
import de.janno.discord.bot.command.IncrementingUUIDSupplier;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.ComponentCommand;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(SnapshotExtension.class)
class StarterCommandMockTest {

    PersistenceManager persistenceManager;
    Expect expect;

    StarterCommand underTest;
    CustomDiceCommand customDiceCommand;
    CustomParameterCommand customParameterCommand;
    SumCustomSetCommand sumCustomSetCommand;
    Supplier<UUID> uuidSupplier;

    static Stream<Arguments> createPreset() {
        return Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .filter(p -> !ChannelConfigCommand.COMMAND_NAME.equals(p.getCommandId()))
                .flatMap(d -> I18n.allSupportedLanguage().stream()
                        .map(l -> Arguments.of(d, l)));
    }

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        uuidSupplier = IncrementingUUIDSupplier.create();
        persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        underTest = new StarterCommand(persistenceManager, uuidSupplier, customParameterCommand, customDiceCommand, sumCustomSetCommand);
    }

    @ParameterizedTest
    @MethodSource
    void createPreset(RpgSystemCommandPreset.PresetId presetId, Locale locale) {
        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("create")
                .option(CommandInteractionOption.builder()
                        .name("command_name_1")
                        .stringValue(presetId.getName(locale))
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("message")
                        .stringValue("custom name")
                        .build())
                .build()), 1L);
        underTest.handleSlashCommandEvent(slashEvent, uuidSupplier, locale).block();

        expect.scenario(presetId.name() + "_" + "event1" + "_" + locale).toMatchSnapshot(slashEvent.getSortedActions());

        ButtonEventAdaptorMock buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage().orElseThrow();

        underTest.handleComponentInteractEvent(buttonEventAdaptorMock).block();
        expect.scenario(presetId.name() + "_" + "event2" + "_" + locale).toMatchSnapshot(buttonEventAdaptorMock.getSortedActions());

        String buttonId = buttonEventAdaptorMock.getEditedComponentRowDefinition().getFirst().getButtonDefinitions().getFirst().getId();

        final ComponentCommand componentCommand = switch (presetId.getCommandId()) {
            case CustomDiceCommand.COMMAND_NAME -> customDiceCommand;
            case SumCustomSetCommand.COMMAND_NAME -> sumCustomSetCommand;
            case CustomParameterCommand.COMMAND_NAME -> customParameterCommand;
            case null, default -> throw new IllegalStateException();
        };

        ButtonEventAdaptorMock buttonEventAdaptorMock2 = ButtonEventAdaptorMock.ofCustomId(buttonId, buttonEventAdaptorMock.getMessageId());

        componentCommand.handleComponentInteractEvent(buttonEventAdaptorMock2).block();
        expect.scenario(presetId.name() + "_" + "event3" + "_" + locale).toMatchSnapshot(buttonEventAdaptorMock2.getSortedActions());
    }

    @Test
    void createWithUserNamed_sameMassage() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(uuidSupplier.get(), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!")
        ).orElseThrow());

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("create")
                .option(CommandInteractionOption.builder()
                        .name("command_name_1")
                        .stringValue("named_!uniqueKey1!")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("message")
                        .stringValue("custom name")
                        .build())
                .build()), 1L);
        underTest.handleSlashCommandEvent(slashEvent, uuidSupplier, Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());

        ButtonEventAdaptorMock buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage().orElseThrow();

        underTest.handleComponentInteractEvent(buttonEventAdaptorMock).block();
        expect.scenario("event2").toMatchSnapshot(buttonEventAdaptorMock.getSortedActions());

        String buttonId = buttonEventAdaptorMock.getEditedComponentRowDefinition().getFirst().getButtonDefinitions().getFirst().getId();

        ButtonEventAdaptorMock buttonEventAdaptorMock2 = ButtonEventAdaptorMock.ofCustomId(buttonId, buttonEventAdaptorMock.getMessageId());

        customDiceCommand.handleComponentInteractEvent(buttonEventAdaptorMock2).block();
        expect.scenario("event3").toMatchSnapshot(buttonEventAdaptorMock2.getSortedActions());
    }

    @Test
    void createWithUserNamed_newMassage() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(uuidSupplier.get(), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!")
        ).orElseThrow());

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("create")
                .option(CommandInteractionOption.builder()
                        .name("command_name_1")
                        .stringValue("named_!uniqueKey1!")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("message")
                        .stringValue("custom name")
                        .build())
                .option(CommandInteractionOption.builder()
                        .name("open_in_new_message")
                        .booleanValue(true)
                        .build())
                .build()), 1L);
        underTest.handleSlashCommandEvent(slashEvent, uuidSupplier, Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());

        ButtonEventAdaptorMock buttonEventAdaptorMock = slashEvent.getFirstButtonEventMockOfLastButtonMessage().orElseThrow();

        underTest.handleComponentInteractEvent(buttonEventAdaptorMock).block();
        expect.scenario("event2").toMatchSnapshot(buttonEventAdaptorMock.getSortedActions());

        String buttonId = buttonEventAdaptorMock.getSendMessages().getFirst().getComponentRowDefinitions().getFirst().getButtonDefinitions().getFirst().getId();

        ButtonEventAdaptorMock buttonEventAdaptorMock2 = ButtonEventAdaptorMock.ofCustomId(buttonId, buttonEventAdaptorMock.getMessageId());

        customDiceCommand.handleComponentInteractEvent(buttonEventAdaptorMock2).block();
        expect.scenario("event3").toMatchSnapshot(buttonEventAdaptorMock2.getSortedActions());
    }

    @Test
    void help() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(uuidSupplier.get(), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!")
        ).orElseThrow());

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("help")
                .build()), 1L);
        underTest.handleSlashCommandEvent(slashEvent, uuidSupplier, Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());
    }

    @Test
    void welcome() {
        persistenceManager.saveMessageConfig(customDiceCommand.createMessageConfig(uuidSupplier.get(), 1L, 1L, 1L, new CustomDiceConfig(null,
                List.of(new ButtonIdLabelAndDiceExpression("1", "Roll", "2d6", false, false, null)),
                AnswerFormatType.full, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.d6_dots, DiceImageStyle.d6_dots.getDefaultColor()),
                Locale.ENGLISH, null, "named_!uniqueKey1!")
        ).orElseThrow());

        SlashEventAdaptorMock slashEvent = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("welcome")
                .build()), 1L);
        underTest.handleSlashCommandEvent(slashEvent, uuidSupplier, Locale.ENGLISH).block();

        expect.scenario("event1").toMatchSnapshot(slashEvent.getSortedActions());
    }


}