package de.janno.discord.bot.command;

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
import java.util.Optional;
import java.util.stream.Collectors;

public final class BaseCommandOptions {
    public static final String ANSWER_TARGET_CHANNEL_OPTION = "target_channel";
    public static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_TARGET_CHANNEL_OPTION)
            .description("Another channel where the answer will be given")
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();
    public static final String ANSWER_FORMAT_OPTION = "answer_format";
    public static final CommandDefinitionOption ANSWER_FORMAT_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_FORMAT_OPTION)
            .description("How the answer will be displayed")
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(AnswerFormatType.values())
                    .map(answerFormatType -> CommandDefinitionOptionChoice.builder()
                            .name(answerFormatType.name())
                            .value(answerFormatType.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    public static final String DICE_IMAGE_STYLE_OPTION = "dice_image_style";
    public static final CommandDefinitionOption DICE_IMAGE_STYLE_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_STYLE_OPTION)
            .description("If and in what style the dice throw should be shown as image")
            .type(CommandDefinitionOption.Type.STRING)
            .choices(Arrays.stream(DiceImageStyle.values())
                    .map(ri -> CommandDefinitionOptionChoice.builder()
                            .name(ri.name())
                            .value(ri.name())
                            .build())
                    .collect(Collectors.toList()))
            .build();
    public static final String DICE_IMAGE_COLOR_OPTION = "dice_image_color";
    public static final CommandDefinitionOption DICE_IMAGE_COLOR_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_COLOR_OPTION)
            .description("The default color option. Can be influenced by the `color` method")
            .type(CommandDefinitionOption.Type.STRING)
            .autoComplete(true)
            .build();

    public static List<AutoCompleteAnswer> autoCompleteColorOption(AutoCompleteRequest autoCompleteRequest) {
        if (!DICE_IMAGE_COLOR_OPTION.equals(autoCompleteRequest.getFocusedOptionName())) {
            return List.of();
        }
        Optional<String> styleOptionValue = autoCompleteRequest.getOptionValues().stream()
                .filter(o -> DICE_IMAGE_STYLE_OPTION.equals(o.getOptionName()))
                .map(OptionValue::getOptionValue)
                .findFirst();
        if (styleOptionValue.isEmpty() || !DiceImageStyle.isValidStyle(styleOptionValue.get())) {
            return List.of(new AutoCompleteAnswer("Select the dice image style first", "none"));
        }
        return DiceImageStyle.valueOf(styleOptionValue.get()).getSupportedColors().stream()
                .filter(s -> s.contains(autoCompleteRequest.getFocusedOptionValue()))
                .limit(25)
                .map(c -> new AutoCompleteAnswer(c, c))
                .collect(Collectors.toList());
    }

    public static Optional<Long> getAnswerTargetChannelIdFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getChannelIdSubOptionWithName(ANSWER_TARGET_CHANNEL_OPTION);
    }

    public static Optional<AnswerFormatType> getAnswerTypeFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(ANSWER_FORMAT_OPTION)
                .map(AnswerFormatType::valueOf);
    }

    public static Optional<DiceImageStyle> getDiceStyleOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(DICE_IMAGE_STYLE_OPTION)
                .map(DiceImageStyle::valueOf);
    }

    public static Optional<String> getDiceColorOptionFromStartCommandOption(@NonNull CommandInteractionOption options) {
        return options.getStringSubOptionWithName(DICE_IMAGE_COLOR_OPTION);
    }
}
