package de.janno.discord.bot;


import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesCommand;
import de.janno.discord.bot.command.customDice.CustomDiceCommand;
import de.janno.discord.bot.command.customParameter.CustomParameterCommand;
import de.janno.discord.bot.command.fate.FateCommand;
import de.janno.discord.bot.command.holdReroll.HoldRerollCommand;
import de.janno.discord.bot.command.poolTarget.PoolTargetCommand;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetCommand;
import de.janno.discord.bot.command.sumDiceSet.SumDiceSetCommand;
import de.janno.discord.connector.DiscordConnector;

public class Bot {
    public static void main(final String[] args) throws Exception {
        final String token = args[0];
        final boolean disableCommandUpdate = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        BotMetrics.init(publishMetricsToUrl);

        DiscordConnector.createAndStart(token, disableCommandUpdate, ImmutableList.of(
                        new CountSuccessesCommand(),
                        new CustomDiceCommand(),
                        new FateCommand(),
                        new DirectRollCommand(),
                        new SumDiceSetCommand(),
                        new SumCustomSetCommand(),
                        new HoldRerollCommand(),
                        new PoolTargetCommand(),
                        new CustomParameterCommand(),
                        new WelcomeCommand(),
                        new HelpCommand()
                ),
                new WelcomeCommand().getWelcomeMessage());
    }

}
