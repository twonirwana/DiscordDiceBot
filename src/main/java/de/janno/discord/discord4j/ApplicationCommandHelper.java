package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableList;
import de.janno.discord.command.slash.CommandDefinition;
import de.janno.discord.command.slash.CommandDefinitionOption;
import de.janno.discord.command.slash.CommandDefinitionOptionChoice;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.possible.Possible;

import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationCommandHelper {

    public static CommandDefinition applicationCommandData2CommandDefinition(ApplicationCommandData applicationCommandData) {
        return CommandDefinition.builder()
                .name(applicationCommandData.name())
                .description(applicationCommandData.description())
                .options(applicationCommandData.options()
                        .toOptional().orElse(ImmutableList.of()).stream()
                        .map(ApplicationCommandHelper::applicationCommandOptionData2CommandDefinitionOption).collect(Collectors.toList()))
                .build();
    }

    private static CommandDefinitionOption applicationCommandOptionData2CommandDefinitionOption(ApplicationCommandOptionData applicationCommandOptionData) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.of(applicationCommandOptionData.type()))
                .name(applicationCommandOptionData.name())
                .description(applicationCommandOptionData.description())
                .required(applicationCommandOptionData.required().toOptional().orElse(false))
                .minValue(applicationCommandOptionData.minValue().toOptional().orElse(null))
                .maxValue(applicationCommandOptionData.maxValue().toOptional().orElse(null))
                .options(applicationCommandOptionData.options().toOptional().orElse(ImmutableList.of()).stream()
                        .map(ApplicationCommandHelper::applicationCommandOptionData2CommandDefinitionOption)
                        .collect(Collectors.toList()))
                .choices(applicationCommandOptionData.choices().toOptional().orElse(ImmutableList.of()).stream()
                        .map(choiceData -> CommandDefinitionOptionChoice.builder()
                                .name(choiceData.name())
                                .value(choiceData.value())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static ApplicationCommandRequest commandDefinition2ApplicationCommandRequest(CommandDefinition commandDefinition) {
        return ApplicationCommandRequest.builder()
                .name(commandDefinition.getName())
                .description(commandDefinition.getDescription())
                .options(commandDefinition.getOptions().stream()
                        .map(ApplicationCommandHelper::commandDefinitionOption2ApplicationCommandOptionData).collect(Collectors.toList()))
                .build();
    }

    private static ApplicationCommandOptionData commandDefinitionOption2ApplicationCommandOptionData(CommandDefinitionOption commandDefinitionOption) {
        return ApplicationCommandOptionData.builder()
                .type(commandDefinitionOption.getType().getValue())
                .name(commandDefinitionOption.getName())
                .description(commandDefinitionOption.getDescription())
                .required(Optional.ofNullable(commandDefinitionOption.getRequired()).map(Possible::of).orElse(Possible.absent()))
                .minValue(Optional.ofNullable(commandDefinitionOption.getMinValue()).map(Possible::of).orElse(Possible.absent()))
                .maxValue(Optional.ofNullable(commandDefinitionOption.getMaxValue()).map(Possible::of).orElse(Possible.absent()))
                .options(commandDefinitionOption.getOptions().stream()
                        .map(ApplicationCommandHelper::commandDefinitionOption2ApplicationCommandOptionData)
                        .collect(Collectors.toList()))
                .choices(commandDefinitionOption.getChoices().stream()
                        .map(choice -> ApplicationCommandOptionChoiceData.builder()
                                .name(choice.getName())
                                .value(choice.getValue())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
