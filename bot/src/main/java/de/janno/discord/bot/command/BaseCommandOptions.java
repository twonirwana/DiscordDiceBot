package de.janno.discord.bot.command;

import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.OptionValue;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public final class BaseCommandOptions {
    public static final String LOCALE_OPTION_NAME = "language";
    public static final CommandDefinitionOption LOCALE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(LOCALE_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.locale.name"))
            .description(I18n.getMessage("base.option.locale.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.locale.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(I18n.allSupportedLanguage().stream()
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            .name(ri.getDisplayName())
                            .nameLocales(I18n.allNoneEnglishMessagesChoices("base.option.language." + ri))
                            .value(ri.toString())
                            .build())
                    .collect(Collectors.toList()))
            .build();

    public static final String DICE_IMAGE_COLOR_OPTION_NAME = "dice_image_color";
    public static final CommandDefinitionOption DICE_IMAGE_COLOR_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_COLOR_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.dice_color.name"))
            .description(I18n.getMessage("base.option.dice_color.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.dice_color.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .autoComplete(true)
            .build();
    public static final String DICE_IMAGE_STYLE_OPTION_NAME = "dice_image_style";
    public static final CommandDefinitionOption DICE_IMAGE_STYLE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_STYLE_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.dice_image_style.name"))
            .description(I18n.getMessage("base.option.dice_image_style.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.dice_image_style.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(DiceImageStyle.values())
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            .name(ri.name())
                            .nameLocales(I18n.allNoneEnglishMessagesChoices("base.option.dice_image_style." + ri.name()))
                            .value(ri.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    public static final String ANSWER_FORMAT_OPTION_NAME = "answer_format";
    public static final CommandDefinitionOption ANSWER_FORMAT_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_FORMAT_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.answer_format.name"))
            .description(I18n.getMessage("base.option.answer_format.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.answer_format.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(AnswerFormatType.values())
                    .map(answerFormatType -> CommandDefinitionOptionChoice.builder()
                            .name(answerFormatType.name())
                            .nameLocales(I18n.allNoneEnglishMessagesChoices("base.option.answer_format.%s.name".formatted(answerFormatType.name())))
                            .value(answerFormatType.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    public static final String ANSWER_INTERACTION_OPTION_NAME = "answer_interaction";
    public static final CommandDefinitionOption ANSWER_INTERACTION_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_INTERACTION_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.answer_interaction.name"))
            .description(I18n.getMessage("base.option.answer_interaction.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.answer_interaction.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(AnswerInteractionType.values())
                    .map(answerInteractionType -> CommandDefinitionOptionChoice.builder()
                            .name(answerInteractionType.name())
                            .nameLocales(I18n.allNoneEnglishMessagesChoices("base.option.answer_interaction.%s.name".formatted(answerInteractionType.name())))
                            .value(answerInteractionType.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    public static final String TARGET_CHANNEL_OPTION_NAME = "target_channel";
    public static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(TARGET_CHANNEL_OPTION_NAME)
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.target_channel.name"))
            .description(I18n.getMessage("base.option.target_channel.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.target_channel.description"))
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();
    public static final String NAME_OPTION_NAME = "name";
    public static final CommandDefinitionOption NAME_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(NAME_OPTION_NAME)
            //todo i18n
            .nameLocales(I18n.allNoneEnglishMessagesNames("base.option.name.name"))
            .description(I18n.getMessage("base.option.name.description", Locale.ENGLISH))
            .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("base.option.name.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .build();

    public static AnswerInteractionType getAnswerInteractionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(ANSWER_INTERACTION_OPTION_NAME)
                .map(AnswerInteractionType::valueOf)
                .orElse(AnswerInteractionType.none);
    }

    public static List<AutoCompleteAnswer> autoCompleteColorOption(AutoCompleteRequest autoCompleteRequest, Locale userLocale) {
        if (!DICE_IMAGE_COLOR_OPTION_NAME.equals(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }
        Optional<String> styleOptionValue = autoCompleteRequest.getOptionValues().stream()
                .filter(o -> DICE_IMAGE_STYLE_OPTION_NAME.equals(o.getOptionName()))
                .map(OptionValue::getOptionValue)
                .findFirst();
        if (styleOptionValue.isEmpty() || !DiceImageStyle.isValidStyle(styleOptionValue.get())) {
            return List.of(new AutoCompleteAnswer(I18n.getMessage("base.option.dice_image_style.autoComplete.missingStyle.name", userLocale),
                    DiceImageStyle.NONE_DICE_COLOR));
        }
        DiceImageStyle diceImageStyle = DiceImageStyle.valueOf(styleOptionValue.get());
        return diceImageStyle.getSupportedColors().stream()
                .filter(s -> diceImageStyle.getLocalizedColorName(s, userLocale).contains(autoCompleteRequest.getFocusedOptionValue()))
                .limit(25)
                .map(c -> new AutoCompleteAnswer(diceImageStyle.getLocalizedColorName(c, userLocale), c))
                .collect(Collectors.toList());
    }

    public static Optional<Long> getAnswerTargetChannelIdFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getChannelIdSubOptionWithName(TARGET_CHANNEL_OPTION_NAME);
    }

    public static Optional<AnswerFormatType> getAnswerTypeFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(ANSWER_FORMAT_OPTION_NAME)
                .map(AnswerFormatType::valueOf);
    }

    public static Optional<DiceImageStyle> getDiceStyleOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(DICE_IMAGE_STYLE_OPTION_NAME)
                .map(DiceImageStyle::valueOf);
    }

    public static Optional<String> getDiceColorOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(DICE_IMAGE_COLOR_OPTION_NAME);
    }

    public static Optional<Locale> getLocaleOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(LOCALE_OPTION_NAME).map(Locale::of);
    }

    public static Optional<String> getNameFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(NAME_OPTION_NAME);
    }
}
