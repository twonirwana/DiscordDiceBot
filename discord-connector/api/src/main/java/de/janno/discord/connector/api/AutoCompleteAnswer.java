package de.janno.discord.connector.api;

import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

@Value
public class AutoCompleteAnswer {
    @NonNull
    String name;
    @NonNull
    String value;

    public AutoCompleteAnswer(String name, String value) {
        this.name = StringUtils.abbreviate(name, 100);
        this.value = StringUtils.abbreviate(value, 100);
    }
}
