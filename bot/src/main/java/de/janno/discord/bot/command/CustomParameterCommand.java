package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterCommand.Config, CustomParameterCommand.State> {

    //todo button label, pagination for buttons


    private static final String COMMAND_NAME = "custom_parameter";
    private static final String EXPRESSION_OPTION = "expression";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String LOCKED_USER_NAME_DELIMITER = "\u2236"; //"∶" Ratio
    private static final String RANGE_DELIMITER = ":";
    private static final String LABEL_DELIMITER = "@";
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}");
    private final static String RANGE_REPLACE_REGEX = RANGE_DELIMITER + ".+?(?=\\Q}\\E)";

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
        return "Fill custom parameter of a given dice expression and roll it when all parameter are provided";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/custom_parameter start' and provide a dice expression with parameter variables with the format {parameter_name}. \n" + DiceParserHelper.HELP)
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
        if (!state.hasMissingParameter()) {
            String expression = DiceParserHelper.getExpressionFromExpressionWithOptionalLabel(state.getFilledExpression(), LABEL_DELIMITER);
            String label = DiceParserHelper.getLabelFromExpressionWithOptionalLabel(state.getFilledExpression(), LABEL_DELIMITER).orElse(null);
            return Optional.of(diceParserHelper.roll(expression, label));
        }
        return Optional.empty();
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        return new Config(splitCustomId(event.getCustomId()));
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        return new State(splitCustomId(event.getCustomId()), event.getMessageContent(), event.getInvokingGuildMemberName());
    }

    private String[] splitCustomId(String customId) {
        return customId.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
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
                .content(String.format("%s: Please select value for %s", config.baseExpressionWithoutRange(), config.getFirstParameterName()))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                .build();
    }

    @Override
    protected Optional<Long> getAnswerTargetChannelId(Config config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(State state, Config config) {
        if (!state.hasMissingParameter()) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithOptionalState(config, state));
    }

    @Override
    protected Optional<String> getCurrentMessageContentChange(State state, Config config) {
        if (!state.hasMissingParameter()) {
            return Optional.empty();
        }
        String cleanName = Optional.ofNullable(state.getLockedForUserName()).map(n -> String.format("%s%s", n, LOCKED_USER_NAME_DELIMITER)).orElse("");
        return Optional.of(String.format("%s%s: Please select value for %s", cleanName, state.filledExpressionWithoutRange(), state.getCurrentParameterName()));
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, Config config) {
        if (!state.hasMissingParameter()) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("%s: Please select value for %s", config.baseExpressionWithoutRange(), config.getFirstParameterName()))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                    .build());
        }
        return Optional.empty();
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull Config config, @Nullable State state) {
        String parameterExpression = Optional.ofNullable(state)
                .map(State::getCurrentParameterExpression)
                .orElse(config.getFirstParameterExpression());
        List<String> buttonValues = getButtonValues(parameterExpression);
        List<ButtonDefinition> buttons = buttonValues.stream()
                .map(v -> ButtonDefinition.builder()
                        .id(createButtonCustomId(v, config, state))
                        .label(v)
                        .build())
                .collect(Collectors.toList());
        if (state != null && !state.getSelectedParameterValues().isEmpty()) {
            buttons.add(ButtonDefinition.builder()
                    .id(createButtonCustomId(CLEAR_BUTTON_ID, config, state))
                    .label("Clear")
                    .style(ButtonDefinition.Style.DANGER)
                    .build());
        }
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    String createButtonCustomId(@NonNull String buttonValue, @NonNull Config config, @Nullable State state) {
        Collection<CustomIdIndexWithValue> stateIdComponents = Optional.ofNullable(state).map(State::getIdComponents).orElse(ImmutableList.of(new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, "")));
        String[] values = new String[5];
        values[0] = COMMAND_NAME;
        stateIdComponents.forEach(c -> c.addToArray(values));
        config.getIdComponents().forEach(c -> c.addToArray(values));
        values[CustomIdIndex.BUTTON_VALUE.index] = buttonValue;
        return String.join(BotConstants.CONFIG_DELIMITER, values);
    }

    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION).orElse("");
        if (!PARAMETER_VARIABLE_PATTERN.matcher(baseExpression).find()) {
            return Optional.of("The expression needs at least one parameter expression like '{name}");
        }
        if (Pattern.compile("(\\Q{\\E(?)\\Q{\\E(?)(.*)(?)\\Q}\\E(?)\\Q}\\E)").matcher(baseExpression).find()) {
            return Optional.of("Nested brackets are not allowed");
        }
        if (StringUtils.countMatches(baseExpression, "{") != StringUtils.countMatches(baseExpression, "}")) {
            return Optional.of("All brackets must be closed");
        }
        if (baseExpression.contains("{}")) {
            return Optional.of("A parameter expression must not be empty");
        }
        if (baseExpression.contains(BotConstants.LEGACY_DELIMITER)) {
            return Optional.of(String.format("Expression contains invalid character: '%s'", BotConstants.LEGACY_DELIMITER));
        }
        if (baseExpression.contains(BotConstants.CONFIG_DELIMITER)) {
            return Optional.of(String.format("Expression contains invalid character: '%s'", BotConstants.CONFIG_DELIMITER));
        }
        if (baseExpression.contains(State.SELECTED_PARAMETER_DELIMITER)) {
            return Optional.of(String.format("Expression contains invalid character: '%s'", State.SELECTED_PARAMETER_DELIMITER));
        }
        Config config = getConfigFromStartOptions(options);
        if (getButtonValues(config.getFirstParameterExpression()).isEmpty()) {
            return Optional.of(String.format("The expression '%s' contains no valid parameter options", config.getFirstParameterExpression()));
        }
        return validateAllPossibleStates(config);
    }

    private Optional<String> validateAllPossibleStates(Config config) {

        List<StateWithCustomIdAndParameter> allPossibleStatePermutations = allPossibleStatePermutations(config);
        for (StateWithCustomIdAndParameter aState : allPossibleStatePermutations) {
            String customId = aState.getCustomId();
            if (customId.length() > 100) {
                return Optional.of(String.format("The following expression with parameters is %d to long: %s", (customId.length() - 100), aState.getState().getFilledExpression()));
            }
            if (aState.getParameter().size() != ImmutableSet.copyOf(aState.getParameter()).size()) {
                return Optional.of(String.format("Parameter '%s' contains duplicate parameter option but they must be unique.", aState.getParameter()));
            }
            if (!aState.getState().hasMissingParameter()) {
                Optional<String> validationMessage = diceParserHelper.validateDiceExpressionWitOptionalLabel(aState.getState().getFilledExpression(), "@", "/custom_parameter help", 100);
                if (validationMessage.isPresent()) {
                    return validationMessage;
                }
            }
            if (aState.getState().hasMissingParameter() && getButtonValues(aState.getState().getCurrentParameterExpression()).isEmpty()) {
                return Optional.of(String.format("The expression '%s' contains no valid parameter options", aState.getState().getCurrentParameterExpression()));
            }
        }
        return Optional.empty();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(Config config) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        String parameterExpression = config.getFirstParameterExpression();
        List<String> parameterValues = getButtonValues(parameterExpression);
        for (String parameterValue : parameterValues) {
            String customId = createButtonCustomId(parameterValue, config, null);
            State nextState = new State(splitCustomId(customId), "", "");
            out.add(new StateWithCustomIdAndParameter(customId, nextState, parameterValues));
            out.addAll(allPossibleStatePermutations(config, nextState));
        }
        return out;
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(Config config, State state) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        if (state.hasMissingParameter()) {
            String parameterExpression = state.getCurrentParameterExpression();
            List<String> parameterValues = getButtonValues(parameterExpression);
            for (String parameterValue : parameterValues) {
                String customId = createButtonCustomId(parameterValue, config, state);
                State nextState = new State(splitCustomId(customId), "", "");
                out.add(new StateWithCustomIdAndParameter(customId, nextState, parameterValues));
                out.addAll(allPossibleStatePermutations(config, nextState));
            }
        }
        return out;
    }

    @VisibleForTesting
    @NonNull List<String> getButtonValues(@NonNull String currentParameterExpression) {
        Matcher matcher = BUTTON_VALUE_PATTERN.matcher(currentParameterExpression);
        if (BUTTON_RANGE_PATTERN.matcher(currentParameterExpression).find()) {
            int min = getMinButtonFrom(currentParameterExpression);
            int max = getMaxButtonFrom(currentParameterExpression);
            return IntStream.range(min, max + 1).mapToObj(String::valueOf).collect(Collectors.toList());
        } else if (matcher.find()) {
            String buttonValueExpression = matcher.group(1);
            return Arrays.stream(buttonValueExpression.split(BUTTON_VALUE_DELIMITER)).limit(23).toList();
        }
        return IntStream.range(1, 16).mapToObj(String::valueOf).collect(Collectors.toList());
    }

    @VisibleForTesting
    int getMinButtonFrom(String currentParameterExpression) {
        Matcher matcher = BUTTON_RANGE_PATTERN.matcher(currentParameterExpression);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    @VisibleForTesting
    int getMaxButtonFrom(String currentParameterExpression) {
        Matcher matcher = BUTTON_RANGE_PATTERN.matcher(currentParameterExpression);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            return Math.min(Math.max(min, max), min + 23);
        }
        return 15;
    }

    private enum CustomIdIndex {
        BASE_EXPRESSION(1),
        ANSWER_TARGET_CHANNEL(2),
        SELECTED_PARAMETER(3),
        BUTTON_VALUE(4);


        private final int index;

        CustomIdIndex(int index) {
            this.index = index;
        }
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
        @EqualsAndHashCode.Exclude
        String currentParameterExpression; //null if expression is complete
        @EqualsAndHashCode.Exclude
        String currentParameterName; //null if expression is complete
        @Nullable
        String lockedForUserName;

        public State(@NonNull String[] customIdComponents, String messageContent, String invokingUser) {
            String baseString = customIdComponents[CustomIdIndex.BASE_EXPRESSION.index];
            String alreadySelectedParameter = customIdComponents[CustomIdIndex.SELECTED_PARAMETER.index];
            String buttonValue = customIdComponents[CustomIdIndex.BUTTON_VALUE.index];

            if (CLEAR_BUTTON_ID.equals(buttonValue)) {
                this.selectedParameterValues = ImmutableList.of();
                this.lockedForUserName = null;
            } else {
                ImmutableList.Builder<String> selectedParameterBuilder =
                        ImmutableList.<String>builder()
                                .addAll(Arrays.stream(alreadySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                                        .filter(s -> !Strings.isNullOrEmpty(s))
                                        .collect(Collectors.toList()));
                this.lockedForUserName = Optional.ofNullable(getUserNameFromMessage(messageContent)).orElse(invokingUser);
                if (lockedForUserName == null || lockedForUserName.equals(invokingUser)) {
                    selectedParameterBuilder.add(buttonValue);
                }
                this.selectedParameterValues = selectedParameterBuilder.build();
            }
            this.filledExpression = getFilledExpression(baseString, selectedParameterValues);

            this.currentParameterExpression = hasMissingParameter(filledExpression) ? getNextParameterExpression(filledExpression) : null;

            this.currentParameterName = currentParameterExpression != null ? cleanupExpressionForDisplay(currentParameterExpression) : null;
        }

        private static boolean hasMissingParameter(@NonNull String expression) {
            return PARAMETER_VARIABLE_PATTERN.matcher(expression).find();
        }

        public boolean hasMissingParameter() {
            return hasMissingParameter(filledExpression);
        }


        private String getUserNameFromMessage(@NonNull String messageContent) {
            if (messageContent.contains(LOCKED_USER_NAME_DELIMITER)) {
                return messageContent.split(LOCKED_USER_NAME_DELIMITER)[0];
            }
            return null;
        }

        public Collection<CustomIdIndexWithValue> getIdComponents() {
            return ImmutableList.of(
                    new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues))
            );
        }

        private String getFilledExpression(String baseExpression, List<String> selectedParameterValues) {
            String filledExpression = baseExpression;
            for (String parameterValue : selectedParameterValues) {
                String nextParameterName = getNextParameterExpression(filledExpression);
                filledExpression = filledExpression.replace(nextParameterName, parameterValue);
            }
            return filledExpression;
        }

        public String filledExpressionWithoutRange() {
            return cleanupExpressionForDisplay(filledExpression);
        }

        @Override
        public String toShortString() {
            return ImmutableList.<String>builder()
                    .addAll(selectedParameterValues)
                    .add(Optional.ofNullable(lockedForUserName).orElse(""))
                    .build().toString();
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
        @NonNull
        @EqualsAndHashCode.Exclude
        String firstParameterExpression;

        public Config(@NonNull String[] customIdComponents) {
            this(customIdComponents[CustomIdIndex.BASE_EXPRESSION.index], getOptionalLongFromArray(customIdComponents, 2));
        }

        public Config(@NonNull String baseExpression, Long answerTargetChannelId) {
            this.baseExpression = baseExpression;
            this.answerTargetChannelId = answerTargetChannelId;
            this.firstParameterExpression = getNextParameterExpression(baseExpression);
            this.firstParameterName = cleanupExpressionForDisplay(firstParameterExpression);
        }

        public Collection<CustomIdIndexWithValue> getIdComponents() {
            return ImmutableList.of(
                    new CustomIdIndexWithValue(CustomIdIndex.BASE_EXPRESSION, baseExpression),
                    new CustomIdIndexWithValue(CustomIdIndex.ANSWER_TARGET_CHANNEL, Optional.ofNullable(answerTargetChannelId).map(Objects::toString).orElse(""))
            );
        }

        @Override
        public String toShortString() {
            return ImmutableList.of(baseExpression, targetChannelToString(answerTargetChannelId)).toString();
        }

        public String baseExpressionWithoutRange() {
            return cleanupExpressionForDisplay(baseExpression);
        }
    }

    @EqualsAndHashCode
    private abstract static class CustomParameter {

        protected static String cleanupExpressionForDisplay(String expression) {
            return expression
                    .replaceAll(RANGE_REPLACE_REGEX, "")
                    .replace("{", "*{")
                    .replace("}", "}*");
        }

        protected static @NonNull String getNextParameterExpression(@NonNull String expression) {
            Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
            if (matcher.find()) {
                return matcher.group(0);
            }
            throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
        }
    }

    @Value
    private static class StateWithCustomIdAndParameter {
        @NonNull
        String customId;
        @NonNull
        State state;
        @NotNull
        List<String> parameter;
    }

    @Value
    private static class CustomIdIndexWithValue {
        @NonNull
        CustomIdIndex customIdIndex;
        @NonNull
        String value;

        public void addToArray(String[] array) {
            array[customIdIndex.index] = value;
        }
    }
}
