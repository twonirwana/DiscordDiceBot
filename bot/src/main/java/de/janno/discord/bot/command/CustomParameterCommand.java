package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterCommand.Config, CustomParameterCommand.State> {

    //todo button range, message format, input field?, tests, config validation, doc

    private static final String COMMAND_NAME = "custom_parameter";

    private static final String EXPRESSION_OPTION = "expression";

    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    private static final String CLEAR_BUTTON_ID = "clear";
    private final DiceParserHelper diceParserHelper;

    public CustomParameterCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomParameterCommand(DiceParserHelper diceParserHelper) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceParserHelper = diceParserHelper;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Fill the parameter of a given dice expression and roll it when all parameter are provided";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/custom_parameter start' and provide a dice expression with parameter variables with the format {parameter_name}")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION)
                        .required(true)
                        .description("Expression")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                ANSWER_TARGET_CHANNEL_COMMAND_OPTION
        );
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        if (state.getStatus() == State.Status.COMPLETE) {
            return Optional.of(diceParserHelper.roll(state.getFilledExpression(), null));
        }
        return Optional.empty();
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new Config(customIdSplit);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new State(customIdSplit, event.getInvokingGuildMemberName());
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION).orElse("");
        Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);
        return new Config(baseExpression, answerTargetChannelId.orElse(null));
    }

    @Override
    protected MessageDefinition createNewButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content(String.format("%s: Please select value for %s", config.getBaseExpression(), config.getFirstParameterName()))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                .build();
    }

    @Override
    protected Optional<Long> getAnswerTargetChannelId(Config config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(State state, Config config) {
        if (state.getStatus() == State.Status.COMPLETE ||
                state.getStatus() == State.Status.NO_ACTION) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithOptionalState(config, state));
    }

    @Override
    protected Optional<String> getCurrentMessageContentChange(State state, Config config) {
        if (state.getStatus() == State.Status.COMPLETE ||
                state.getStatus() == State.Status.NO_ACTION) {
            return Optional.empty();

        }
        String cleanName = Optional.ofNullable(state.getLockedForUserName()).map(n -> String.format("%s:", n)).orElse("");
        return Optional.of(String.format("%s%s: Please select value for %s", cleanName, state.getFilledExpression(), state.getCurrentParameterName()));
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, Config config) {
        if (state.getStatus() == State.Status.COMPLETE) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("%s: Please select value for %s", config.getBaseExpression(), config.getFirstParameterName()))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                    .build());
        }
        return Optional.empty();
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull Config config, @Nullable State state) {
        List<ButtonDefinition> buttons = IntStream.range(1, 25)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(createButtonCustomId(String.valueOf(i), config, state))
                        .label(i + "")
                        .build())
                .collect(Collectors.toList());
        buttons.add(ButtonDefinition.builder()
                .id(createButtonCustomId(CLEAR_BUTTON_ID, config, state))
                .label("Clear")
                .style(ButtonDefinition.Style.DANGER)
                .build());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    String createButtonCustomId(@NonNull String buttonValue, @NonNull Config config, @Nullable State state) {
        Preconditions.checkArgument(!buttonValue.contains(BotConstants.CONFIG_DELIMITER));

        String selectedParameterString = Optional.ofNullable(state).map(State::toIdString).orElse(BotConstants.CONFIG_DELIMITER);
        return String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, config.toIdString(), selectedParameterString, buttonValue);
    }

    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
        Config conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    Optional<String> validate(Config config) {

        return Optional.empty();
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    static class State extends CustomParameter implements IState {
        private static final String SELECTED_PARAMETER_DELIMITER = "\t";
        @NonNull
        List<String> selectedParameterValues;
        @NonNull
        @EqualsAndHashCode.Exclude
        String filledExpression;
        @NonNull
        @EqualsAndHashCode.Exclude
        Status status;
        @EqualsAndHashCode.Exclude
        String currentParameterExpression; //null if expression is complete
        @EqualsAndHashCode.Exclude
        String currentParameterName; //null if expression is complete
        @Nullable
        String lockedForUserName;

        public State(@NonNull String[] customIdComponents, String invokingUser) {
            String baseString = customIdComponents[1];
            String alreadySelectedParameter = customIdComponents[3];
            String lockedToUser = Strings.emptyToNull(customIdComponents[4]);
            String buttonValue = customIdComponents[5];

            if (CLEAR_BUTTON_ID.equals(buttonValue)) {
                this.selectedParameterValues = ImmutableList.of();
                this.lockedForUserName = null;
            } else {
                this.selectedParameterValues = ImmutableList.<String>builder()
                        .addAll(Arrays.stream(alreadySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                                .filter(s -> !Strings.isNullOrEmpty(s))
                                .collect(Collectors.toList()))
                        .add(buttonValue)
                        .build();
                this.lockedForUserName = Optional.ofNullable(lockedToUser).orElse(invokingUser);
            }
            this.filledExpression = getFilledExpression(baseString, selectedParameterValues);

            if (lockedForUserName != null && !lockedForUserName.equals(invokingUser)) {
                this.status = Status.NO_ACTION;
            } else if (selectedParameterValues.isEmpty()) {
                this.status = Status.CLEAR;
            } else if (hasMissingParameter(filledExpression)) {
                this.status = Status.IN_SELECTION;
            } else {
                this.status = Status.COMPLETE;
            }
            this.currentParameterExpression = getCurrentParameterExpression(filledExpression);
            this.currentParameterName = removeBrackets(currentParameterExpression);

        }

        public String toIdString() {
            return String.join(BotConstants.CONFIG_DELIMITER, String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues), Strings.nullToEmpty(lockedForUserName));
        }

        private String getFilledExpression(String baseExpression, List<String> selectedParameterValues) {
            String filledExpression = baseExpression;
            for (String parameterValue : selectedParameterValues) {
                String nextParameterName = getCurrentParameterExpression(filledExpression);
                if (nextParameterName != null) {
                    filledExpression = filledExpression.replace(nextParameterName, parameterValue);
                }
            }
            return filledExpression;
        }

        @Override
        public String toShortString() {
            return ImmutableList.<String>builder()
                    .addAll(selectedParameterValues)
                    .add(Optional.ofNullable(lockedForUserName).orElse(""))
                    .build().toString();
        }

        private enum Status {
            IN_SELECTION,
            COMPLETE,
            CLEAR,
            NO_ACTION
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    protected static class Config extends CustomParameter implements IConfig {
        @NonNull
        String baseExpression;
        Long answerTargetChannelId;
        @NonNull
        @EqualsAndHashCode.Exclude
        String firstParameterName;

        public Config(@NonNull String[] customIdComponents) {
            this.baseExpression = customIdComponents[1];
            this.answerTargetChannelId = getOptionalLongFromArray(customIdComponents, 2);
            this.firstParameterName = removeBrackets(getCurrentParameterExpression(baseExpression));
        }

        public Config(@NonNull String baseExpression, Long answerTargetChannelId) {
            this.baseExpression = baseExpression;
            this.answerTargetChannelId = answerTargetChannelId;
            this.firstParameterName = removeBrackets(getCurrentParameterExpression(baseExpression));

        }

        public String toIdString() {
            return String.join(BotConstants.CONFIG_DELIMITER, baseExpression, Optional.ofNullable(answerTargetChannelId).map(Objects::toString).orElse(""));
        }

        @Override
        public String toShortString() {
            return ImmutableList.of(baseExpression, targetChannelToString(answerTargetChannelId)).toString();
        }
    }

    @EqualsAndHashCode
    private abstract static class CustomParameter {
        protected static @Nullable String getCurrentParameterExpression(@NonNull String expression) {
            Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
            if (matcher.find()) {
                return matcher.group(0);
            }
            return null;
        }

        protected static String removeBrackets(@Nullable String input) {
            if (input == null) {
                return null;
            }
            return input.replace("{", "").replace("}", "");
        }

        protected static boolean hasMissingParameter(@NonNull String expression) {
            return PARAMETER_VARIABLE_PATTERN.matcher(expression).find();
        }
    }
}
