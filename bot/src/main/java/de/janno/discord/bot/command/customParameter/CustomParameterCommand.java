package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.ButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.random.NumberSupplier;
import de.janno.evaluator.dice.random.RandomNumberSupplier;
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
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}");
    private static final String STATE_DATA_TYPE_ID = "CustomParameterStateDataV2";
    private static final String STATE_DATA_TYPE_ID_LEGACY = "CustomParameterStateData";
    private static final String CONFIG_TYPE_ID = "CustomParameterConfig";
    private final DiceSystemAdapter diceSystemAdapter;

    public CustomParameterCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParser(), new RandomNumberSupplier(), 1000);
    }

    @VisibleForTesting
    public CustomParameterCommand(MessageDataDAO messageDataDAO, Dice dice, NumberSupplier randomNumberSupplier, int maxDiceRolls) {
        super(messageDataDAO);
        this.diceSystemAdapter = new DiceSystemAdapter(randomNumberSupplier, maxDiceRolls, dice);
    }

    private static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }


    private static String removeRange(String expression) {
        return expression.replaceAll(RANGE_REPLACE_REGEX, "");
    }

    private static String getUserNameFromMessage(@NonNull String messageContent) {
        if (messageContent.contains(LOCKED_USER_NAME_DELIMITER)) {
            return messageContent.split(LOCKED_USER_NAME_DELIMITER)[0];
        }
        return null;
    }

    @VisibleForTesting
    static boolean hasMissingParameter(@NonNull State<CustomParameterStateData> state) {
        return Optional.ofNullable(state.getData())
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .isPresent();
    }

    @VisibleForTesting
    static State<CustomParameterStateData> createParameterStateFromLegacyId(String customId, String messageContent, String invokingUser) {
        String[] split = splitCustomId(customId);
        String buttonValue = split[CustomIdIndex.BUTTON_VALUE.index];
        List<SelectedParameter> currentlySelectedParameters = List.of(); //legacy forgets the state
        String lockedForUserName = getUserNameFromMessage(messageContent);
        CustomParameterConfig config = createConfigFromCustomId(customId);
        return new State<>(buttonValue, updateState(currentlySelectedParameters, config, buttonValue, lockedForUserName, invokingUser));
    }

    private static CustomParameterStateData updateState(@Nullable List<SelectedParameter> currentlySelectedParameterList,
                                                        @NonNull CustomParameterConfig config,
                                                        @NonNull String buttonValue,
                                                        @Nullable String currentlyLockedForUser,
                                                        @NonNull String invokingUser) {
        final String shouldBeLockedForUser;
        List<SelectedParameter> currentOrNewSelectedParameter = Optional.ofNullable(currentlySelectedParameterList).orElse(config.getParamters().stream()
                .map(p -> new SelectedParameter(p.getParameterExpression(), p.getName(), null, null))
                .toList());
        Optional<String> currentParameterExpression = currentOrNewSelectedParameter.stream()
                .filter(sp -> sp.getSelectedValue() == null)
                .map(SelectedParameter::getParameterExpression)
                .findFirst();
        if (CLEAR_BUTTON_ID.equals(buttonValue) || currentParameterExpression.isEmpty()) {
            ImmutableList<SelectedParameter> newSelectedParameterList = config.getParamters().stream()
                    .map(sp -> new SelectedParameter(sp.getParameterExpression(), sp.getName(), null, null)).collect(ImmutableList.toImmutableList());
            return new CustomParameterStateData(newSelectedParameterList, null);
        } else {
            shouldBeLockedForUser = Optional.ofNullable(currentlyLockedForUser).orElse(invokingUser);
        }

        ImmutableList<SelectedParameter> newSelectedParameterList = currentOrNewSelectedParameter.stream()
                .map(sp -> {
                    if (Objects.equals(sp.getParameterExpression(), currentParameterExpression.get())) {
                        String label = getParameterForParameterExpression(config, sp.getParameterExpression())
                                .map(Parameter::getParameterOptions).orElse(List.of()).stream()
                                .filter(vl -> vl.getValue().equals(buttonValue))
                                .map(Parameter.ValueAndLabel::getLabel)
                                .filter(Objects::nonNull)
                                .findFirst().orElse(null);
                        return new SelectedParameter(sp.getParameterExpression(), sp.getName(), buttonValue, label);
                    }
                    return sp.copy();
                }).collect(ImmutableList.toImmutableList());
        return new CustomParameterStateData(newSelectedParameterList, shouldBeLockedForUser);
    }

    private static Optional<Parameter> getParameterForParameterExpression(@NonNull CustomParameterConfig config, @Nullable String parameterExpression) {
        if (parameterExpression == null) {
            return Optional.empty();
        }
        return config.getParamters().stream()
                .filter(p -> p.getParameterExpression().equals(parameterExpression))
                .findFirst();
    }

    @VisibleForTesting
    static String getFilledExpression(CustomParameterConfig config, State<CustomParameterStateData> state) {
        String filledExpression = config.getBaseExpression();
        List<SelectedParameter> selectedParameters = Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(ImmutableList.of());
        for (SelectedParameter selectedParameter : selectedParameters) {
            if (selectedParameter.getSelectedValue() != null) {
                filledExpression = filledExpression.replace(selectedParameter.getParameterExpression(), selectedParameter.getSelectedValue());
            }
        }
        return filledExpression;
    }

    @VisibleForTesting
    static Optional<String> getCurrentParameterExpression(State<CustomParameterStateData> state) {
        return Optional.ofNullable(state.getData())
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .map(SelectedParameter::getParameterExpression);
    }

    @VisibleForTesting
    static Optional<String> getCurrentParameterName(State<CustomParameterStateData> state) {
        return Optional.ofNullable(state.getData())
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .map(SelectedParameter::getParameterExpression);
    }

    private static String[] splitCustomId(String customId) {
        return customId.split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
    }


    @VisibleForTesting
    static CustomParameterConfig createConfigFromCustomId(String customId) {
        String[] split = splitCustomId(customId);
        return new CustomParameterConfig(
                getOptionalLongFromArray(split, CustomIdIndex.ANSWER_TARGET_CHANNEL.index),
                split[CustomIdIndex.BASE_EXPRESSION.index],
                DiceParserSystem.DICEROLL_PARSER, AnswerFormatType.full);
    }

    static List<Parameter> createParameterListFromBaseExpression(String expression) {
        Matcher variableMatcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        ImmutableList.Builder<Parameter> builder = ImmutableList.builder();
        while (variableMatcher.find()) {
            String parameterExpression = variableMatcher.group();
            String expressionWithoutRange = removeRange(parameterExpression);
            String name = expressionWithoutRange.substring(1, expressionWithoutRange.length() - 1);
            if (BUTTON_RANGE_PATTERN.matcher(parameterExpression).find()) {
                int min = getMinButtonFrom(parameterExpression);
                int max = getMaxButtonFrom(parameterExpression);
                builder.add(new Parameter(parameterExpression, name, IntStream.range(min, max + 1)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()));
            } else if (BUTTON_VALUE_PATTERN.matcher(parameterExpression).find()) {
                Matcher valueMatcher = BUTTON_VALUE_PATTERN.matcher(parameterExpression);
                valueMatcher.find();
                String buttonValueExpression = valueMatcher.group(1);
                builder.add(new Parameter(parameterExpression, name, Arrays.stream(buttonValueExpression.split(BUTTON_VALUE_DELIMITER))
                        .limit(23)
                        .map(s -> {
                            if (s.contains(DiceSystemAdapter.LABEL_DELIMITER)) {
                                String[] split = s.split(DiceSystemAdapter.LABEL_DELIMITER);
                                if (split.length == 2 && !Strings.isNullOrEmpty(split[0]) && !Strings.isNullOrEmpty(split[1])) {
                                    return new Parameter.ValueAndLabel(split[0], split[1]);
                                }
                            }
                            return new Parameter.ValueAndLabel(s, s);
                        })
                        .toList()));
            } else {
                builder.add(new Parameter(parameterExpression, name, IntStream.range(1, 16)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ValueAndLabel(s, s))
                        .toList()));
            }
        }

        return builder.build();
    }

    @VisibleForTesting
    static int getMinButtonFrom(String currentParameterExpression) {
        Matcher matcher = BUTTON_RANGE_PATTERN.matcher(currentParameterExpression);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    @VisibleForTesting
    static int getMaxButtonFrom(String currentParameterExpression) {
        Matcher matcher = BUTTON_RANGE_PATTERN.matcher(currentParameterExpression);
        if (matcher.find()) {
            int min = Integer.parseInt(matcher.group(1));
            int max = Integer.parseInt(matcher.group(2));
            return Math.min(Math.max(min, max), min + 23);
        }
        return 15;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Fill custom parameter of a given dice expression and roll it when all parameter are provided";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Use '/custom_parameter start' and provide a dice expression with parameter variables with the format {parameter_name}. \n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/custom_parameter expression:{numberOfDice:1<⇒10}d{sides:4/6/8/10/12/20/100}`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server", "https://discord.gg/e43BsqKpFr", false))
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
                        .build());
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(state)) {
            String expression = getFilledExpression(config, state);
            final String label;
            if (config.getAnswerFormatType() == AnswerFormatType.full) {
                label = null;
            } else {
                label = Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()).stream()
                        .map(sp -> "%s:%s".formatted(sp.getName(), sp.getLabelOfSelectedValue()))
                        .collect(Collectors.joining(", "));
            }
            return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(expression,
                    label,
                    false,
                    config.getDiceParserSystem(),
                    config.getAnswerFormatType()));
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
        return new CustomParameterConfig(answerTargetChannelId.orElse(null), baseExpression, DiceParserSystem.DICE_EVALUATOR, getAnswerTypeFromStartCommandOption(options));
    }

    @Override
    protected AnswerFormatType defaultAnswerFormat() {
        return AnswerFormatType.without_expression;
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(CustomParameterConfig config) {
        return MessageDefinition.builder()
                .content(formatMessageContent(config, null, null))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                .build();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(state)) {
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
        return messageDataDTO.map(dataDTO -> deserializeAndUpdateState(dataDTO, buttonValue, invokingUserName));
    }

    @VisibleForTesting
    ConfigAndState<CustomParameterConfig, CustomParameterStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO,
                                                                                              @NonNull String buttonValue,
                                                                                              @NonNull String invokingUser) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        Preconditions.checkArgument(STATE_DATA_TYPE_ID.equals(messageDataDTO.getStateDataClassId()) ||
                STATE_DATA_TYPE_ID_LEGACY.equals(messageDataDTO.getStateDataClassId())
                || Mapper.NO_PERSISTED_STATE.equals(messageDataDTO.getStateDataClassId()), "Unknown stateDataClassId: %s", messageDataDTO.getStateDataClassId());
        final CustomParameterStateData loadedStateData;
        if (STATE_DATA_TYPE_ID_LEGACY.equals(messageDataDTO.getStateDataClassId())) {
            loadedStateData = null;
        } else {
            loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                    .map(sd -> Mapper.deserializeObject(sd, CustomParameterStateData.class))
                    .orElse(null);
        }
        final CustomParameterConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), CustomParameterConfig.class);
        final CustomParameterStateData updatedStateData = updateState(
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getSelectedParameterValues).orElse(null),
                loadedConfig,
                buttonValue,
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getLockedForUserName).orElse(null),
                invokingUser
        );
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                loadedConfig,
                new State<>(buttonValue, updatedStateData));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull CustomParameterConfig config,
                                                                   @Nullable State<CustomParameterStateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull CustomParameterConfig config, @NonNull State<CustomParameterStateData> state) {
        if (Optional.ofNullable(state.getData()).isEmpty() || !hasMissingParameter(state)) {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, Mapper.NO_PERSISTED_STATE, null);
        } else {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData()));
        }
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(state)) {
            return Optional.empty();
        }
        String cleanName = Optional.ofNullable(state.getData())
                .map(CustomParameterStateData::getLockedForUserName)
                .orElse("");
        return Optional.of(formatMessageContent(config, state, cleanName));
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(CustomParameterConfig config, State<CustomParameterStateData> state) {
        if (!hasMissingParameter(state)) {
            return Optional.of(MessageDefinition.builder()
                    .content(formatMessageContent(config, state, null))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(config, null))
                    .build());
        }
        return Optional.empty();
    }

    private String formatMessageContent(CustomParameterConfig config, State<CustomParameterStateData> state, String userName) {
        Parameter currentParameter = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .flatMap(s -> getParameterForParameterExpression(config, s.getParameterExpression()))
                .orElse(config.getParamters().get(0));
        List<String> nameAndExpression = new ArrayList<>();
        if (!Strings.isNullOrEmpty(userName)) {
            nameAndExpression.add(userName + ": ");
        }
        if (config.getAnswerFormatType() == AnswerFormatType.full) {
            final String expression;
            if (state != null && hasMissingParameter(state)) {
                expression = getFilledExpression(config, state);
            } else {
                expression = config.getBaseExpression();
            }
            nameAndExpression.add(removeRange(expression) + "\n");
        }
        String nameExpressionAndSeparator = String.join(" ", nameAndExpression);
        return String.format("%sPlease select value for **%s**", nameExpressionAndSeparator, currentParameter.getName());
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull CustomParameterConfig config, @Nullable State<CustomParameterStateData> state) {
        String currentParameterExpression = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .map(SelectedParameter::getParameterExpression)
                .orElse(config.getParamters().get(0).getParameterExpression());
        Parameter parameter = config.getParamters().stream()
                .filter(p -> Objects.equals(p.getParameterExpression(), currentParameterExpression))
                .findFirst().orElse(config.getParamters().get(0));
        List<ButtonDefinition> buttons = parameter.getParameterOptions().stream()
                .map(vl -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), vl.getValue()))
                        .label(vl.getLabel())
                        .build())
                .collect(Collectors.toList());
        boolean hasSelectedParameter = hasAnySelectedValues(state);
        if (hasSelectedParameter) {
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

    private boolean hasAnySelectedValues(@Nullable State<CustomParameterStateData> state) {
        return Optional.ofNullable(state)
                .map(State::getData)
                .map(CustomParameterStateData::getSelectedParameterValues)
                .orElse(List.of()).stream()
                .anyMatch(sp -> sp.getSelectedValue() != null);
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
        if (createParameterListFromBaseExpression(getNextParameterExpression(config.getBaseExpression())).isEmpty()) {
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
            if (aState.getButtonIdLabelAndDiceExpressions().stream().map(ButtonIdLabelAndDiceExpression::getButtonId).count() != aState.getButtonIdLabelAndDiceExpressions().stream().map(ButtonIdLabelAndDiceExpression::getButtonId).distinct().count()) {
                return Optional.of(String.format("Parameter '%s' contains duplicate parameter option but they must be unique.", aState.getButtonIdLabelAndDiceExpressions().stream().map(ButtonIdLabelAndDiceExpression::getDiceExpression).toList()));
            }
            if (!hasMissingParameter(aState.getState())) {
                Optional<String> validationMessage = diceSystemAdapter.validateDiceExpressionWitOptionalLabel(getFilledExpression(config, aState.getState()), "/custom_parameter help", config.getDiceParserSystem());
                if (validationMessage.isPresent()) {
                    return validationMessage;
                }
            }
            if (hasMissingParameter(aState.getState()) && getParameterForParameterExpression(config, getCurrentParameterExpression(aState.getState()).orElse(null))
                    .map(Parameter::getParameterOptions)
                    .map(List::isEmpty)
                    .orElse(true)) {
                return Optional.of(String.format("The expression '%s' contains no valid parameter options", getCurrentParameterExpression(aState.getState()).orElse("")));
            }
        }
        return Optional.empty();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        String parameterExpression = getNextParameterExpression(config.getBaseExpression());

        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions = getButtons(config, parameterExpression);

        for (ButtonIdLabelAndDiceExpression buttonIdLabelAndDiceExpression : buttonIdLabelAndDiceExpressions) {
            State<CustomParameterStateData> nextState = new State<>(buttonIdLabelAndDiceExpression.getDiceExpression(), updateState(null, config, buttonIdLabelAndDiceExpression.getDiceExpression(), null, "test"));
            out.add(new StateWithCustomIdAndParameter(buttonIdLabelAndDiceExpression.getButtonId(), nextState, buttonIdLabelAndDiceExpressions));
            out.addAll(allPossibleStatePermutations(config, nextState));
        }
        return out;
    }

    List<ButtonIdLabelAndDiceExpression> getButtons(CustomParameterConfig config, String parameterExpression) {
        return getParameterForParameterExpression(config, parameterExpression)
                .map(Parameter::getParameterOptions).orElse(List.of()).stream()
                .map(vl -> new ButtonIdLabelAndDiceExpression(BottomCustomIdUtils.createButtonCustomId(getCommandId(), vl.getValue()), vl.getLabel(), vl.getValue()))
                .toList();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config, State<CustomParameterStateData> state) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        Optional<String> parameterExpression = getCurrentParameterExpression(state);

        if (parameterExpression.isPresent()) {
            List<ButtonIdLabelAndDiceExpression> parameterValues = getButtons(config, parameterExpression.get());
            for (ButtonIdLabelAndDiceExpression parameterValue : parameterValues) {
                State<CustomParameterStateData> nextState = new State<>(parameterValue.getDiceExpression(),
                        updateState(Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()), config,
                                parameterValue.getDiceExpression(), null, "test"));
                out.add(new StateWithCustomIdAndParameter(parameterValue.getButtonId(), nextState, parameterValues));
                out.addAll(allPossibleStatePermutations(config, nextState));
            }
        }
        return out;
    }

    @Value
    private static class StateWithCustomIdAndParameter {
        @NonNull
        String customId;
        @NonNull
        State<CustomParameterStateData> state;
        @NotNull
        List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions;
    }

}
