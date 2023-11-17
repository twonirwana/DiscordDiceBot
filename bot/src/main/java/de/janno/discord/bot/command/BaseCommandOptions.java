package de.janno.discord.bot.command;

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
    private static final String LOCALE_OPTION_NAME_KEY = "base.option.locale.name";
    public static final String LOCALE_OPTION_NAME = I18n.getMessage(LOCALE_OPTION_NAME_KEY, Locale.ENGLISH);
    public static final CommandDefinitionOption LOCALE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(LOCALE_OPTION_NAME)
            .nameLocales(I18n.additionalMessages(LOCALE_OPTION_NAME_KEY))
            .description(I18n.getMessage("base.option.locale.description", Locale.ENGLISH))
            .descriptionLocales(I18n.additionalMessages("base.option.locale.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(I18n.allSupportedLanguage().stream()
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            //todo I18n
                            .name(ri.getDisplayName())
                            .value(ri.getLanguage())
                            .build())
                    .collect(Collectors.toList()))
            .build();

    private static final String DICE_IMAGE_COLOR_OPTION_NAME_KEY = "base.option.dice_color.name";
    public static final String DICE_IMAGE_COLOR_OPTION_NAME = I18n.getMessage(DICE_IMAGE_COLOR_OPTION_NAME_KEY, Locale.ENGLISH);
    public static final CommandDefinitionOption DICE_IMAGE_COLOR_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_COLOR_OPTION_NAME)
            .nameLocales(I18n.additionalMessages(DICE_IMAGE_COLOR_OPTION_NAME_KEY))
            .description(I18n.getMessage("base.option.dice_color.description", Locale.ENGLISH))
            .descriptionLocales(I18n.additionalMessages("base.option.dice_color.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .autoComplete(true)
            .build();
    private static final String DICE_IMAGE_STYLE_OPTION_NAME_KEY = "base.option.dice_image_style.name";
    public static final String DICE_IMAGE_STYLE_OPTION_NAME = I18n.getMessage(DICE_IMAGE_STYLE_OPTION_NAME_KEY, Locale.ENGLISH);
    public static final CommandDefinitionOption DICE_IMAGE_STYLE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_STYLE_OPTION_NAME)
            .nameLocales(I18n.additionalMessages(DICE_IMAGE_STYLE_OPTION_NAME_KEY))
            .description(I18n.getMessage("base.option.dice_image_style.description", Locale.ENGLISH))
            .descriptionLocales(I18n.additionalMessages("base.option.dice_image_style.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(DiceImageStyle.values())
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            //todo I18n
                            .name(ri.name())
                            .value(ri.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    private static final String ANSWER_FORMAT_OPTION_NAME_KEY = "base.option.answer_format.name";
    public static final String ANSWER_FORMAT_OPTION_NAME = I18n.getMessage(ANSWER_FORMAT_OPTION_NAME_KEY, Locale.ENGLISH);
    public static final CommandDefinitionOption ANSWER_FORMAT_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_FORMAT_OPTION_NAME)
            .nameLocales(I18n.additionalMessages(ANSWER_FORMAT_OPTION_NAME_KEY))
            .description(I18n.getMessage("base.option.answer_format.description", Locale.ENGLISH))
            .descriptionLocales(I18n.additionalMessages("base.option.answer_format.description"))
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(AnswerFormatType.values())
                    .map(answerFormatType -> CommandDefinitionOptionChoice.builder()
                            //todo I18n
                            .name(answerFormatType.name())
                            .value(answerFormatType.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    private static final String TARGET_CHANNEL_OPTION_NAME_KEY = "base.option.target_channel.name";
    public static final String TARGET_CHANNEL_OPTION_NAME = I18n.getMessage(TARGET_CHANNEL_OPTION_NAME_KEY, Locale.ENGLISH);
    public static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(TARGET_CHANNEL_OPTION_NAME)
            .nameLocales(I18n.additionalMessages(TARGET_CHANNEL_OPTION_NAME_KEY))
            .description(I18n.getMessage("base.option.target_channel.description", Locale.ENGLISH))
            .descriptionLocales(I18n.additionalMessages("base.option.target_channel.description"))
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();

    public static List<AutoCompleteAnswer> autoCompleteColorOption(AutoCompleteRequest autoCompleteRequest, Locale userLocale) {
        if (!DICE_IMAGE_COLOR_OPTION_NAME.equals(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }
        Optional<String> styleOptionValue = autoCompleteRequest.getOptionValues().stream()
                .filter(o -> DICE_IMAGE_STYLE_OPTION_NAME.equals(o.getOptionName()))
                .map(OptionValue::getOptionValue)
                .findFirst();
        if (styleOptionValue.isEmpty() || !DiceImageStyle.isValidStyle(styleOptionValue.get())) {
            return List.of(new AutoCompleteAnswer(I18n.getMessage("base.option.dice_image_style.auto.complete.missing.style.name", userLocale),
                    I18n.getMessage("base.option.dice.dice_image_style.auto.complete.missing.style.value", userLocale)));
        }
        return DiceImageStyle.valueOf(styleOptionValue.get()).getSupportedColors().stream()
                //todo I18n
                .filter(s -> s.contains(autoCompleteRequest.getFocusedOptionValue()))
                .limit(25)
                .map(c -> new AutoCompleteAnswer(c, c))
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
}
