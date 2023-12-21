package de.janno.discord.connector.jda;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

@Slf4j
public final class I18n {
    private final static String MESSAGES_KEY = "discordConnector";

    private I18n() {
    }

    public static String getMessage(String key, Locale locale) {
        return StringEscapeUtils.unescapeJava(ResourceBundle.getBundle(MESSAGES_KEY, locale).getString(key));
    }

    public static String getMessage(String key, Locale locale, Object... arguments) {
        return MessageFormat.format(getMessage(key, locale), arguments);
    }

}
