package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterConfig, CustomParameterStateData> {

    //todo button label, pagination for buttons

    static final String EXPRESSION_OPTION_NAME = "expression";

    private static final String COMMAND_NAME = "custom_parameter";
    private static final String CLEAR_BUTTON_ID = "clear";
    private final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    private static final String SELECTED_PARAMETER_DELIMITER = "\t";
    private static final String RANGE_DELIMITER = ":";
    private final static String RANGE_REPLACE_REGEX = RANGE_DELIMITER + ".+?(?=\\Q}\\E)";
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}");
    private static final String STATE_DATA_TYPE_ID = "CustomParameterStateDataV2";
    private static final String STATE_DATA_TYPE_ID_LEGACY = "CustomParameterStateData";
    private static final String CONFIG_TYPE_ID = "CustomParameterConfig";
    private final static Pattern LABEL_MATCHER = Pattern.compile("@[^}]+$");
    private final DiceSystemAdapter diceSystemAdapter;

    public CustomParameterCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, new DiceParser(), cachingDiceEvaluator);
    }

    @VisibleForTesting
    public CustomParameterCommand(PersistenceManager persistenceManager, Dice dice, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceSystemAdapter = new DiceSystemAdapter(cachingDiceEvaluator, dice);
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


    @VisibleForTesting
    static boolean hasMissingParameter(@NonNull State<CustomParameterStateData> state) {
        return Optional.ofNullable(state.getData())
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .isPresent();
    }

    private static CustomParameterStateData updateState(@Nullable List<SelectedParameter> currentlySelectedParameterList,
                                                        @NonNull CustomParameterConfig config,
                                                        @NonNull String buttonValue,
                                                        @Nullable String currentlyLockedForUser,
                                                        @NonNull String invokingUser) {
        final String shouldBeLockedForUser;
        List<SelectedParameter> currentOrNewSelectedParameter = Optional.ofNullable(currentlySelectedParameterList).orElse(config.getParameters().stream()
                .map(p -> new SelectedParameter(p.getParameterExpression(), p.getName(), null, null))
                .toList());
        Optional<String> currentParameterExpression = currentOrNewSelectedParameter.stream()
                .filter(sp -> sp.getSelectedValue() == null)
                .map(SelectedParameter::getParameterExpression)
                .findFirst();
        if (CLEAR_BUTTON_ID.equals(buttonValue) || currentParameterExpression.isEmpty()) {
            ImmutableList<SelectedParameter> newSelectedParameterList = config.getParameters().stream()
                    .map(sp -> new SelectedParameter(sp.getParameterExpression(), sp.getName(), null, null)).collect(ImmutableList.toImmutableList());
            return new CustomParameterStateData(newSelectedParameterList, null);
        } else {
            shouldBeLockedForUser = Optional.ofNullable(currentlyLockedForUser).orElse(invokingUser);
        }

        ImmutableList<SelectedParameter> newSelectedParameterList = currentOrNewSelectedParameter.stream()
                .map(sp -> {
                    if (Objects.equals(sp.getParameterExpression(), currentParameterExpression.get()) &&
                            (currentlyLockedForUser == null || Objects.equals(currentlyLockedForUser, invokingUser))) {
                        List<Parameter.ParameterOption> parameters = getParameterForParameterExpression(config, sp.getParameterExpression())
                                .map(Parameter::getParameterOptions).orElse(List.of());
                        Optional<Parameter.ParameterOption> selectedParameterOption = parameters.stream()
                                .filter(vl -> vl.getId().equals(buttonValue))
                                .findFirst();
                        //fallback for legacy buttons, which use the value and not the index as button id
                        //This can be false if the old value matches an indexId, e.g. is something like id2
                        if (selectedParameterOption.isEmpty()) {
                            selectedParameterOption = parameters.stream()
                                    .filter(vl -> vl.getValue().equals(buttonValue))
                                    .findFirst();
                        }
                        Parameter.ParameterOption selectedParameter = selectedParameterOption.orElseThrow(() -> new RuntimeException("Found no parameter in for value %s in %s".formatted(buttonValue, parameters)));
                        return new SelectedParameter(sp.getParameterExpression(), sp.getName(), selectedParameter.getValue(), selectedParameter.getLabel());
                    }
                    return sp.copy();
                }).collect(ImmutableList.toImmutableList());
        return new CustomParameterStateData(newSelectedParameterList, shouldBeLockedForUser);
    }

    private static Optional<Parameter> getParameterForParameterExpression(@NonNull CustomParameterConfig config, @Nullable String parameterExpression) {
        if (parameterExpression == null) {
            return Optional.empty();
        }
        return config.getParameters().stream()
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

    static List<Parameter> createParameterListFromBaseExpression(String expression) {
        Matcher variableMatcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        ImmutableList.Builder<Parameter> builder = ImmutableList.builder();
        while (variableMatcher.find()) {
            String parameterExpression = variableMatcher.group();
            String expressionWithoutRange = removeRange(parameterExpression);
            String name = expressionWithoutRange.substring(1, expressionWithoutRange.length() - 1);
            Matcher valueMatcher = BUTTON_VALUE_PATTERN.matcher(parameterExpression);
            if (BUTTON_RANGE_PATTERN.matcher(parameterExpression).find()) {
                int min = getMinButtonFrom(parameterExpression);
                int max = getMaxButtonFrom(parameterExpression);
                AtomicInteger counter = new AtomicInteger(1);
                builder.add(new Parameter(parameterExpression, name, IntStream.range(min, max + 1)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, createParameterOptionIdFromIndex(counter.getAndIncrement())))
                        .toList()));
            } else if (valueMatcher.find()) {
                String buttonValueExpression = valueMatcher.group(1);
                AtomicInteger counter = new AtomicInteger(1);
                builder.add(new Parameter(parameterExpression, name, Arrays.stream(buttonValueExpression.split(BUTTON_VALUE_DELIMITER))
                        .limit(23)
                        .map(s -> {
                            if (s.contains(DiceSystemAdapter.LABEL_DELIMITER)) {
                                String[] split = s.split(DiceSystemAdapter.LABEL_DELIMITER);
                                if (split.length == 2 && !Strings.isNullOrEmpty(split[0]) && !Strings.isNullOrEmpty(split[1])) {
                                    return new Parameter.ParameterOption(split[0], split[1], createParameterOptionIdFromIndex(counter.getAndIncrement()));
                                }
                            }
                            return new Parameter.ParameterOption(s, s, createParameterOptionIdFromIndex(counter.getAndIncrement()));
                        })
                        .toList()));
            } else {
                builder.add(new Parameter(parameterExpression, name, IntStream.range(1, 16)
                        .boxed()
                        .map(s -> new Parameter.ParameterOption(String.valueOf(s), String.valueOf(s), createParameterOptionIdFromIndex(s)))
                        .toList()));
            }
        }

        return builder.build();
    }

    private static String createParameterOptionIdFromIndex(int index) {
        return "id%d".formatted(index);
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
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("custom_parameter.help.message", userLocale) + " \n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("custom_parameter.help.example.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.documentation.field.name", userLocale), I18n.getMessage("help.documentation.field.value", userLocale), false))
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.discord.server.field.name", userLocale), I18n.getMessage("help.discord.server.field.value", userLocale), false))
                .build();
    }

    @Override
    public @NonNull String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessages("custom_parameter.option.expression.name"))
                        .description(I18n.getMessage("custom_parameter.option.expression.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessages("custom_parameter.option.expression.description"))
                        .required(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build());
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CustomParameterConfig config, State<CustomParameterStateData> state, long channelId, long userId) {
        if (!hasMissingParameter(state)) {
            String expression = getFilledExpression(config, state);
            String label = getLabel(config, state);
            String expressionWithoutSuffixLabel = removeSuffixLabelFromExpression(expression, label);
            return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(expressionWithoutSuffixLabel,
                    label,
                    false,
                    config.getDiceParserSystem(),
                    config.getAnswerFormatType(),
                    config.getDiceStyleAndColor(),
                    config.getConfigLocale()));
        }
        return Optional.empty();
    }

    private String removeSuffixLabelFromExpression(@NonNull String expression, @Nullable String label) {
        String atWithLabel = "@" + label;
        if (label != null && expression.endsWith(atWithLabel)) { //only remove if the label is from the suffix
            return expression.substring(0, expression.length() - atWithLabel.length());
        }
        return expression;
    }

    private String getLabel(CustomParameterConfig config, State<CustomParameterStateData> state) {
        final String label;
        Matcher labelMatcher = LABEL_MATCHER.matcher(removeRange(config.getBaseExpression()));

        if (labelMatcher.find()) {
            String match = labelMatcher.group();
            label = match.substring(1); //remove @
        } else {
            if (config.getAnswerFormatType() == AnswerFormatType.full) {
                label = null;
            } else {
                label = Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()).stream()
                        .map(sp -> "%s: %s".formatted(sp.getName(), sp.getLabelOfSelectedValue()))
                        .collect(Collectors.joining(", "));
            }
        }
        return label;
    }

    @Override
    protected @NonNull CustomParameterConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION_NAME).orElse("").trim();
        Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        return new CustomParameterConfig(answerTargetChannelId,
                baseExpression,
                DiceParserSystem.DICE_EVALUATOR,
                answerType,
                null,
                new DiceStyleAndColor(
                        BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                        BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor())),
                userLocale
        );
    }

    @Override
    protected AnswerFormatType defaultAnswerFormat() {
        return AnswerFormatType.without_expression;
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createNewButtonMessage(@NonNull UUID configUUID, @NonNull CustomParameterConfig config) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(formatMessageContent(config, null, null))
                .componentRowDefinitions(getButtonLayoutWithOptionalState(configUUID, config, null))
                .build();
    }


    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, CustomParameterConfig config, State<CustomParameterStateData> state, long channelId, long userId) {
        if (!hasMissingParameter(state)) {
            return Optional.empty();
        }
        return Optional.of(getButtonLayoutWithOptionalState(configUUID, config, state));
    }

    @Override
    protected ConfigAndState<CustomParameterConfig, CustomParameterStateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                                     @NonNull MessageDataDTO messageDataDTO,
                                                                                                                     @NonNull String buttonValue,
                                                                                                                     @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, messageDataDTO, buttonValue, invokingUserName);
    }

    @VisibleForTesting
    ConfigAndState<CustomParameterConfig, CustomParameterStateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                              @NonNull MessageDataDTO messageDataDTO,
                                                                                              @NonNull String buttonValue,
                                                                                              @NonNull String invokingUser) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID_LEGACY, STATE_DATA_TYPE_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final CustomParameterStateData loadedStateData = Optional.of(messageDataDTO)
                .filter(m -> STATE_DATA_TYPE_ID.equals(m.getStateDataClassId()))
                .map(MessageDataDTO::getStateData)
                .map(sd -> Mapper.deserializeObject(sd, CustomParameterStateData.class))
                .orElse(null);

        final CustomParameterConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), CustomParameterConfig.class);
        final CustomParameterStateData updatedStateData = updateState(
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getSelectedParameterValues).orElse(null),
                loadedConfig,
                buttonValue,
                Optional.ofNullable(loadedStateData).map(CustomParameterStateData::getLockedForUserName).orElse(null),
                invokingUser
        );
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(),
                loadedConfig,
                new State<>(buttonValue, updatedStateData));
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull CustomParameterConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull CustomParameterConfig config, @NonNull State<CustomParameterStateData> state) {
        if (!hasMissingParameter(state)) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            //message data so we knew the button message exists
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null));
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
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
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID, @NonNull CustomParameterConfig config, @NonNull State<CustomParameterStateData> state, long guildId, long channelId) {
        if (!hasMissingParameter(state)) {
            return Optional.of(EmbedOrMessageDefinition.builder()
                    .type(EmbedOrMessageDefinition.Type.MESSAGE)
                    .descriptionOrContent(formatMessageContent(config, state, null))
                    .componentRowDefinitions(getButtonLayoutWithOptionalState(configUUID, config, null))
                    .build());
        }
        return Optional.empty();
    }

    private String formatMessageContent(CustomParameterConfig config, State<CustomParameterStateData> state, String userName) {
        Parameter currentParameter = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .flatMap(s -> getParameterForParameterExpression(config, s.getParameterExpression()))
                .orElse(config.getParameters().get(0));
        List<String> nameAndExpression = new ArrayList<>();
        if (!Strings.isNullOrEmpty(userName)) {
            nameAndExpression.add(userName + ":");
        }
        if (config.getAnswerFormatType() == AnswerFormatType.full) {
            final String expression;
            if (state != null && hasMissingParameter(state)) {
                expression = getFilledExpression(config, state);
            } else {
                expression = config.getBaseExpression();
            }
            nameAndExpression.add(removeRange(expression) + "\n");
        } else {
            nameAndExpression.add("");
        }
        String nameExpressionAndSeparator = String.join(" ", nameAndExpression);
        return I18n.getMessage("custom_parameter.select.parameter",
                config.getConfigLocale(),
                nameExpressionAndSeparator,
                currentParameter.getName());
    }

    private List<ComponentRowDefinition> getButtonLayoutWithOptionalState(@NonNull UUID configUUID, @NonNull CustomParameterConfig config, @Nullable State<CustomParameterStateData> state) {
        String currentParameterExpression = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .map(SelectedParameter::getParameterExpression)
                .orElse(config.getParameters().get(0).getParameterExpression());
        Parameter parameter = config.getParameters().stream()
                .filter(p -> Objects.equals(p.getParameterExpression(), currentParameterExpression))
                .findFirst().orElse(config.getParameters().get(0));
        List<ButtonDefinition> buttons = parameter.getParameterOptions().stream()
                .map(vl -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), vl.getId(), configUUID))
                        .label(vl.getLabel())
                        .build())
                .collect(Collectors.toList());
        boolean hasSelectedParameter = hasAnySelectedValues(state);
        if (hasSelectedParameter) {
            buttons.add(ButtonDefinition.builder()
                    .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, configUUID))
                    .label(I18n.getMessage("custom_parameter.button.label.clear", config.getConfigLocale()))
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
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION_NAME).orElse("");
        log.info("Start validating: {}", baseExpression);
        int variableCount = 0;
        Matcher variableMatcher = PARAMETER_VARIABLE_PATTERN.matcher(baseExpression);
        while (variableMatcher.find()) {
            variableCount++;
        }
        if (variableCount == 0) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.variable.count.zero", userLocale));
        }
        if (variableCount > 4) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.variable.count.max.four", userLocale));
        }
        if (Pattern.compile("(\\Q{\\E(?)\\Q{\\E(?)(.*)(?)\\Q}\\E(?)\\Q}\\E)").matcher(baseExpression).find()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.nested.brackets", userLocale));
        }
        if (StringUtils.countMatches(baseExpression, "{") != StringUtils.countMatches(baseExpression, "}")) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.unclosed.bracket", userLocale));
        }
        //todo check for brackets with whitespace
        if (baseExpression.contains("{}")) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.empty.brackets", userLocale));
        }
        if (baseExpression.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.character", userLocale, BottomCustomIdUtils.CUSTOM_ID_DELIMITER));
        }
        if (baseExpression.contains(SELECTED_PARAMETER_DELIMITER)) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.character", userLocale, SELECTED_PARAMETER_DELIMITER));
        }
        CustomParameterConfig config = getConfigFromStartOptions(options, userLocale);
        if (createParameterListFromBaseExpression(getNextParameterExpression(config.getBaseExpression())).isEmpty()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.parameter.option", userLocale, getNextParameterExpression(config.getBaseExpression())));
        }
        return validateAllPossibleStates(config);
    }

    private Optional<String> validateAllPossibleStates(CustomParameterConfig config) {

        List<StateWithCustomIdAndParameter> allPossibleStatePermutations = allPossibleStatePermutations(config);
        for (StateWithCustomIdAndParameter aState : allPossibleStatePermutations) {
            if (!hasMissingParameter(aState.getState())) {
                String expression = getFilledExpression(config, aState.getState());
                String label = getLabel(config, aState.getState());
                String expressionWithoutSuffixLabel = removeSuffixLabelFromExpression(expression, label);
                Optional<String> validationMessage = diceSystemAdapter.validateDiceExpressionWitOptionalLabel(expressionWithoutSuffixLabel,
                        "/%s %s".formatted(I18n.getMessage("custom_parameter.name", config.getConfigLocale()), I18n.getMessage("base.option.help", config.getConfigLocale())),
                        config.getDiceParserSystem(),
                        config.getConfigLocale());
                if (validationMessage.isPresent()) {
                    return validationMessage;
                }
            }
            if (hasMissingParameter(aState.getState()) && getParameterForParameterExpression(config, getCurrentParameterExpression(aState.getState()).orElse(null))
                    .map(Parameter::getParameterOptions)
                    .map(List::isEmpty)
                    .orElse(true)) {
                return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.parameter.option", config.getConfigLocale(), getCurrentParameterExpression(aState.getState()).orElse("")));
            }
        }
        return Optional.empty();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        String parameterExpression = getNextParameterExpression(config.getBaseExpression());

        List<ButtonLabelAndValue> buttonLabelAndValues = getButtons(config, parameterExpression);

        for (ButtonLabelAndValue buttonLabelAndValue : buttonLabelAndValues) {
            State<CustomParameterStateData> nextState = new State<>(buttonLabelAndValue.getValue(), updateState(null, config, buttonLabelAndValue.getValue(), null, "test"));
            out.add(new StateWithCustomIdAndParameter(buttonLabelAndValue.getValue(), nextState, buttonLabelAndValues));
            out.addAll(allPossibleStatePermutations(config, nextState));
        }
        return out;
    }

    List<ButtonLabelAndValue> getButtons(CustomParameterConfig config, String parameterExpression) {
        return getParameterForParameterExpression(config, parameterExpression)
                .map(Parameter::getParameterOptions).orElse(List.of()).stream()
                .map(vl -> new ButtonLabelAndValue(vl.getLabel(), vl.getId()))
                .toList();
    }

    private List<StateWithCustomIdAndParameter> allPossibleStatePermutations(CustomParameterConfig config, State<CustomParameterStateData> state) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        Optional<String> parameterExpression = getCurrentParameterExpression(state);

        if (parameterExpression.isPresent()) {
            List<ButtonLabelAndValue> parameterValues = getButtons(config, parameterExpression.get());
            for (ButtonLabelAndValue parameterValue : parameterValues) {
                State<CustomParameterStateData> nextState = new State<>(parameterValue.getValue(),
                        updateState(Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()), config,
                                parameterValue.getValue(), null, "test"));
                out.add(new StateWithCustomIdAndParameter(parameterValue.getValue(), nextState, parameterValues));
                out.addAll(allPossibleStatePermutations(config, nextState));
            }
        }
        return out;
    }

    @Value
    static class ButtonLabelAndValue {
        String label;
        String value;
    }

    @Value
    private static class StateWithCustomIdAndParameter {
        @NonNull
        String customId;
        @NonNull
        State<CustomParameterStateData> state;
        @NotNull
        List<ButtonLabelAndValue> buttonIdLabelAndDiceExpressions;
    }

}
