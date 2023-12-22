package de.janno.discord.bot;


import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.ClearCommand;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.directRoll.DirectRollCommand;
import de.janno.discord.bot.command.directRoll.HiddenDirectRollCommand;
import de.janno.discord.bot.command.directRoll.ValidationCommand;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.help.HelpCommand;
import de.janno.discord.bot.command.help.QuickstartCommand;
import de.janno.discord.bot.command.help.RpgSystemCommandPreset;
import de.janno.discord.bot.command.help.WelcomeCommand;
import de.janno.discord.bot.command.holdReroll.HoldRerollCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumDiceSet.SumDiceSetCommand;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.bot.persistance.PersistenceManagerImpl;
import de.janno.discord.connector.DiscordConnectorImpl;
import de.janno.evaluator.dice.random.RandomNumberSupplier;

import java.util.Set;

public class Bot {
    private final static String DEFAULT_ARG = "default"; //to skip this optional argument

    public static void main(final String[] args) throws Exception {
        final String token = args[0];
        final boolean disableCommandUpdate = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        BotMetrics.init(publishMetricsToUrl, 8080);

        final String h2Url;
        if (args.length >= 4 && !DEFAULT_ARG.equals(args[3])) {
            h2Url = args[3];
        } else {
            h2Url = "jdbc:h2:file:./persistence/dice_config;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=10";
        }
        final String h2User;
        if (args.length >= 5 && !DEFAULT_ARG.equals(args[4])) {
            h2User = args[4];
        } else {
            h2User = null;
        }
        final String h2Password;
        if (args.length >= 6 && !DEFAULT_ARG.equals(args[5])) {
            h2Password = args[5];
        } else {
            h2Password = null;
        }

        final String newsGuildId;
        if (args.length >= 7 && !DEFAULT_ARG.equals(args[6])) {
            newsGuildId = args[6];
        } else {
            newsGuildId = null;
        }

        String newsChannelId;
        if (args.length >= 8 && !DEFAULT_ARG.equals(args[7])) {
            newsChannelId = args[7];
        } else {
            newsChannelId = null;
        }
        PersistenceManager persistenceManager = new PersistenceManagerImpl(h2Url, h2User, h2Password);

        Set<Long> allGuildIdsInPersistence = persistenceManager.getAllGuildIds();
        CachingDiceEvaluator cachingDiceEvaluator = new CachingDiceEvaluator(new RandomNumberSupplier(), 1000, 10_000);

        CountSuccessesCommand countSuccessesCommand = new CountSuccessesCommand(persistenceManager);
        CustomDiceCommand customDiceCommand = new CustomDiceCommand(persistenceManager, cachingDiceEvaluator);
        FateCommand fateCommand = new FateCommand(persistenceManager);
        CustomParameterCommand customParameterCommand = new CustomParameterCommand(persistenceManager, cachingDiceEvaluator);
        SumCustomSetCommand sumCustomSetCommand = new SumCustomSetCommand(persistenceManager, cachingDiceEvaluator);
        PoolTargetCommand poolTargetCommand = new PoolTargetCommand(persistenceManager);
        RpgSystemCommandPreset rpgSystemCommandPreset = new RpgSystemCommandPreset(persistenceManager, customParameterCommand, customDiceCommand, sumCustomSetCommand);

        WelcomeCommand welcomeCommand = new WelcomeCommand(persistenceManager, rpgSystemCommandPreset);

        DiscordConnectorImpl.createAndStart(token,
                disableCommandUpdate,
                ImmutableList.of(countSuccessesCommand,
                        customDiceCommand,
                        fateCommand,
                        new DirectRollCommand(persistenceManager, cachingDiceEvaluator),
                        new HiddenDirectRollCommand(persistenceManager, cachingDiceEvaluator),
                        new ValidationCommand(persistenceManager, cachingDiceEvaluator),
                        new ChannelConfigCommand(persistenceManager),
                        new SumDiceSetCommand(persistenceManager),
                        sumCustomSetCommand,
                        new HoldRerollCommand(persistenceManager),
                        poolTargetCommand,
                        customParameterCommand,
                        welcomeCommand,
                        new ClearCommand(persistenceManager),
                        new QuickstartCommand(rpgSystemCommandPreset),
                        new HelpCommand()
                ),
                welcomeCommand.getWelcomeMessage(),
                allGuildIdsInPersistence,
                newsGuildId,
                newsChannelId);
    }

}
