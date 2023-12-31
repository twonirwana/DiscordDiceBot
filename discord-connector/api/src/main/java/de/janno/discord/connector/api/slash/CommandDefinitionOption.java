package de.janno.discord.connector.api.slash;


import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Value
@Builder
public class CommandDefinitionOption {

    private final static Pattern NAME_PATTERN = Pattern.compile("^[-_\\p{L}\\p{N}\\p{sc=Deva}\\p{sc=Thai}]{1,32}$");
    Type type;
    String name;
    @Singular
    Set<CommandLocaleName> nameLocales;
    String description;
    @Singular
    Set<CommandLocaleDescription> descriptionLocales;
    boolean required;
    @Singular
    List<CommandDefinitionOptionChoice> choices;
    @Singular
    List<CommandDefinitionOption> options;
    Long minValue;
    Long maxValue;
    boolean autoComplete;

    public CommandDefinitionOption(Type type, String name, Set<CommandLocaleName> nameLocales, String description,
                                   Set<CommandLocaleDescription> descriptionLocales, Boolean required, List<CommandDefinitionOptionChoice> choices,
                                   List<CommandDefinitionOption> options, Long minValue, Long maxValue, Boolean autoComplete) {
        this.type = type;
        this.name = name;
        this.nameLocales = nameLocales;
        this.description = description;
        this.descriptionLocales = descriptionLocales;
        this.required = Optional.ofNullable(required).orElse(false);
        this.choices = choices;
        this.options = options;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.autoComplete = Optional.ofNullable(autoComplete).orElse(false);

        //https://discord.com/developers/docs/interactions/application-commands#application-command-object-application-command-naming
        Preconditions.checkArgument(NAME_PATTERN.matcher(name).matches(), "Invalid command name: %s", name);
        Preconditions.checkArgument(name.toLowerCase(Locale.ROOT).equals(name), "Name must be lowercase only! Provided: \"%s\"", name);
        Preconditions.checkArgument(description.length() <= 100, "command description to long: %s", description);
        Preconditions.checkArgument(options.size() <= 25, "Too many options in {}, max is 25", options);
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

        Preconditions.checkArgument(choices.size() <= 25, "Too many choices in %s, max is 25", choices);
        List<String> duplicatedChoicesNames = choices.stream().map(CommandDefinitionOptionChoice::getName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        Preconditions.checkArgument(duplicatedChoicesNames.isEmpty(), "The following choicesName are not unique: %s", duplicatedChoicesNames);
        Map<String, List<String>> duplicatedChoiceLocaleNames = choices.stream().flatMap(cd -> cd.getNameLocales().stream())
                .collect(Collectors.groupingBy(c -> c.getLocale().toString())).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, l -> l.getValue().stream()
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                        .entrySet().stream()
                        .filter(e -> e.getValue() > 1)
                        .map(Map.Entry::getKey)
                        .map(CommandLocaleChoice::getChoice)
                        .toList()
                )).entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Preconditions.checkArgument(duplicatedChoiceLocaleNames.isEmpty(), "The following choicesName locale are not unique: %s", duplicatedChoiceLocaleNames);
    }

    @Getter
    public enum Type {
        UNKNOWN(-1),
        SUB_COMMAND(1),
        SUB_COMMAND_GROUP(2),
        STRING(3),
        INTEGER(4),
        BOOLEAN(5),
        USER(6),
        CHANNEL(7),
        ROLE(8),
        MENTIONABLE(9),
        NUMBER(10),
        ATTACHMENT(11);

        private final int value;

        Type(final int value) {
            this.value = value;
        }

        public static Type of(final int value) {
            Arrays.stream(Type.values())
                    .filter(t -> t.value == value)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(String.format("Unknown type: %s", value)));
            return switch (value) {
                case 1 -> SUB_COMMAND;
                case 2 -> SUB_COMMAND_GROUP;
                case 3 -> STRING;
                case 4 -> INTEGER;
                case 5 -> BOOLEAN;
                case 6 -> USER;
                case 7 -> CHANNEL;
                case 8 -> ROLE;
                case 9 -> MENTIONABLE;
                case 10 -> NUMBER;
                case 11 -> ATTACHMENT;
                default -> UNKNOWN;
            };
        }

    }

}
