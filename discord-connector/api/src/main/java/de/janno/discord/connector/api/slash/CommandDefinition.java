package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Builder
@Value
public class CommandDefinition {

    private final static Pattern NAME_PATTERN = Pattern.compile("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}$");
    @NonNull
    String name;
    @NonNull
    String description;
    @Singular
    List<CommandLocaleName> nameLocales;
    @Singular
    List<CommandLocaleDescription> descriptionLocales;
    @Singular
    List<CommandDefinitionOption> options;

    public CommandDefinition(@NonNull String name, @NonNull String description, List<CommandLocaleName> nameLocales, List<CommandLocaleDescription> descriptionLocales, List<CommandDefinitionOption> options) {
        this.name = name;
        this.description = description;
        this.nameLocales = nameLocales;
        this.descriptionLocales = descriptionLocales;
        this.options = options;

        //https://discord.com/developers/docs/interactions/application-commands#application-command-object-application-command-naming
        Preconditions.checkArgument(NAME_PATTERN.matcher(name).matches(), "Invalid command name: %s", name);
        Preconditions.checkArgument(name.toLowerCase(Locale.ROOT).equals(name), "Name must be lowercase only! Provided: \"%s\"", name);
        Preconditions.checkArgument(description.length() <= 100, "command description to long: %s", description);
        Preconditions.checkArgument(options.size() <= 25, "Too many options in %s, max is 25", options);
        List<String> duplicatedOptionNames = options.stream().map(CommandDefinitionOption::getName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        Preconditions.checkArgument(duplicatedOptionNames.isEmpty(), "The following optionName are not unique: %s", duplicatedOptionNames);
        Map<String, List<String>> duplicatedOptionLocaleNames = options.stream().flatMap(cd -> cd.getNameLocales().stream())
                .collect(Collectors.groupingBy(c -> c.getLocale().toString())).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, l -> l.getValue().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet().stream()
                        .filter(e -> e.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .map(CommandLocaleName::getName)
                        .toList()
                )).entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Preconditions.checkArgument(duplicatedOptionLocaleNames.isEmpty(), "The following optionName locale are not unique: %s", duplicatedOptionLocaleNames);
    }
}
