package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
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
public class CustomParameterCommand extends AbstractCommand<CustomParameterConfig, CustomParameterState> {

    //todo button label, pagination for buttons


    static final String CLEAR_BUTTON_ID = "clear";
    final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    static final String LOCKED_USER_NAME_DELIMITER = "\u2236"; //"∶" Ratio
    static final String SELECTED_PARAMETER_DELIMITER = "\t";
    private static final String COMMAND_NAME = "custom_parameter";
    private static final String EXPRESSION_OPTION = "expression";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private static final String RANGE_DELIMITER = ":";
    final static String RANGE_REPLACE_REGEX = RANGE_DELIMITER + ".+?(?=\\Q}\\E)";
    private static final String LABEL_DELIMITER = "@";
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}");
    private final DiceParserHelper diceParserHelper;

    public CustomParameterCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomParameterCommand(DiceParserHelper diceParserHelper) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceParserHelper = diceParserHelper;
    }

    private static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }

    private static String getLockedForUserName(String buttonValue, String messageContent, String invokingUser) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return null;
        }
        return Optional.ofNullable(getUserNameFromMessage(messageContent)).orElse(invokingUser);
    }

    private static String cleanupExpressionForDisplay(String expression) {
        return expression
                .replaceAll(RANGE_REPLACE_REGEX, "")
                .replace("{", "*{")
                .replace("}", "}*");
    }

    private static List<String> getSelectedParameterValues(String buttonValue, String alreadySelectedParameter, String lockedForUserName, @NonNull String invokingUser) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<String> selectedParameterBuilder =
                ImmutableList.<String>builder()
                        .addAll(Arrays.stream(alreadySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                                .filter(s -> !Strings.isNullOrEmpty(s))
                                .collect(Collectors.toList()));
        if (lockedForUserName.equals(invokingUser)) {
            selectedParameterBuilder.add(buttonValue);
        }
        return selectedParameterBuilder.build();
    }

    private static String getUserNameFromMessage(@NonNull String messageContent) {
        if (messageContent.contains(LOCKED_USER_NAME_DELIMITER)) {
            return messageContent.split(LOCKED_USER_NAME_DELIMITER)[0];
        }
        return null;
    }

    @VisibleForTesting
    static boolean hasMissingParameter(@NonNull String expression) {
        return PARAMETER_VARIABLE_PATTERN.matcher(expression).find();
    }

    @VisibleForTesting
    static CustomParameterState createParameterState(String customId, String messageContent, String invokingUser) {
        String[] split = splitCustomId(customId);
        String buttonValue = split[CustomIdIndex.BUTTON_VALUE.index];
        String alreadySelectedParameter = split[CustomIdIndex.SELECTED_PARAMETER.index];
        String lockedForUserName = getLockedForUserName(buttonValue, messageContent, invokingUser);
        List<String> selectedParameterValues = getSelectedParameterValues(buttonValue, alreadySelectedParameter, lockedForUserName, invokingUser);

        return new CustomParameterState(buttonValue, selectedParameterValues, lockedForUserName);
    }

    @VisibleForTesting
    static String getFilledExpression(CustomParameterConfig config, CustomParameterState state) {
        String filledExpression = config.getBaseExpression();
        for (String parameterValue : state.getSelectedParameterValues()) {
            String nextParameterName = getNextParameterExpression(filledExpression);
            filledExpression = filledExpression.replace(nextParameterName, parameterValue);
        }
        return filledExpression;
    }

    @VisibleForTesting
    static String getCurrentParameterExpression(CustomParameterConfig config, CustomParameterState state) {
        String filledExpression = getFilledExpression(config, state);
        return hasMissingParameter(filledExpression) ? getNextParameterExpression(filledExpression) : null;
    }

    @VisibleForTesting
    static String getCurrentParameterName(CustomParameterConfig config, CustomParameterState state) {
        String currentParameterExpression = getCurrentParameterExpression(config, state);
        return currentParameterExpression != null ? cleanupExpressionForDisplay(currentParameterExpression) : null;
    }

    private static String[] splitCustomId(String customId) {
        return customId.split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
    }


    @VisibleForTesting
    static CustomParameterConfig createConfigFromCustomId(String customId) {
        String[] split = splitCustomId(customId);
        return new CustomParameterConfig(getOptionalLongFromArray(split, 2), split[CustomIdIndex.BASE_EXPRESSION.index]);
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
    protected Optional<EmbedDefinition> getAnswer(CustomParameterState state, CustomParameterConfig config) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            String expression = DiceParserHelper.getExpressionFromExpressionWithOptionalLabel(getFilledExpression(config, state), LABEL_DELIMITER);
            String label = DiceParserHelper.getLabelFromExpressionWithOptionalLabel(getFilledExpression(config, state), LABEL_DELIMITER).orElse(null);
            return Optional.of(diceParserHelper.roll(expression, label));
        }
        return Optional.empty();
    }

    @Override
    protected CustomParameterConfig getConfigFromEvent(IButtonEventAdaptor event) {
        return createConfigFromCustomId(event.getCustomId());
    }

    @Override
    protected CustomParameterState getStateFromEvent(IButtonEventAdaptor event) {
        return createParameterState(event.getCustomId(), event.getMessageContent(), event.getInvokingGuildMemberName());
    }

    @Override
    protected CustomParameterConfig getConfigFromStartOptions(CommandInteractionOption options) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION).orElse("");
        Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);

        return new CustomParameterConfig(answerTargetChannelId.orElse(null), baseExpression);
    }

    @Override
    public MessageDefinition createNewButtonMessage(CustomParameterConfig config) {
        return MessageDefinition.builder()
                .content(String.format("%s: Please select value for %s", cleanupExpressionForDisplay(config.getBaseExpression()), cleanupExpressionForDisplay(getNextParameterExpression(config.getBaseExpression()))))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                .build();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(CustomParameterState state, CustomParameterConfig config) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithOptionalState(config, state));
    }

    @Override
    public Optional<String> getCurrentMessageContentChange(CustomParameterState state, CustomParameterConfig config) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.empty();
        }
        String cleanName = Optional.ofNullable(state.getLockedForUserName()).map(n -> String.format("%s%s", n, LOCKED_USER_NAME_DELIMITER)).orElse("");
        return Optional.of(String.format("%s%s: Please select value for %s", cleanName, cleanupExpressionForDisplay(getFilledExpression(config, state)), getCurrentParameterName(config, state)));
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(CustomParameterState state, CustomParameterConfig config) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("%s: Please select value for %s", cleanupExpressionForDisplay(config.getBaseExpression()), cleanupExpressionForDisplay(getNextParameterExpression(config.getBaseExpression()))))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                    .build());
        }
        return Optional.empty();
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull CustomParameterConfig config, @Nullable CustomParameterState state) {
        String parameterExpression = Optional.ofNullable(state)
                .map(s -> getCurrentParameterExpression(config, s))
                .orElse(getNextParameterExpression(config.getBaseExpression()));
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

    String createButtonCustomId(@NonNull String buttonValue, @NonNull CustomParameterConfig config, @Nullable CustomParameterState state) {
        Collection<CustomIdIndexWithValue> stateIdComponents = Optional.ofNullable(state)
                .map(CustomParameterState::getIdComponents)
                .orElse(ImmutableList.of(new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, "")));
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
        if (baseExpression.contains(SELECTED_PARAMETER_DELIMITER)) {
            return Optional.of(String.format("Expression contains invalid character: '%s'", SELECTED_PARAMETER_DELIMITER));
        }
        CustomParameterConfig config = getConfigFromStartOptions(options);
        if (getButtonValues(getNextParameterExpression(config.getBaseExpression())).isEmpty()) {
            return Optional.of(String.format("The expression '%s' contains no valid parameter options", getNextParameterExpression(config.getBaseExpression())));
        }
        return validateAllPossibleStates(config);
    }

    private Optional<String> validateAllPossibleStates(CustomParameterConfig config) {

        List<StateWithCustomIdAndParameter> allPossibleStatePermutations = allPossibleStatePermutations(config);
        for (StateWithCustomIdAndParameter aState : allPossibleStatePermutations) {
            String customId = aState.getCustomId();
            if (customId.length() > 100) {
                return Optional.of(String.format("The following expression with parameters is %d to long: %s", (customId.length() - 100), getFilledExpression(config, aState.getState())));
            }
            if (aState.getParameter().size() != ImmutableSet.copyOf(aState.getParameter()).size()) {
                return Optional.of(String.format("Parameter '%s' contains duplicate parameter option but they must be unique.", aState.getParameter()));
            }
            if (!hasMissingParameter(getFilledExpression(config, aState.getState()))) {
                Optional<String> validationMessage = diceParserHelper.validateDiceExpressionWitOptionalLabel(getFilledExpression(config, aState.getState()), "@", "/custom_parameter help", 100);
                if (validationMessage.isPresent()) {
                    return validationMessage;
                }
            }
            if (hasMissingParameter(getFilledExpression(config, aState.getState())) && getButtonValues(getCurrentParameterExpression(config, aState.getState())).isEmpty()) {
                return Optional.of(String.format("The expression '%s' contains no valid parameter options", getCurrentParameterExpression(config, aState.getState())));
            }
        }
        return Optional.empty();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        String parameterExpression = getNextParameterExpression(config.getBaseExpression());
        List<String> parameterValues = getButtonValues(parameterExpression);
        for (String parameterValue : parameterValues) {
            String customId = createButtonCustomId(parameterValue, config, null);
            CustomParameterState nextState = createParameterState(customId, "", "");
            out.add(new StateWithCustomIdAndParameter(customId, nextState, parameterValues));
            out.addAll(allPossibleStatePermutations(config, nextState));
        }
        return out;
    }


    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config, CustomParameterState state) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        if (hasMissingParameter(getFilledExpression(config, state))) {
            String parameterExpression = getCurrentParameterExpression(config, state);
            List<String> parameterValues = getButtonValues(parameterExpression);
            for (String parameterValue : parameterValues) {
                String customId = createButtonCustomId(parameterValue, config, state);
                CustomParameterState nextState = createParameterState(customId, "", "");
                out.add(new StateWithCustomIdAndParameter(customId, nextState, parameterValues));
                out.addAll(allPossibleStatePermutations(config, nextState));
            }
        }
        return out;
    }

    @VisibleForTesting
    @NonNull List<String> getButtonValues(String currentParameterExpression) {
        if (currentParameterExpression == null) {
            return ImmutableList.of();
        }
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


    @Value
    private static class StateWithCustomIdAndParameter {
        @NonNull
        String customId;
        @NonNull
        CustomParameterState state;
        @NotNull
        List<String> parameter;
    }
}
