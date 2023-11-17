package de.janno.discord.connector.api.slash;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CommandDefinitionOptionChoice {
    String name;
    String value;
    @Singular
    List<LocaleValue> nameLocales;
}
