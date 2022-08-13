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
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDAOImpl;
import de.janno.discord.connector.DiscordConnector;

public class Bot {
    public static void main(final String[] args) throws Exception {
        final String token = args[0];
        final boolean disableCommandUpdate = Boolean.parseBoolean(args[1]);
        final String publishMetricsToUrl = args[2];
        BotMetrics.init(publishMetricsToUrl);
        //Todo url as args
        String url = "jdbc:h2:file:./persistence/dice_config";

        MessageDataDAO messageDataDAO = new MessageDataDAOImpl(url, null, null);

        DiscordConnector.createAndStart(token, disableCommandUpdate, ImmutableList.of(
                        new CountSuccessesCommand(messageDataDAO),
                        new CustomDiceCommand(messageDataDAO),
                        new FateCommand(messageDataDAO),
                        new DirectRollCommand(),
                        new SumDiceSetCommand(messageDataDAO),
                        new SumCustomSetCommand(messageDataDAO),
                        new HoldRerollCommand(messageDataDAO),
                        new PoolTargetCommand(messageDataDAO),
                        new CustomParameterCommand(messageDataDAO),
                        new WelcomeCommand(messageDataDAO),
                        new HelpCommand()
                ),
                new WelcomeCommand(messageDataDAO).getWelcomeMessage());
    }

}
