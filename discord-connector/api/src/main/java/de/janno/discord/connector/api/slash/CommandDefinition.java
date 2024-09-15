package de.janno.discord.connector.api.slash;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.*;
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
    Set<CommandLocaleName> nameLocales;
    @Singular
    Set<CommandLocaleDescription> descriptionLocales;
    @Singular
    List<CommandDefinitionOption> options;
    @Singular
    SortedSet<CommandIntegrationType> integrationTypes;

    public CommandDefinition(@NonNull String name,
                             @NonNull String description,
                             Set<CommandLocaleName> nameLocales,
                             Set<CommandLocaleDescription> descriptionLocales,
                             List<CommandDefinitionOption> options,
                             SortedSet<CommandIntegrationType> integrationTypes) {
        this.name = name;
        this.description = description;
        this.nameLocales = nameLocales;
        this.descriptionLocales = descriptionLocales;
        this.options = options;
        this.integrationTypes = (integrationTypes == null || integrationTypes.isEmpty()) ? ImmutableSortedSet.of(CommandIntegrationType.GUILD_INSTALL) : integrationTypes;

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
