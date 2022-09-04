package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
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
public class CustomParameterCommand extends AbstractCommand<CustomParameterConfig, CustomParameterStateData> {

    //todo button label, pagination for buttons

    static final String CLEAR_BUTTON_ID = "clear";
    final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    static final String LOCKED_USER_NAME_DELIMITER = "\u2236"; //"∶" Ratio
    static final String SELECTED_PARAMETER_DELIMITER = "\t";
    private static final String COMMAND_NAME = "custom_parameter";
    private static final String EXPRESSION_OPTION = "expression";
    private static final String RANGE_DELIMITER = ":";
    final static String RANGE_REPLACE_REGEX = RANGE_DELIMITER + ".+?(?=\\Q}\\E)";
    private static final String LABEL_DELIMITER = "@";
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}");
    private static final String STATE_DATA_TYPE_ID = "CustomParameterStateData";
    private static final String CONFIG_TYPE_ID = "CustomParameterConfig";
    private final DiceParserHelper diceParserHelper;

    public CustomParameterCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomParameterCommand(MessageDataDAO messageDataDAO, DiceParserHelper diceParserHelper) {
        super(messageDataDAO);
        this.diceParserHelper = diceParserHelper;
    }

    private static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }


    private static String cleanupExpressionForDisplay(String expression) {
        return expression
                .replaceAll(RANGE_REPLACE_REGEX, "")
                .replace("{", "*{")
                .replace("}", "}*");
    }

    private static List<String> getSelectedParameterValues(String buttonValue, List<String> alreadySelectedParameter, String lockedForUserName, @NonNull String invokingUser) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<String> selectedParameterBuilder =
                ImmutableList.<String>builder()
                        .addAll(alreadySelectedParameter);
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
    static State<CustomParameterStateData> createParameterStateFromLegacyId(String customId, String messageContent, String invokingUser) {
        String[] split = splitCustomId(customId);
        String buttonValue = split[CustomIdIndex.BUTTON_VALUE.index];
        String currentlySelectedParameter = split[CustomIdIndex.SELECTED_PARAMETER.index];
        List<String> currentlySelectedParameterList = Arrays.stream(currentlySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                .filter(s -> !Strings.isNullOrEmpty(s))
                .toList();
        String lockedForUserName = getUserNameFromMessage(messageContent);


        return new State<>(buttonValue, updateState(currentlySelectedParameterList, buttonValue, lockedForUserName, invokingUser));
    }

    private static CustomParameterStateData updateState(@NonNull List<String> currentlySelectedParameterList,
                                                        @NonNull String buttonValue,
                                                        @Nullable String currentlyLockedForUser,
                                                        @NonNull String invokingUser) {
        final String shouldBeLockedForUser;
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            shouldBeLockedForUser = null;
        } else {
            shouldBeLockedForUser = Optional.ofNullable(currentlyLockedForUser).orElse(invokingUser);
        }
        List<String> selectedParameterValues = getSelectedParameterValues(buttonValue, currentlySelectedParameterList, shouldBeLockedForUser, invokingUser);
        return new CustomParameterStateData(selectedParameterValues, shouldBeLockedForUser);
    }

    @VisibleForTesting
    static String getFilledExpression(CustomParameterConfig config, State<CustomParameterStateData> state) {
        String filledExpression = config.getBaseExpression();
        List<String> selectedParameter = Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(ImmutableList.of());
        for (String parameterValue : selectedParameter) {
            String nextParameterName = getNextParameterExpression(filledExpression);
            filledExpression = filledExpression.replace(nextParameterName, parameterValue);
        }
        return filledExpression;
    }

    @VisibleForTesting
    static String getCurrentParameterExpression(CustomParameterConfig config, State<CustomParameterStateData> state) {
        String filledExpression = getFilledExpression(config, state);
        return hasMissingParameter(filledExpression) ? getNextParameterExpression(filledExpression) : null;
    }

    @VisibleForTesting
    static String getCurrentParameterName(CustomParameterConfig config, State<CustomParameterStateData> state) {
        String currentParameterExpression = getCurrentParameterExpression(config, state);
        return currentParameterExpression != null ? cleanupExpressionForDisplay(currentParameterExpression) : null;
    }

    private static String[] splitCustomId(String customId) {
        return customId.split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
    }


    @VisibleForTesting
    static CustomParameterConfig createConfigFromCustomId(String customId) {
        String[] split = splitCustomId(customId);
        return new CustomParameterConfig(getOptionalLongFromArray(split, CustomIdIndex.ANSWER_TARGET_CHANNEL.index), split[CustomIdIndex.BASE_EXPRESSION.index]);
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Fill custom parameter of a given dice expression and roll it when all parameter are provided";
    }

    @Override
    protected @NonNull EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/custom_parameter start' and provide a dice expression with parameter variables with the format {parameter_name}. \n" + DiceParserHelper.HELP)
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION)
                        .required(true)
                        .description("Expression")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<EmbedDefinition> getAnswer(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            String expression = DiceParserHelper.getExpressionFromExpressionWithOptionalLabel(getFilledExpression(config, state), LABEL_DELIMITER);
            String label = DiceParserHelper.getLabelFromExpressionWithOptionalLabel(getFilledExpression(config, state), LABEL_DELIMITER).orElse(null);
            return Optional.of(diceParserHelper.roll(expression, label));
        }
        return Optional.empty();
    }

    @Override
    protected @NonNull CustomParameterConfig getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        return createConfigFromCustomId(event.getCustomId());
    }

    @Override
    protected @NonNull State<CustomParameterStateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        return createParameterStateFromLegacyId(event.getCustomId(), event.getMessageContent(), event.getInvokingGuildMemberName());
    }

    @Override
    protected @NonNull CustomParameterConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION).orElse("");
        Optional<Long> answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options);

        return new CustomParameterConfig(answerTargetChannelId.orElse(null), baseExpression);
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(CustomParameterConfig config) {
        return MessageDefinition.builder()
                .content(String.format("%s: Please select value for %s", cleanupExpressionForDisplay(config.getBaseExpression()), cleanupExpressionForDisplay(getNextParameterExpression(config.getBaseExpression()))))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                .build();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithOptionalState(config, state));
    }

    @Override
    protected Optional<ConfigAndState<CustomParameterConfig, CustomParameterStateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                                               long messageId,
                                                                                                                               @NonNull String buttonValue,
                                                                                                                               @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        if (messageDataDTO.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deserializeAndUpdateState(messageDataDTO.get(), buttonValue, invokingUserName));
    }

    @VisibleForTesting
    ConfigAndState<CustomParameterConfig, CustomParameterStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO,
                                                                                              @NonNull String buttonValue,
                                                                                              @NonNull String invokingUser) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        Preconditions.checkArgument(STATE_DATA_TYPE_ID.equals(messageDataDTO.getStateDataClassId())
                || Mapper.NO_PERSISTED_STATE.equals(messageDataDTO.getStateDataClassId()), "Unknown stateDataClassId: %s", messageDataDTO.getStateDataClassId());

        final CustomParameterStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, CustomParameterStateData.class))
                .orElse(null);
        final CustomParameterConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), CustomParameterConfig.class);
        final CustomParameterStateData updatedStateData = updateState(
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getSelectedParameterValues).orElse(ImmutableList.of()),
                buttonValue,
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getLockedForUserName).orElse(null),
                invokingUser
        );
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                loadedConfig,
                new State<>(buttonValue, updatedStateData));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID, long channelId, long messageId, @NonNull CustomParameterConfig config, @Nullable State<CustomParameterStateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, channelId, messageId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull CustomParameterConfig config, @NonNull State<CustomParameterStateData> state) {
        if (state.getData() == null || !hasMissingParameter(getFilledExpression(config, state))) {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, Mapper.NO_PERSISTED_STATE, null);
        } else {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData()));
        }
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.empty();
        }
        String cleanName = Optional.ofNullable(state.getData())
                .map(CustomParameterStateData::getLockedForUserName)
                .map(n -> String.format("%s%s", n, LOCKED_USER_NAME_DELIMITER))
                .orElse("");
        return Optional.of(String.format("%s%s: Please select value for %s", cleanName, cleanupExpressionForDisplay(getFilledExpression(config, state)), getCurrentParameterName(config, state)));
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(getFilledExpression(config, state))) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("%s: Please select value for %s", cleanupExpressionForDisplay(config.getBaseExpression()), cleanupExpressionForDisplay(getNextParameterExpression(config.getBaseExpression()))))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                    .build());
        }
        return Optional.empty();
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull CustomParameterConfig config, @Nullable State<CustomParameterStateData> state) {
        String parameterExpression = Optional.ofNullable(state)
                .map(s -> getCurrentParameterExpression(config, s))
                .orElse(getNextParameterExpression(config.getBaseExpression()));
        List<String> buttonValues = getButtonValues(parameterExpression);
        List<ButtonDefinition> buttons = buttonValues.stream()
                .map(v -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), v))
                        .label(v)
                        .build())
                .collect(Collectors.toList());
        List<String> selectedParameter = Optional.ofNullable(state)
                .map(State::getData)
                .map(CustomParameterStateData::getSelectedParameterValues)
                .orElse(ImmutableList.of());
        if (state != null && !selectedParameter.isEmpty()) {
            buttons.add(ButtonDefinition.builder()
                    .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID))
                    .label("Clear")
                    .style(ButtonDefinition.Style.DANGER)
                    .build());
        }
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }


    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
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
        if (baseExpression.length() > 1000) { //max length of the message content, where the current state is given is 2000
            return Optional.of(String.format("The expression has %s to many characters", (baseExpression.length() - 1000)));
        }
        if (baseExpression.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
            return Optional.of(String.format("Expression contains invalid character: '%s'", BottomCustomIdUtils.CUSTOM_ID_DELIMITER));
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
                Optional<String> validationMessage = diceParserHelper.validateDiceExpressionWitOptionalLabel(getFilledExpression(config, aState.getState()), "@", "/custom_parameter help", Integer.MAX_VALUE);
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
            String customId = BottomCustomIdUtils.createButtonCustomId(getCommandId(), parameterValue);
            State<CustomParameterStateData> nextState = new State<>(parameterValue, updateState(ImmutableList.of(), parameterValue, null, "test"));
            out.add(new StateWithCustomIdAndParameter(customId, nextState, parameterValues));
            out.addAll(allPossibleStatePermutations(config, nextState));
        }
        return out;
    }


    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config, State<CustomParameterStateData> state) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        if (hasMissingParameter(getFilledExpression(config, state))) {
            String parameterExpression = getCurrentParameterExpression(config, state);
            List<String> parameterValues = getButtonValues(parameterExpression);
            for (String parameterValue : parameterValues) {
                String customId = BottomCustomIdUtils.createButtonCustomId(getCommandId(), parameterValue);
                State<CustomParameterStateData> nextState = new State<>(parameterValue,
                        updateState(Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(ImmutableList.of()),
                                parameterValue, null, "test"));
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
        State<CustomParameterStateData> state;
        @NotNull
        List<String> parameter;
    }
}
