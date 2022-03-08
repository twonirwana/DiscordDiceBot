package de.janno.discord.discord4j;

import de.janno.discord.command.slash.CommandDefinition;
import de.janno.discord.command.slash.CommandDefinitionOption;
import de.janno.discord.command.slash.CommandDefinitionOptionChoice;
import de.janno.discord.command.slash.CommandInteractionOption;
import org.javacord.api.interaction.*;

import java.util.stream.Collectors;

public class ApplicationCommandHelper {

    public static CommandDefinition slashCommand2CommandDefinition(SlashCommand slashCommand) {
        return CommandDefinition.builder()
                .name(slashCommand.getName())
                .description(slashCommand.getDescription())
                .options(slashCommand.getOptions().stream()
                        .map(ApplicationCommandHelper::slashCommandOption2CommandDefinitionOption).collect(Collectors.toList()))
                .build();
    }

    private static CommandDefinitionOption slashCommandOption2CommandDefinitionOption(SlashCommandOption slashCommandOption) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.of(slashCommandOption.getType().getValue()))
                .name(slashCommandOption.getName())
                .description(slashCommandOption.getDescription())
                .required(slashCommandOption.isRequired())
                .minValue(slashCommandOption.getLongMinValue().orElse(null))
                .maxValue(slashCommandOption.getLongMaxValue().orElse(null))
                .options(slashCommandOption.getOptions().stream()
                        .map(ApplicationCommandHelper::slashCommandOption2CommandDefinitionOption)
                        .collect(Collectors.toList()))
                .choices(slashCommandOption.getChoices().stream()
                        .map(choiceData -> CommandDefinitionOptionChoice.builder()
                                .name(choiceData.getName())
                                .value(choiceData.getValueAsString())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    public static SlashCommandBuilder commandDefinition2SlashCommandBuilder(CommandDefinition commandDefinition) {
        return new SlashCommandBuilder()
                .setName(commandDefinition.getName())
                .setDescription(commandDefinition.getDescription())
                .setOptions(commandDefinition.getOptions().stream()
                        .map(ApplicationCommandHelper::commandDefinitionOption2SlashCommandOption).collect(Collectors.toList()));
    }

    private static SlashCommandOption commandDefinitionOption2SlashCommandOption(CommandDefinitionOption commandDefinitionOption) {
        SlashCommandOptionBuilder builder = new SlashCommandOptionBuilder()
                .setType(SlashCommandOptionType.fromValue(commandDefinitionOption.getType().getValue()))
                .setName(commandDefinitionOption.getName())
                .setDescription(commandDefinitionOption.getDescription())
                .setRequired(commandDefinitionOption.getRequired())

                .setOptions(commandDefinitionOption.getOptions().stream()
                        .map(ApplicationCommandHelper::commandDefinitionOption2SlashCommandOption)
                        .collect(Collectors.toList()))
                .setChoices(commandDefinitionOption.getChoices().stream()
                        .map(choice -> new SlashCommandOptionChoiceBuilder()
                                .setName(choice.getName())
                                .setValue(choice.getValue())
                                .build())
                        .collect(Collectors.toList()));
        if (commandDefinitionOption.getMinValue() != null) {
            builder.setLongMinValue(commandDefinitionOption.getMinValue());
        }
        if (commandDefinitionOption.getMaxValue() != null) {
            builder.setLongMaxValue(commandDefinitionOption.getMaxValue());
        }
        return builder.build();
    }

    public static CommandInteractionOption slashCommandInteractionOption2CommandInteractionOption(SlashCommandInteractionOption slashCommandInteractionOption) {
        return CommandInteractionOption.builder()
                .name(slashCommandInteractionOption.getName())
                .booleanValue(slashCommandInteractionOption.getBooleanValue().orElse(null))
                .longValue(slashCommandInteractionOption.getLongValue().orElse(null))
                .stringValue(slashCommandInteractionOption.getStringValue().orElse(null))
                .options(slashCommandInteractionOption.getOptions().stream()
                        .map(ApplicationCommandHelper::slashCommandInteractionOption2CommandInteractionOption)
                        .collect(Collectors.toList()))
                .build();
    }
}
