package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import lombok.Value;

import java.util.Locale;

@Value
public class CommandLocaleDescription {
    Locale locale;
    String description;
    public CommandLocaleDescription(Locale locale, String description) {
        this.locale = locale;
        this.description = description;
        Preconditions.checkArgument(description.length() <= 100, "command description to long: %s", description);
    }
}
