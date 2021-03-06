package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
public class FateCommand extends AbstractCommand<FateCommand.Config, FateCommand.State> {

    private static final String COMMAND_NAME = "fate";
    private static final String ACTION_MODIFIER_OPTION = "type";
    private static final String ACTION_MODIFIER_OPTION_SIMPLE = "simple";
    private static final String ACTION_MODIFIER_OPTION_MODIFIER = "with_modifier";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final DiceUtils diceUtils;

    @VisibleForTesting
    public FateCommand(DiceUtils diceUtils) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceUtils = diceUtils;
    }

    public FateCommand() {
        this(new DiceUtils());
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected Optional<Long> getAnswerTargetChannelId(Config config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
    }
    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure Fate dice";
    }
    private String createButtonMessage(Config config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return "Click a button to roll four fate dice and add the value of the button";
        }
        return "Click a button to roll four fate dice";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Buttons for Fate/Fudge dice. There are two types, the simple produces one button that rolls four dice and " +
                        "provides the result together with the sum. The type with_modifier produces multiple buttons for modifier -4 to +10" +
                        " that roll four dice and add the modifier of the button.")
                .field(new EmbedDefinition.Field("Example", "'/fate start type:with_modifier' or '/fate start type:simple'", false))
                .build();
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(CommandDefinitionOption.builder()
                        .name(ACTION_MODIFIER_OPTION)
                        .required(true)
                        .description("Show modifier buttons")
                        .type(CommandDefinitionOption.Type.STRING)
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name(ACTION_MODIFIER_OPTION_SIMPLE)
                                .value(ACTION_MODIFIER_OPTION_SIMPLE)
                                .build())
                        .choice(CommandDefinitionOptionChoice.builder()
                                .name(ACTION_MODIFIER_OPTION_MODIFIER)
                                .value(ACTION_MODIFIER_OPTION_MODIFIER)
                                .build())
                        .build(),
                ANSWER_TARGET_CHANNEL_COMMAND_OPTION);
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        return new Config(options.getStringSubOptionWithName(ACTION_MODIFIER_OPTION).orElse(ACTION_MODIFIER_OPTION_SIMPLE),
                getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        List<Integer> rollResult = diceUtils.rollFate();

        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType()) && state.getModifier() != null) {
            int modifier = state.getModifier();
            String modifierString = "";
            if (modifier > 0) {
                modifierString = " +" + modifier;
            } else if (modifier < 0) {
                modifierString = " " + modifier;
            }
            int resultWithModifier = DiceUtils.fateResult(rollResult) + modifier;

            String title = String.format("4dF%s = %d", modifierString, resultWithModifier);
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
        } else {
            String title = String.format("4dF = %d", DiceUtils.fateResult(rollResult));
            String details = DiceUtils.convertFateNumberToString(rollResult);
            return Optional.of(new EmbedDefinition(title, details, ImmutableList.of()));
        }
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, Config config) {
        return Optional.of(createNewButtonMessage(config));
    }

    @Override
    protected MessageDefinition createNewButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content(createButtonMessage(config))
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    private List<ComponentRowDefinition> createButtonLayout(Config config) {
        if (ACTION_MODIFIER_OPTION_MODIFIER.equals(config.getType())) {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    //              ID,  label
                                    ButtonDefinition.builder().id(createButtonCustomId("-4", config)).label("-4").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-3", config)).label("-3").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-2", config)).label("-2").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("-1", config)).label("-1").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("0", config)).label("0").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(createButtonCustomId("1", config)).label("+1").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("2", config)).label("+2").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("3", config)).label("+3").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("4", config)).label("+4").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("5", config)).label("+5").build()
                            )
                    ).build(),
                    ComponentRowDefinition.builder().buttonDefinitions(
                            ImmutableList.of(
                                    ButtonDefinition.builder().id(createButtonCustomId("6", config)).label("+6").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("7", config)).label("+7").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("8", config)).label("+8").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("9", config)).label("+9").build(),
                                    ButtonDefinition.builder().id(createButtonCustomId("10", config)).label("+10").build()
                            )
                    ).build());
        } else {
            return ImmutableList.of(
                    ComponentRowDefinition.builder().buttonDefinition(
                            ButtonDefinition.builder().id(createButtonCustomId(ROLL_BUTTON_ID, config)).label("Roll 4dF").build()
                    ).build());
        }
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        return new Config(split[2], getOptionalLongFromArray(split, 3));
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String modifier = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1];
        if (!Strings.isNullOrEmpty(modifier) && NumberUtils.isParsable(modifier)) {
            return new State(Integer.valueOf(modifier));
        }
        return new State(null);
    }

    @VisibleForTesting
    String createButtonCustomId(String modifier, Config config) {
        Preconditions.checkArgument(!modifier.contains(BotConstants.CONFIG_DELIMITER));
        Preconditions.checkArgument(!config.getType().contains(BotConstants.CONFIG_DELIMITER));

        return String.join(BotConstants.CONFIG_DELIMITER,
                COMMAND_NAME,
                modifier,
                config.getType(),
                Optional.ofNullable(config.getAnswerTargetChannelId()).map(Object::toString).orElse(""));
    }


    @Value
    protected static class Config implements IConfig {

        @NonNull
        String type;
        Long answerTargetChannelId;

        @Override
        public String toShortString() {
            return String.format("[%s, %s]", type, targetChannelToString(answerTargetChannelId));
        }
    }

    @Value
    static class State implements IState {
        Integer modifier;

        @Override
        public String toShortString() {
            return String.format("[%s]", Optional.ofNullable(modifier).map(String::valueOf).orElse(""));
        }
    }
}
