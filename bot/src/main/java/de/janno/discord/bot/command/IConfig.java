package de.janno.discord.bot.command;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
public interface IConfig {

    String toShortString();

    default String targetChannelToString(Long answerTargetChannel){
        if(answerTargetChannel == null){
            return "local";
        }
        return "target";
    }

}
