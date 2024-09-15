package de.janno.discord.connector.jda;

import com.google.common.collect.ImmutableSortedSet;
import de.janno.discord.connector.api.slash.*;
import lombok.NonNull;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.api.interactions.commands.localization.LocalizationMap;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.*;
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
                .nameLocales(discordLocale2LocaleName(slashCommand.getNameLocalizations()))
                .descriptionLocales(discordLocale2LocaleDescription(slashCommand.getDescriptionLocalizations()))
                .options(optionList)
                .integrationTypes(integrationTypes2CommandIntegrationTypes(slashCommand.getIntegrationTypes()))
                .build();
    }

    private static Set<IntegrationType> commandIntegrationTypes2IntegrationTypes(Set<CommandIntegrationType> commandIntegrationTypes) {
        return commandIntegrationTypes.stream()
                .map(i -> IntegrationType.valueOf(i.name()))
                .collect(Collectors.toSet());
    }

    private static Set<CommandIntegrationType> integrationTypes2CommandIntegrationTypes(Set<IntegrationType> integrationTypes) {
        return integrationTypes.stream()
                .map(i -> CommandIntegrationType.valueOf(i.name()))
                .collect(Collectors.toSet());
    }

    private static CommandDefinitionOption subcommandGroup2CommandDefinitionOption(Command.SubcommandGroup subcommandGroup) {
        return CommandDefinitionOption.builder()
                .type(CommandDefinitionOption.Type.SUB_COMMAND_GROUP)
                .name(subcommandGroup.getName())
                .description(subcommandGroup.getDescription())
                .nameLocales(discordLocale2LocaleName(subcommandGroup.getNameLocalizations()))
                .descriptionLocales(discordLocale2LocaleDescription(subcommandGroup.getDescriptionLocalizations()))
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
                .nameLocales(discordLocale2LocaleName(subcommand.getNameLocalizations()))
                .descriptionLocales(discordLocale2LocaleDescription(subcommand.getDescriptionLocalizations()))
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
                .nameLocales(discordLocale2LocaleName(commandOption.getNameLocalizations()))
                .descriptionLocales(discordLocale2LocaleDescription(commandOption.getDescriptionLocalizations()))
                .required(commandOption.isRequired())
                .autoComplete(commandOption.isAutoComplete())
                .minValue(Optional.ofNullable(commandOption.getMinValue()).map(Number::longValue).orElse(null))
                .maxValue(Optional.ofNullable(commandOption.getMaxValue()).map(Number::longValue).orElse(null))
                .choices(commandOption.getChoices().stream()
                        .map(c -> CommandDefinitionOptionChoice.builder()
                                .name(c.getName())
                                .value(c.getAsString())
                                .nameLocales(discordLocale2LocaleChoice(c.getNameLocalizations()))
                                .build()
                        ).collect(Collectors.toList())
                )
                .build();
    }

    private static Set<InteractionContextType> getInteractionContextTypeFromInteractionTypes(@NonNull Set<CommandIntegrationType> interactionTypes) {
        ImmutableSortedSet.Builder<InteractionContextType> builder = new ImmutableSortedSet.Builder<>(Enum::compareTo);
        builder.add(InteractionContextType.BOT_DM);
        if (interactionTypes.contains(CommandIntegrationType.GUILD_INSTALL)) {
            builder.add(InteractionContextType.GUILD);
        }
        if (interactionTypes.contains(CommandIntegrationType.USER_INSTALL)) {
            builder.add(InteractionContextType.PRIVATE_CHANNEL);
        }
        return builder.build();
    }

    public static CommandData commandDefinition2CommandData(CommandDefinition commandDefinition) {
        return new CommandDataImpl(commandDefinition.getName(), commandDefinition.getDescription())
                .setNameLocalizations(localeName2DiscordLocaleMap(commandDefinition.getNameLocales()))
                .setDescriptionLocalizations(localeDescription2DiscordLocaleMap(commandDefinition.getDescriptionLocales()))
                .setIntegrationTypes(commandIntegrationTypes2IntegrationTypes(commandDefinition.getIntegrationTypes()))
                .setContexts(getInteractionContextTypeFromInteractionTypes(commandDefinition.getIntegrationTypes()))
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
                .setNameLocalizations(localeName2DiscordLocaleMap(commandDefinitionOption.getNameLocales()))
                .setDescriptionLocalizations(localeDescription2DiscordLocaleMap(commandDefinitionOption.getDescriptionLocales()))
                .addSubcommands(commandDefinitionOption.getOptions().stream()
                        .map(ApplicationCommandConverter::commandDefinitionOption2SubcommandData)
                        .collect(Collectors.toList()));
    }

    private static SubcommandData commandDefinitionOption2SubcommandData(CommandDefinitionOption commandDefinitionOption) {
        return new SubcommandData(commandDefinitionOption.getName(), commandDefinitionOption.getDescription())
                .setNameLocalizations(localeName2DiscordLocaleMap(commandDefinitionOption.getNameLocales()))
                .setDescriptionLocalizations(localeDescription2DiscordLocaleMap(commandDefinitionOption.getDescriptionLocales()))
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
                .setNameLocalizations(localeName2DiscordLocaleMap(commandDefinitionOption.getNameLocales()))
                .setDescriptionLocalizations(localeDescription2DiscordLocaleMap(commandDefinitionOption.getDescriptionLocales()))
                .addChoices(commandDefinitionOption.getChoices().stream()
                        .map(choice -> new Command.Choice(choice.getName(), choice.getValue())
                                .setNameLocalizations(localeChoice2DiscordLocaleMap(choice.getNameLocales()))
                        )
                        .collect(Collectors.toList()));
        if (commandDefinitionOption.getMinValue() != null) {
            optionData.setMinValue(commandDefinitionOption.getMinValue());
        }
        if (commandDefinitionOption.getMaxValue() != null) {
            optionData.setMaxValue(commandDefinitionOption.getMaxValue());
        }
        return optionData;
    }

    private static Map<DiscordLocale, String> localeDescription2DiscordLocaleMap(Collection<CommandLocaleDescription> commandLocaleDesciptions) {
        return commandLocaleDesciptions.stream().collect(Collectors.toMap(lv -> DiscordLocale.from(lv.getLocale()), CommandLocaleDescription::getDescription));
    }

    private static Map<DiscordLocale, String> localeName2DiscordLocaleMap(Collection<CommandLocaleName> commandLocaleNames) {
        return commandLocaleNames.stream().collect(Collectors.toMap(lv -> DiscordLocale.from(lv.getLocale()), CommandLocaleName::getName));
    }

    private static Map<DiscordLocale, String> localeChoice2DiscordLocaleMap(Collection<CommandLocaleChoice> commandLocaleChoices) {
        return commandLocaleChoices.stream().collect(Collectors.toMap(lv -> DiscordLocale.from(lv.getLocale()), CommandLocaleChoice::getChoice));
    }


    private static List<CommandLocaleName> discordLocale2LocaleName(LocalizationMap localizationMap) {
        return localizationMap.toMap().entrySet().stream()
                .map(e -> new CommandLocaleName(LocaleConverter.toLocale(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(lv -> lv.getLocale().toString()))
                .collect(Collectors.toList());
    }

    private static List<CommandLocaleDescription> discordLocale2LocaleDescription(LocalizationMap localizationMap) {
        return localizationMap.toMap().entrySet().stream()
                .map(e -> new CommandLocaleDescription(LocaleConverter.toLocale(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(lv -> lv.getLocale().toString()))
                .collect(Collectors.toList());
    }

    private static List<CommandLocaleChoice> discordLocale2LocaleChoice(LocalizationMap localizationMap) {
        return localizationMap.toMap().entrySet().stream()
                .map(e -> new CommandLocaleChoice(LocaleConverter.toLocale(e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(lv -> lv.getLocale().toString()))
                .collect(Collectors.toList());
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
