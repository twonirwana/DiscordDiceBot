package de.janno.discord.bot;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.slash.CommandLocaleChoice;
import de.janno.discord.connector.api.slash.CommandLocaleDescription;
import de.janno.discord.connector.api.slash.CommandLocaleName;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;

import java.text.MessageFormat;
import java.util.*;

@Slf4j
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
        return List.of(Locale.GERMAN, Locale.of("pt","BR"), Locale.FRENCH);
    }

    public static List<Locale> allSupportedLanguage() {
        return ImmutableList.<Locale>builder()
                .add(Locale.ENGLISH)
                .addAll(getAdditionalLanguage())
                .build();
    }

    private static List<LocaleString> allNoneEnglishMessagesValues(String key) {
        return getAdditionalLanguage().stream()
                .map(l -> new LocaleString(l, getMessage(key, l)))
                .filter(m -> !Objects.equals(m.value(), getMessage(key, Locale.ENGLISH))) //remove all locals that are equal to the default english one
                .toList();
    }

    public static List<CommandLocaleName> allNoneEnglishMessagesNames(String key) {
        return allNoneEnglishMessagesValues(key).stream()
                .map(lv -> new CommandLocaleName(lv.locale(), lv.value()))
                .toList();
    }

    public static List<CommandLocaleDescription> allNoneEnglishMessagesDescriptions(String key) {
        return allNoneEnglishMessagesValues(key).stream()
                .map(lv -> new CommandLocaleDescription(lv.locale(), lv.value()))
                .toList();
    }

    public static List<CommandLocaleChoice> allNoneEnglishMessagesChoices(String key) {
        return allNoneEnglishMessagesValues(key).stream()
                .map(lv -> new CommandLocaleChoice(lv.locale(), lv.value()))
                .toList();
    }

    public static List<CommandLocaleDescription> allNoneEnglishDescriptionsWithKeys(String key, String... keys) {
        return getAdditionalLanguage().stream()
                .map(l -> new CommandLocaleDescription(l, getMessage(key, l, Arrays.stream(keys)
                        .map(k -> getMessage(k, l))
                        .toArray(Object[]::new))))
                .filter(m -> !Objects.equals(m.getDescription(), getMessage(key, Locale.ENGLISH))) //remove all locals that are equal to the default english one
                .toList();
    }

    private record LocaleString(Locale locale, String value){}

}
