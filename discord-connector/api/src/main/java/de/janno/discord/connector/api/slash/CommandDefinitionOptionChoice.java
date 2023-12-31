package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
public class CommandDefinitionOptionChoice {
    @NonNull
    String name;
    @NonNull
    String value;
    @Singular
    Set<CommandLocaleChoice> nameLocales; //limitations are like descriptions
    // https://discord.com/developers/docs/interactions/application-commands#application-command-object-application-command-option-choice-structure

    public CommandDefinitionOptionChoice(@NonNull String name, @NonNull String value, Set<CommandLocaleChoice> nameLocales) {
        this.name = name;
        this.value = value;
        this.nameLocales = nameLocales;
        Preconditions.checkArgument(name.length() <= 100, "command choice name to long: %s", name);
        Preconditions.checkArgument(value.length() <= 100, "command name value to long: %s", value);
        List<String> duplicatedOptionNames = nameLocales.stream().map(CommandLocaleChoice::getChoice)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        Preconditions.checkArgument(duplicatedOptionNames.isEmpty(), "The following name locales are not unique: %s", duplicatedOptionNames);
    }
}
