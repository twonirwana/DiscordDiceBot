package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import lombok.Value;

import java.util.Locale;

@Value
public class CommandLocaleChoice {
    Locale locale;
    String choice;
    public CommandLocaleChoice(Locale locale, String choice) {
        this.locale = locale;
        this.choice = choice;
        Preconditions.checkArgument(choice.length() <= 100, "command choice to long: %s", choice);
    }
}
