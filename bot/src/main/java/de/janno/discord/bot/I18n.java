package de.janno.discord.bot;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.slash.LocaleValue;
import org.apache.commons.lang3.StringEscapeUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public final class I18n {
    private final static String MESSAGES_KEY = "botMessages";

    private I18n() {
    }

    public static String getMessage(String key, Locale locale) {
        return StringEscapeUtils.unescapeJava(ResourceBundle.getBundle(MESSAGES_KEY, locale).getString(key));
    }

    public static String getMessage(String key, Locale locale, Object... arguments) {
        return MessageFormat.format(getMessage(key, locale), arguments);
    }

    public static List<Locale> getAdditionalLanguage() {
        return List.of(Locale.GERMAN);
    }

    public static List<Locale> allSupportedLanguage() {
        return ImmutableList.<Locale>builder()
                .add(Locale.ENGLISH)
                .addAll(getAdditionalLanguage())
                .build();
    }

    public static List<LocaleValue> allNoneEnglishMessages(String key) {
        return getAdditionalLanguage().stream()
                .map(l -> new LocaleValue(l, getMessage(key, l)))
                .filter(m -> !Objects.equals(m.value(), getMessage(key, Locale.ENGLISH))) //remove all locals that are equal to the default english one
                .toList();
    }

    public static List<LocaleValue> allNoneEnglishWithKeys(String key, String... keys) {
        return getAdditionalLanguage().stream()
                .map(l -> new LocaleValue(l, getMessage(key, l, Arrays.stream(keys)
                        .map(k -> getMessage(k, l))
                        .collect(Collectors.toList()))))
                .filter(m -> !Objects.equals(m.value(), getMessage(key, Locale.ENGLISH))) //remove all locals that are equal to the default english one
                .toList();
    }


}
