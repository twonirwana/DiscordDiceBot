package de.janno.discord.bot.command;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.dice.image.LegacyImageConfigHelper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.*;

/**
 * A configuration for a dice system. It will be created with the slash command and not modified afterwards.
 */
@EqualsAndHashCode
@Getter
@ToString
@SuperBuilder(toBuilder = true)
public class RollConfig implements Config {

    private final Long answerTargetChannelId;

    @NonNull
    private final AnswerFormatType answerFormatType;
    @NonNull
    private final AnswerInteractionType answerInteractionType;

    @NonNull
    private final DiceStyleAndColor diceStyleAndColor;

    @NonNull
    private final Locale configLocale;


    private final UUID callStarterConfigAfterFinish;


    private final String name;

    @JsonCreator
    public RollConfig(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                      @JsonProperty("answerFormatType") AnswerFormatType answerFormatType,
                      @JsonProperty("answerInteractionType") AnswerInteractionType answerInteractionType,
                      @JsonProperty("resultImage") ResultImage resultImage,
                      @JsonProperty("diceImageStyle") DiceStyleAndColor diceStyleAndColor,
                      @JsonProperty("configLocale") Locale configLocale,
                      @JsonProperty("callStarterConfigAfterFinish") UUID callStarterConfigAfterFinish,
                      @JsonProperty("name") String name
    ) {
        this.answerTargetChannelId = answerTargetChannelId;
        this.answerFormatType = Optional.ofNullable(answerFormatType).orElse(AnswerFormatType.full);
        this.answerInteractionType = Optional.ofNullable(answerInteractionType).orElse(AnswerInteractionType.none);

        if (resultImage != null) {
            this.diceStyleAndColor = LegacyImageConfigHelper.getStyleAndColor(resultImage);
        } else if (diceStyleAndColor != null) {
            this.diceStyleAndColor = diceStyleAndColor;
        } else {
            this.diceStyleAndColor = new DiceStyleAndColor(DiceImageStyle.none, DiceImageStyle.none.getDefaultColor());
        }
        this.configLocale = Optional.ofNullable(configLocale).orElse(Locale.ENGLISH);
        this.callStarterConfigAfterFinish = callStarterConfigAfterFinish;
        this.name = name;
    }

    public String toShortString() {
        return String.format("[%s, %s, %s, %s]", getTargetChannelShortString(), answerFormatType, answerInteractionType, diceStyleAndColor);
    }

    public String toCommandOptionsString() {
        List<String> out = new ArrayList<>();
        out.add(String.format("%s: %s", BaseCommandOptions.ANSWER_FORMAT_OPTION_NAME, answerFormatType));
        out.add(String.format("%s: %s", BaseCommandOptions.ANSWER_INTERACTION_OPTION_NAME, answerInteractionType));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_STYLE_OPTION_NAME, diceStyleAndColor.getDiceImageStyle()));
        out.add(String.format("%s: %s", BaseCommandOptions.DICE_IMAGE_COLOR_OPTION_NAME, diceStyleAndColor.getConfiguredDefaultColor()));
        if (answerTargetChannelId != null) {
            out.add(String.format("%s: <#%s>", BaseCommandOptions.TARGET_CHANNEL_OPTION_NAME, answerTargetChannelId));
        }
        if (name != null) {
            out.add(String.format("%s: %s", BaseCommandOptions.NAME_OPTION_NAME, name));
        }
        //language should be set over the client
        //out.add(String.format("%s: %s", BaseCommandOptions.LOCALE_OPTION_NAME, configLocale));
        return String.join(" ", out);
    }

    protected String getTargetChannelShortString() {
        return answerTargetChannelId == null ? "local" : "target";
    }

    @JsonIgnore
    public boolean alwaysSumResultUp() {
        return false;
    }

}
