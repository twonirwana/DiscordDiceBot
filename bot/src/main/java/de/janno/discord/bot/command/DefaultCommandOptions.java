package de.janno.discord.bot.command;

import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public final class DefaultCommandOptions {
    public static final String ANSWER_TARGET_CHANNEL_OPTION = "target_channel";
    public static final String ANSWER_FORMAT_OPTION = "answer_format";
    public static final String DICE_IMAGE_STYLE_OPTION = "dice_image_style";
    public static final String DICE_IMAGE_COLOR_OPTION = "dice_image_color";

    public static final CommandDefinitionOption ANSWER_TARGET_CHANNEL_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(ANSWER_TARGET_CHANNEL_OPTION)
            .description("Another channel where the answer will be given")
            .type(CommandDefinitionOption.Type.CHANNEL)
            .build();

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

    public static final CommandDefinitionOption DICE_IMAGE_COLOR_COMMAND_OPTION = CommandDefinitionOption.builder()
            .name(DICE_IMAGE_COLOR_OPTION)
            .description("The default color option. Can be influenced by the `color` method")
            .type(CommandDefinitionOption.Type.STRING)
            //todo autocomplete choice over image styles
            /* .choices(Arrays.stream(ResultImage.values())
                     .map(ri -> CommandDefinitionOptionChoice.builder()
                             .name(ri.name())
                             .value(ri.name())
                             .build())
                     .collect(Collectors.toList()))*/
            .build();

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
