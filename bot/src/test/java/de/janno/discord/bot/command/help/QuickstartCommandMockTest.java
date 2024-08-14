package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.IncrementingUUIDSupplier;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.api.SlashCommand;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class QuickstartCommandMockTest {

    Expect expect;

    static Stream<Arguments> generateRpgSystemLocaleData() {
        return Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .flatMap(d -> I18n.allSupportedLanguage().stream()
                        .map(l -> Arguments.of(d, l)));
    }

    private static List<CommandInteractionOption> getOptionsFromString(String commandStringOptions, SlashCommand command) {
        commandStringOptions = commandStringOptions.trim();
        List<String> commandStartOptions = command.getCommandDefinition().getOptions().stream()
                .filter(o -> o.getName().equals("start"))
                .flatMap(o -> o.getOptions().stream())
                .map(CommandDefinitionOption::getName)
                .toList();

        List<Integer> indexToSplit = commandStartOptions.stream()
                .map(commandStringOptions::indexOf)
                .sorted()
                .filter(i -> i > 0)
                .toList();

        int lastIndex = 0;
        List<String> commandSplitAtOptions = new ArrayList<>();
        for (int point : indexToSplit) {
            commandSplitAtOptions.add(commandStringOptions.substring(lastIndex, point));
            lastIndex = point;
        }

        return commandSplitAtOptions.stream()
                .map(s -> CommandInteractionOption.builder()
                        .name(s.substring(0, s.indexOf(":")).trim())
                        .stringValue(s.substring(s.indexOf(":") + 1).trim())
                        .build())
                .toList();

    }

    static Stream<Arguments> aliasRollData() {
        return Stream.of(
                Arguments.of(RpgSystemCommandPreset.PresetId.FATE_ALIAS, "4dF"),
                Arguments.of(RpgSystemCommandPreset.PresetId.DND5_ALIAS, "adv"),
                Arguments.of(RpgSystemCommandPreset.PresetId.NWOD_ALIAS, "8w"),
                Arguments.of(RpgSystemCommandPreset.PresetId.OWOD_ALIAS, "8r6"),
                Arguments.of(RpgSystemCommandPreset.PresetId.OWOD_ALIAS, "10re6"),
                Arguments.of(RpgSystemCommandPreset.PresetId.SHADOWRUN_ALIAS, "10sr"),
                Arguments.of(RpgSystemCommandPreset.PresetId.SAVAGE_WORLDS_ALIAS, "r4"),
                Arguments.of(RpgSystemCommandPreset.PresetId.SAVAGE_WORLDS_ALIAS, "sw4"),
                Arguments.of(RpgSystemCommandPreset.PresetId.BLADES_IN_THE_DARK_ALIAS, "0b"),
                Arguments.of(RpgSystemCommandPreset.PresetId.BLADES_IN_THE_DARK_ALIAS, "5b")
        );
    }

    @ParameterizedTest(name = "{index} config={0}, locale={1}")
    @MethodSource("generateRpgSystemLocaleData")
    void handleSlashCommandEvent(RpgSystemCommandPreset.PresetId presetId, Locale userLocale) {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        QuickstartCommand underTest = new QuickstartCommand(rpgSystemCommandPreset);

        SlashEventAdaptorMock slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("system")
                .stringValue(presetId.name())
                .build()));

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), userLocale);
        StepVerifier.create(res).verifyComplete();

        expect.scenario(presetId.name() + "_" + userLocale).toMatchSnapshot(slashEventAdaptor.getSortedActions());
        expect.scenario(presetId.name() + "_alias_" + userLocale).toMatchSnapshot(AliasHelper.getChannelAlias(SlashEventAdaptorMock.CHANNEL_ID, persistenceManager));
    }

    @ParameterizedTest(name = "{index} system={0}, r={1}")
    @MethodSource("aliasRollData")
    void quickstartAlias_directRoll(RpgSystemCommandPreset.PresetId presetId, String rExpression) {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0));
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        DirectRollCommand directRollCommand = new DirectRollCommand(persistenceManager, cachingDiceEvaluator);
        QuickstartCommand underTest = new QuickstartCommand(rpgSystemCommandPreset);

        SlashEventAdaptorMock quickstartAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("system")
                .stringValue(presetId.name())
                .build()));

        StepVerifier.create(underTest.handleSlashCommandEvent(quickstartAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH)).verifyComplete();
        expect.scenario(presetId.name() + "_" + rExpression + "_quickstart").toMatchSnapshot(quickstartAdaptor.getSortedActions());

        SlashEventAdaptorMock directRollAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("expression")
                .stringValue(rExpression)
                .build()), Locale.ENGLISH);

        StepVerifier.create(directRollCommand.handleSlashCommandEvent(directRollAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000001"), Locale.ENGLISH)).verifyComplete();

        expect.scenario(presetId.name() + "_" + rExpression + "_directRoll").toMatchSnapshot(directRollAdaptor.getSortedActions());
    }

    @ParameterizedTest(name = "{index} config={0}, locale={1}")
    @MethodSource("generateRpgSystemLocaleData")
    void config2CommandString_slashCommand_firstButton(RpgSystemCommandPreset.PresetId presetId, Locale userLocale) {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator((minExcl, maxIncl) -> minExcl + 1);
        Supplier<UUID> uuidSupplier = IncrementingUUIDSupplier.create();
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator, uuidSupplier);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);

        String command = RpgSystemCommandPreset.getCommandString(presetId, userLocale);
        SlashEventAdaptorMock slashEventAdaptor;
        Mono<Void> slashRes;
        if (command.startsWith("/custom_dice start")) {
            String commandOptions = command.substring(18);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, customDiceCommand))
                    .build()), userLocale);
            slashRes = customDiceCommand.handleSlashCommandEvent(slashEventAdaptor, uuidSupplier, userLocale);
        } else if (command.startsWith("/custom_parameter start")) {
            String commandOptions = command.substring(23);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, customParameterCommand))
                    .build()), userLocale);
            slashRes = customParameterCommand.handleSlashCommandEvent(slashEventAdaptor, uuidSupplier, userLocale);
        } else if (command.startsWith("/sum_custom_set start")) {
            String commandOptions = command.substring(21);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, sumCustomSetCommand))
                    .build()), userLocale);
            slashRes = sumCustomSetCommand.handleSlashCommandEvent(slashEventAdaptor, uuidSupplier, userLocale);
        } else if (command.startsWith("/channel_config alias multi_save aliases")) {
            String aliases = command.substring(41, command.indexOf("scope:") - 1);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("alias")
                    .option(CommandInteractionOption.builder()
                            .name("multi_save")
                            .option(CommandInteractionOption.builder()
                                    .name("aliases")
                                    .stringValue(aliases)
                                    .build())
                            .build())
                    .option(CommandInteractionOption.builder()
                            .name("scope")
                            .stringValue("all_users_in_this_channel")
                            .build())
                    .build()), userLocale);
            slashRes = channelConfigCommand.handleSlashCommandEvent(slashEventAdaptor, uuidSupplier, userLocale);
        } else {
            throw new IllegalStateException("Unknown command for " + presetId);
        }

        StepVerifier.create(slashRes).verifyComplete();

        expect.scenario("slashCommand:" + presetId.name() + "_" + userLocale).toMatchSnapshot(slashEventAdaptor.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEventAdaptor.getFirstButtonEventMockOfLastButtonMessage();

        Mono<Void> buttonRes;
        if (command.startsWith("/custom_dice start")) {
            assertThat(buttonEventAdaptorMock).isPresent();
            buttonRes = customDiceCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
            StepVerifier.create(buttonRes).verifyComplete();
            expect.scenario("firstButtonEvent:" + presetId.name() + "_" + userLocale).toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
        } else if (command.startsWith("/custom_parameter start")) {
            assertThat(buttonEventAdaptorMock).isPresent();
            buttonRes = customParameterCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
            StepVerifier.create(buttonRes).verifyComplete();
            expect.scenario("firstButtonEvent:" + presetId.name() + "_" + userLocale).toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
        } else if (command.startsWith("/sum_custom_set start")) {
            assertThat(buttonEventAdaptorMock).isPresent();
            buttonRes = sumCustomSetCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
            StepVerifier.create(buttonRes).verifyComplete();
            expect.scenario("firstButtonEvent:" + presetId.name() + "_" + userLocale).toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());
        } else if (command.startsWith("/channel_config alias multi_save")) {
            assertThat(buttonEventAdaptorMock).isEmpty();
            expect.scenario("alias:" + presetId.name() + "_" + userLocale).toMatchSnapshot(AliasHelper.getChannelAlias(SlashEventAdaptorMock.CHANNEL_ID, persistenceManager));
        } else {
            throw new IllegalStateException("Unknown command for " + presetId);
        }


    }
}
