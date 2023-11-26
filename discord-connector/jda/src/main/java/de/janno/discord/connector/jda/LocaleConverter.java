package de.janno.discord.connector.jda;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.util.Locale;

public final class LocaleConverter {

    public static Locale toLocale(DiscordLocale discordLocale){
        String localeString = discordLocale.getLocale();
        if(localeString.contains("-")){
            String[] splitt = localeString.split("-");
            return  Locale.of(splitt[0], splitt[1]);
        }
        return Locale.of(localeString);
    }
}
