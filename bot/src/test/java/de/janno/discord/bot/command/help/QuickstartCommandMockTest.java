package de.janno.discord.bot.command.help;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.SlashEventAdaptorMock;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class QuickstartCommandMockTest {

    private Expect expect;

    static Stream<Arguments> generateRpgSystemLocaleData() {
        return Arrays.stream(RpgSystemCommandPreset.PresetId.values())
                .flatMap(d -> I18n.allSupportedLanguage().stream()
                        .map(l -> Arguments.of(d, l)));
    }

    private static List<CommandInteractionOption> getOptionsFromString(String commandStringOptions, AbstractCommand<?, ?> command) {
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

    @ParameterizedTest(name = "{index} config={0}, locale={1}")
    @MethodSource("generateRpgSystemLocaleData")
    void handleSlashCommandEvent(RpgSystemCommandPreset.PresetId presetId, Locale userLocale) {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(0), 1000, 0);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        QuickstartCommand underTest = new QuickstartCommand(rpgSystemCommandPreset);

        SlashEventAdaptorMock slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                .name("system")
                .stringValue(presetId.name())
                .build()));

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), userLocale);
        StepVerifier.create(res).verifyComplete();

        expect.scenario(presetId.name() + "_" + userLocale).toMatchSnapshot(slashEventAdaptor.getSortedActions());
    }

    @ParameterizedTest(name = "{index} config={0}, locale={1}")
    @MethodSource("generateRpgSystemLocaleData")
    void config2CommandString_slashCommand_firstButton(RpgSystemCommandPreset.PresetId presetId, Locale userLocale) {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator((minExcl, maxIncl) -> minExcl + 1, 1000, 0);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);

        String command = RpgSystemCommandPreset.getCommandString(presetId, userLocale);
        SlashEventAdaptorMock slashEventAdaptor;
        Mono<Void> slashRes;
        if (command.startsWith("/custom_dice start")) {
            String commandOptions = command.substring(18);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, customDiceCommand))
                    .build()), userLocale);
            slashRes = customDiceCommand.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), userLocale);
        } else if (command.startsWith("/custom_parameter start")) {
            String commandOptions = command.substring(23);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, customParameterCommand))
                    .build()), userLocale);
            slashRes = customParameterCommand.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), userLocale);
        } else if (command.startsWith("/sum_custom_set start")) {
            String commandOptions = command.substring(21);
            slashEventAdaptor = new SlashEventAdaptorMock(List.of(CommandInteractionOption.builder()
                    .name("start")
                    .options(getOptionsFromString(commandOptions, sumCustomSetCommand))
                    .build()), userLocale);
            slashRes = sumCustomSetCommand.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), userLocale);
        } else {
            throw new IllegalStateException("Unknown command for " + presetId);
        }

        StepVerifier.create(slashRes).verifyComplete();
        assertThat(slashEventAdaptor.getSortedActions().stream()).anyMatch(s -> s.startsWith("createMessageWithoutReference")); //at least on button Message needs to be crated

        expect.scenario("slashCommand:" + presetId.name() + "_" + userLocale).toMatchSnapshot(slashEventAdaptor.getSortedActions());

        Optional<ButtonEventAdaptorMock> buttonEventAdaptorMock = slashEventAdaptor.getFirstButtonEventMockOfLastButtonMessage();
        assertThat(buttonEventAdaptorMock).isPresent();

        Mono<Void> buttonRes;
        if (command.startsWith("/custom_dice start")) {
            buttonRes = customDiceCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
        } else if (command.startsWith("/custom_parameter start")) {
            buttonRes = customParameterCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
        } else if (command.startsWith("/sum_custom_set start")) {
            buttonRes = sumCustomSetCommand.handleComponentInteractEvent(buttonEventAdaptorMock.get());
        } else {
            throw new IllegalStateException("Unknown command for " + presetId);
        }
        StepVerifier.create(buttonRes).verifyComplete();
        expect.scenario("firstButtonEvent:" + presetId.name() + "_" + userLocale).toMatchSnapshot(buttonEventAdaptorMock.get().getSortedActions());


    }
}
