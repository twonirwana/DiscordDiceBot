package de.janno.discord.bot;


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
import de.janno.evaluator.dice.random.RandomNumberSupplier;
import io.avaje.config.Config;
import lombok.extern.slf4j.Slf4j;

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

        Set<Long> allGuildIdsInPersistence = persistenceManager.getAllGuildIds();
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
                        new QuickstartCommand(rpgSystemCommandPreset),
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
                allGuildIdsInPersistence);
    }

}
