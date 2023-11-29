package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import lombok.Value;

import java.util.Locale;
import java.util.regex.Pattern;


@Value
public class CommandLocaleName {
    private final static Pattern NAME_PATTERN = Pattern.compile("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}$");

    Locale locale;
    String name;

    public CommandLocaleName(Locale locale, String name) {
        this.locale = locale;
        this.name = name;
        Preconditions.checkArgument(NAME_PATTERN.matcher(name).matches(), "Invalid command name: %s", name);
        Preconditions.checkArgument(name.toLowerCase(Locale.ROOT).equals(name), "Name must be lowercase only! Provided: \"%s\"", name);
    }
}
