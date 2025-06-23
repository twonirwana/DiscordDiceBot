package de.janno.discord.bot;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import de.janno.discord.bot.command.ClearCommand;
import de.janno.discord.bot.command.FetchCommand;
import de.janno.discord.bot.command.LegacyIdHandler;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.directRoll.AliasRollCommand;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.command.directRoll.HiddenDirectRollCommand;
import de.janno.discord.bot.command.directRoll.ValidationCommand;
import de.janno.discord.bot.command.help.HelpCommand;
import de.janno.discord.bot.command.help.QuickstartCommand;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.help.WelcomeCommand;
import de.janno.discord.bot.command.reroll.RerollAnswerHandler;
import de.janno.discord.bot.command.starter.StarterCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.DiscordConnectorImpl;
import de.janno.discord.connector.api.ChildrenChannelCreationEvent;
import de.janno.discord.connector.api.DatabaseConnector;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import io.avaje.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

@Slf4j
public class Bot {

    public static void main(final String[] args) throws Exception {
        if (args != null && args.length > 0) {
            final String token = args[0];
            if (!Strings.isNullOrEmpty(token)) {
                log.info("using token from program argument");
                Config.setProperty("token", token);
            }
        }
        BotMetrics.init();
        Config.onChange(event -> event.modifiedKeys().forEach(k -> log.info("config change: {} -> {}", k, event.configuration().getOptional(k).orElse(""))));

        final String url = Config.get("db.url", "jdbc:h2:file:./persistence/dice_config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=5");
        final String user = Config.getNullable("db.user");
        final String password = Config.getNullable("db.password");
        PersistenceManager persistenceManager = new PersistenceManagerImpl(url, user, password);

        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier());

        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        ChannelConfigCommand channelConfigCommand = new ChannelConfigCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        WelcomeCommand welcomeCommand = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset);
        HiddenDirectRollCommand hiddenDirectRollCommand = new HiddenDirectRollCommand(persistenceManager, cachingDiceEvaluator);
        RerollAnswerHandler rerollAnswerHandler = new RerollAnswerHandler(persistenceManager, cachingDiceEvaluator);
        StarterCommand starterCommand = new StarterCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);
        QuickstartCommand quickstartCommand = new QuickstartCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand, channelConfigCommand);
        DiscordConnectorImpl.createAndStart(
                List.of(customDiceCommand,
                        new DirectRollCommand(persistenceManager, cachingDiceEvaluator),
                        new AliasRollCommand(persistenceManager, cachingDiceEvaluator),
                        hiddenDirectRollCommand,
                        new ValidationCommand(persistenceManager, cachingDiceEvaluator),
                        channelConfigCommand,
                        sumCustomSetCommand,
                        customParameterCommand,
                        new ClearCommand(persistenceManager),
                        quickstartCommand,
                        new HelpCommand(),
                        new FetchCommand(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand),
                        starterCommand
                ),
                List.of(customDiceCommand,
                        sumCustomSetCommand,
                        customParameterCommand,
                        welcomeCommand,
                        hiddenDirectRollCommand,
                        rerollAnswerHandler,
                        new LegacyIdHandler(),
                        starterCommand
                ),
                starterCommand.getWelcomeMessage(),
                new DatabaseConnector() {
                    @Override
                    public void markDataOfMissingGuildsToDelete(Set<Long> allGuildIdsAtStartup) {
                        markAlreadyLeavedGuildsAsDeleted(persistenceManager, allGuildIdsAtStartup);
                    }

                    @Override
                    public void markDataOfLeavingGuildsToDelete(long leavingGuildId) {
                        long markDeleteCount = persistenceManager.markDeleteAllForGuild(List.of(leavingGuildId));
                        BotMetrics.incrementMarkAsDeleteAfterLeavingBot(markDeleteCount);
                    }

                    @Override
                    public void unmarkDataOfJoiningGuilds(long joiningGuildId) {
                        long unmarkCount = persistenceManager.undoMarkDelete(joiningGuildId);
                        if (unmarkCount > 0) {
                            log.info("Unmark configs after rejoin: {}", unmarkCount);
                        }
                        BotMetrics.unmarkDeleteAfterRejoin(unmarkCount);
                    }

                    @Override
                    public void copyChildChannel(ChildrenChannelCreationEvent childrenChannelCreationEvent) {
                        persistenceManager.copyChannelConfig(childrenChannelCreationEvent);
                    }
                });
    }


    @VisibleForTesting
    static void markAlreadyLeavedGuildsAsDeleted(PersistenceManager persistenceManager, Set<Long> guildIdsAtStartup) {

        if (guildIdsAtStartup.isEmpty()) {
            log.info("not guilds in startup");
            return;
        }

        Set<Long> allGuildIdsInPersistence = persistenceManager.getAllGuildIds();

        if (allGuildIdsInPersistence.isEmpty()) {
            log.error("No existing guilds found");
            return;
        }
        List<Long> guildsToDelete = allGuildIdsInPersistence.stream()
                .filter(g -> !guildIdsAtStartup.contains(g))
                .toList();

        BigDecimal ratioToDelete = BigDecimal.valueOf(guildsToDelete.size()).divide(BigDecimal.valueOf(allGuildIdsInPersistence.size()), 2, RoundingMode.HALF_UP);
        BigDecimal maxRatioToDelete = Config.getDecimal("maxGuildCleanUpRatio", "0.1");
        if (ratioToDelete.compareTo(maxRatioToDelete) > 0) {
            log.error("Number of guilds to delete: {}, number of guilds in persistence: {}, the ratio is {} but it must be greater then {}", guildsToDelete.size(), allGuildIdsInPersistence.size(), ratioToDelete, maxRatioToDelete);
            return;
        }

        log.info("Start marking data of inactive {} guilds", guildsToDelete.size());
        long totalDeletes = persistenceManager.markDeleteAllForGuild(guildsToDelete);
        log.info("Finished marking {} objects from inactive guilds", totalDeletes);
        BotMetrics.incrementMarkAsDeleteAfterLeavingBot(totalDeletes);

    }

}
