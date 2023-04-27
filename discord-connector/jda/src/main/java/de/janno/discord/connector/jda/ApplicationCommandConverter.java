package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ApplicationCommandConverter {

    public static CommandDefinition slashCommand2CommandDefinition(Command slashCommand) {
        List<CommandDefinitionOption> optionList = new ArrayList<>();
        optionList.addAll(slashCommand.getOptions().stream()
                .map(ApplicationCommandConverter::commandOption2CommandDefinitionOption).toList());
        optionList.addAll(slashCommand.getSubcommands().stream()
                .map(ApplicationCommandConverter::subcommand2CommandDefinitionOption).toList());
        optionList.addAll(slashCommand.getSubcommandGroups().stream()
                .map(ApplicationCommandConverter::subcommandGroup2CommandDefinitionOption).toList());
        return CommandDefinition.builder()
                .name(slashCommand.getName())
                .description(slashCommand.getDescription())
                .options(optionList)
                .build();
    }

    private static CommandDefinitionOption subcommandGroup2CommandDefinitionOption(Command.SubcommandGroup subcommandGroup) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                .name(subcommandGroup.getName())
                .description(subcommandGroup.getDescription())
                .options(subcommandGroup.getSubcommands().stream()
                        .map(ApplicationCommandConverter::subcommand2CommandDefinitionOption)
                        .collect(Collectors.toList()))
                .build();
    }

    private static CommandDefinitionOption subcommand2CommandDefinitionOption(Command.Subcommand subcommand) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.SUB_COMMAND)
                .name(subcommand.getName())
                .description(subcommand.getDescription())
                .options(subcommand.getOptions().stream()
                        .map(ApplicationCommandConverter::commandOption2CommandDefinitionOption)
                        .collect(Collectors.toList()))
                .build();
    }

    private static CommandDefinitionOption commandOption2CommandDefinitionOption(Command.Option commandOption) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.of(commandOption.getType().getKey()))
                .name(commandOption.getName())
                .description(commandOption.getDescription())
                .required(commandOption.isRequired())
                .autoComplete(commandOption.isAutoComplete())
                .minValue(Optional.ofNullable(commandOption.getMinValue()).map(Number::longValue).orElse(null))
                .maxValue(Optional.ofNullable(commandOption.getMaxValue()).map(Number::longValue).orElse(null))
                .choices(commandOption.getChoices().stream()
                        .map(c -> CommandDefinitionOptionChoice.builder()
                                .name(c.getName())
                                .value(c.getAsString())
                                .build()
                        ).collect(Collectors.toList())
                )
                .build();
    }

    public static CommandData commandDefinition2CommandData(CommandDefinition commandDefinition) {
        return new CommandDataImpl(commandDefinition.getName(), commandDefinition.getDescription())
                .addSubcommands(commandDefinition.getOptions().stream()
                        .filter(c -> c.getType() == CommandDefinitionOption.Type.SUB_COMMAND)
                        .map(ApplicationCommandConverter::commandDefinitionOption2SubcommandData)
                        .collect(Collectors.toList()))
                .addSubcommandGroups(commandDefinition.getOptions().stream()
                        .filter(c -> c.getType() == CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .map(ApplicationCommandConverter::commandDefinitionOption2SubcommandGroupData)
                        .collect(Collectors.toList()))
                .addOptions(commandDefinition.getOptions().stream()
                        .filter(c -> c.getType() != CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                        .filter(c -> c.getType() != CommandDefinitionOption.Type.SUB_COMMAND)
                        .map(ApplicationCommandConverter::commandDefinitionOption2OptionData).collect(Collectors.toList()));
    }

    private static SubcommandGroupData commandDefinitionOption2SubcommandGroupData(CommandDefinitionOption commandDefinitionOption) {
        return new SubcommandGroupData(commandDefinitionOption.getName(), commandDefinitionOption.getDescription())
                .addSubcommands(commandDefinitionOption.getOptions().stream()
                        .map(ApplicationCommandConverter::commandDefinitionOption2SubcommandData)
                        .collect(Collectors.toList()));
    }

    private static SubcommandData commandDefinitionOption2SubcommandData(CommandDefinitionOption commandDefinitionOption) {
        return new SubcommandData(commandDefinitionOption.getName(), commandDefinitionOption.getDescription())
                .addOptions(commandDefinitionOption.getOptions().stream()
                        .map(ApplicationCommandConverter::commandDefinitionOption2OptionData)
                        .collect(Collectors.toList()));
    }

    private static OptionData commandDefinitionOption2OptionData(CommandDefinitionOption commandDefinitionOption) {
        OptionData optionData = new OptionData(OptionType.fromKey(commandDefinitionOption.getType().getValue()),
                commandDefinitionOption.getName(),
                commandDefinitionOption.getDescription())
                .setRequired(commandDefinitionOption.isRequired())
                .setAutoComplete(commandDefinitionOption.isAutoComplete())
                .addChoices(commandDefinitionOption.getChoices().stream()
                        .map(choice -> new Command.Choice(choice.getName(), choice.getValue()))
                        .collect(Collectors.toList()));
        if (commandDefinitionOption.getMinValue() != null) {
            optionData.setMinValue(commandDefinitionOption.getMinValue());
        }
        if (commandDefinitionOption.getMaxValue() != null) {
            optionData.setMaxValue(commandDefinitionOption.getMaxValue());
        }
        return optionData;
    }

    public static CommandInteractionOption optionMapping2CommandInteractionOption(OptionMapping optionMapping) {
        return CommandInteractionOption.builder()
                .name(optionMapping.getName())
                .booleanValue(optionMapping.getType() == OptionType.BOOLEAN ? optionMapping.getAsBoolean() : null)
                .longValue(optionMapping.getType() == OptionType.INTEGER ? (long) optionMapping.getAsInt() : null)
                .stringValue(optionMapping.getType() == OptionType.STRING ? optionMapping.getAsString() : null)
                .channelIdValue(optionMapping.getType() == OptionType.CHANNEL ? optionMapping.getAsLong() : null)
                .build();
    }
}
