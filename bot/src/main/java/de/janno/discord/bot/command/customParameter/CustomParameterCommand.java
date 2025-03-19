package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.BotEmojiUtil;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.DropdownDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandDefinitionOptionChoice;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterConfig, CustomParameterStateData> {

    public static final String COMMAND_NAME = "custom_parameter";
    public static final String CONFIG_TYPE_ID = "CustomParameterConfig";
    static final String EXPRESSION_OPTION_NAME = "expression";
    static final String INPUT_TYPE_NAME = "input_type";
    static final String INPUT_TYPE_BUTTON = "button";
    static final String INPUT_TYPE_DROPDOWN = "dropdown";
    private static final String CLEAR_BUTTON_ID = "clear";
    private final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E", Pattern.DOTALL);
    private final static Pattern PARAMETER_EMPTY_PATTERN = Pattern.compile("\\{\\s*}", Pattern.DOTALL);
    private final static Pattern PARAMETER_NESTED_PATTERN = Pattern.compile("(\\Q{\\E(?)\\Q{\\E(?)(.*)(?)\\Q}\\E(?)\\Q}\\E)", Pattern.DOTALL);
    private static final String SELECTED_PARAMETER_DELIMITER = "\t";
    private static final String RANGE_DELIMITER = ":";
    private final static String RANGE_REPLACE_REGEX = RANGE_DELIMITER + ".+?(?=\\Q}\\E)";
    private final static Pattern BUTTON_RANGE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(-?\\d+)<=>(-?\\d+)");
    private final static Pattern PATH_ID_PATTERN = Pattern.compile("!(.*)!$");
    private final static String BUTTON_VALUE_DELIMITER = "/";
    private final static Pattern BUTTON_VALUE_PATTERN = Pattern.compile(RANGE_DELIMITER + "(.+" + BUTTON_VALUE_DELIMITER + ".+)}", Pattern.DOTALL);
    private final static Pattern PARAMETER_OPTION_EMPTY_PATTERN = Pattern.compile(RANGE_DELIMITER + ".*" + BUTTON_VALUE_DELIMITER + "\\s*" + BUTTON_VALUE_DELIMITER + ".*}", Pattern.DOTALL);
    private static final String STATE_DATA_TYPE_ID = "CustomParameterStateDataV2";
    private static final String STATE_DATA_TYPE_ID_LEGACY = "CustomParameterStateData";
    private final static Pattern LABEL_MATCHER = Pattern.compile("@[^}]+$", Pattern.DOTALL);
    private final static String SKIPPED_BY_PATH_VALUE = "";
    private final static String SKIPPED_BY_DIRECT_ROLL_VALUE = "''";
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public CustomParameterCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, cachingDiceEvaluator, UUID::randomUUID);
    }

    @VisibleForTesting
    public CustomParameterCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator, Supplier<UUID> uuidSupplier) {
        super(persistenceManager, uuidSupplier);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
    }

    static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }

    private static String removeRange(String expression) {
        return Pattern.compile(RANGE_REPLACE_REGEX, Pattern.DOTALL).matcher(expression).replaceAll("");
    }


    @VisibleForTesting
    static boolean hasMissingParameter(@NonNull State<CustomParameterStateData> state) {
        return Optional.ofNullable(state.getData())
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .isPresent();
    }

    static CustomParameterStateData updateState(@Nullable List<SelectedParameter> currentlySelectedParameterList,
                                                @NonNull CustomParameterConfig config,
                                                @NonNull String buttonValue,
                                                @Nullable String currentlyLockedForUser,
                                                @NonNull String invokingUser) {
        final String shouldBeLockedForUser;
        List<SelectedParameter> currentOrNewSelectedParameter = Optional.ofNullable(currentlySelectedParameterList).orElse(config.getParameters().stream()
                .map(p -> new SelectedParameter(p.getParameterExpression(), p.getName(), null, null, false, p.getPathId(), null))
                .toList());
        Optional<String> currentParameterExpression = currentOrNewSelectedParameter.stream()
                .filter(sp -> !sp.isFinished())
                .map(SelectedParameter::getParameterExpression)
                .findFirst();
        if (CLEAR_BUTTON_ID.equals(buttonValue) || currentParameterExpression.isEmpty()) {
            ImmutableList<SelectedParameter> newSelectedParameterList = config.getParameters().stream()
                    .map(p -> new SelectedParameter(p.getParameterExpression(), p.getName(), null, null, false, p.getPathId(), null)).collect(ImmutableList.toImmutableList());
            return new CustomParameterStateData(newSelectedParameterList, null);
        } else {
            //todo lock user with dropdown?
            shouldBeLockedForUser = Optional.ofNullable(currentlyLockedForUser).orElse(invokingUser);
        }
        final AtomicBoolean directRoll = new AtomicBoolean(false);
        final AtomicReference<String> removePathNotMatchingThis = new AtomicReference<>(null);
        ImmutableList<SelectedParameter> newSelectedParameterList = currentOrNewSelectedParameter.stream()
                .map(sp -> {
                    if (directRoll.get()) {
                        return new SelectedParameter(sp.getParameterExpression(), sp.getName(), SKIPPED_BY_DIRECT_ROLL_VALUE, null, true, sp.getPathId(), Parameter.NO_PATH);
                    }
                    if ((Objects.equals(sp.getParameterExpression(), currentParameterExpression.get()) ||
                            //dropdowns can update all parameter
                            config.getInputType() == CustomParameterConfig.InputType.dropdown) &&
                            //todo locked user and dropdown?
                            (currentlyLockedForUser == null || Objects.equals(currentlyLockedForUser, invokingUser))) {
                        final List<Parameter.ParameterOption> parameters = getParameterForParameterExpression(config, sp.getParameterExpression())
                                .map(Parameter::getParameterOptions).orElse(List.of());
                        Optional<Parameter.ParameterOption> selectedParameterOption = parameters.stream()
                                .filter(vl -> vl.id().equals(buttonValue))
                                .findFirst();
                        //fallback for legacy buttons, which use the value and not the index as button id
                        //This can be false if the old value matches an indexId, e.g. is something like id2
                        if (selectedParameterOption.isEmpty()) {
                            selectedParameterOption = parameters.stream()
                                    .filter(vl -> vl.value().equals(buttonValue))
                                    .findFirst();
                        }
                        if (selectedParameterOption.isEmpty()) {
                            //can happen if the state was already updated but the buttons are not because of a discord error
                            return sp;
                        }
                        Parameter.ParameterOption selectedParameter = selectedParameterOption.get();
                        if (selectedParameter.directRoll()) {
                            directRoll.set(true);
                        }
                        removePathNotMatchingThis.set(selectedParameter.nextPathId());
                        return new SelectedParameter(sp.getParameterExpression(), sp.getName(), selectedParameter.value(), selectedParameter.label(), true, sp.getPathId(), selectedParameter.nextPathId());
                    }
                    if (removePathNotMatchingThis.get() != null) {
                        if (!Objects.equals(sp.getPathId(), removePathNotMatchingThis.get())) {
                            //all not matching paths get finished
                            return new SelectedParameter(sp.getParameterExpression(), sp.getName(), SKIPPED_BY_PATH_VALUE, null, true, sp.getPathId(), Parameter.NO_PATH);
                        } else {
                            //after the first matching path no filter are applied
                            removePathNotMatchingThis.set(null);
                            return sp.copy();
                        }
                    }
                    return sp.copy();
                }).collect(ImmutableList.toImmutableList());
        return new CustomParameterStateData(newSelectedParameterList, shouldBeLockedForUser);
    }

    static Optional<Parameter> getParameterForParameterExpression(@NonNull CustomParameterConfig config, @Nullable String parameterExpression) {
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
            if (selectedParameter.isFinished()) {
                filledExpression = filledExpression.replace(selectedParameter.getParameterExpression(), Optional.ofNullable(selectedParameter.getSelectedValue()).orElse(""));
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

    private static String getPathId(String in) {
        Matcher idMatcher = PATH_ID_PATTERN.matcher(in);
        if (idMatcher.find()) {
            return idMatcher.group(1);
        }
        return Parameter.NO_PATH;
    }

    private static String removePathString(String in, String pathId) {
        if (Parameter.NO_PATH.equals(pathId)) {
            return in;
        }
        return in.replace("!" + pathId + "!", "");
    }

    static List<Parameter> createParameterListFromBaseExpression(String expression, CustomParameterConfig.InputType inputType) {
        Matcher variableMatcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        ImmutableList.Builder<Parameter> builder = ImmutableList.builder();
        while (variableMatcher.find()) {
            String parameterExpression = variableMatcher.group();
            String expressionWithoutRange = removeRange(parameterExpression);
            String nameAndOptionalPath = expressionWithoutRange.substring(1, expressionWithoutRange.length() - 1);
            String pathId = getPathId(nameAndOptionalPath);
            String name = removePathString(nameAndOptionalPath, pathId);
            Matcher valueMatcher = BUTTON_VALUE_PATTERN.matcher(parameterExpression);
            if (BUTTON_RANGE_PATTERN.matcher(parameterExpression).find()) {
                int min = getMinButtonFrom(parameterExpression);
                int max = getMaxButtonFrom(parameterExpression);
                AtomicInteger counter = new AtomicInteger(1);
                builder.add(new Parameter(parameterExpression, name, IntStream.range(min, max + 1)
                        .mapToObj(String::valueOf)
                        .map(s -> new Parameter.ParameterOption(s, s, createParameterOptionIdFromIndex(counter.getAndIncrement(), inputType, name), false, Parameter.NO_PATH))
                        .toList(), pathId));
            } else if (valueMatcher.find()) {
                String buttonValueExpression = valueMatcher.group(1);
                AtomicInteger counter = new AtomicInteger(1);
                builder.add(new Parameter(parameterExpression, name, Arrays.stream(buttonValueExpression.split(BUTTON_VALUE_DELIMITER))
                        .limit(23)
                        .filter(s -> !Strings.isNullOrEmpty(s))
                        .map(s -> {
                            //if a label exists
                            if (s.contains(DiceEvaluatorAdapter.LABEL_DELIMITER) && s.split(DiceEvaluatorAdapter.LABEL_DELIMITER).length == 2) {
                                String[] split = s.split(DiceEvaluatorAdapter.LABEL_DELIMITER);
                                final String parameterOptionExpressionWithPath = split[0];
                                final String nextPathId = getPathId(parameterOptionExpressionWithPath);
                                final String parameterOptionExpression = removePathString(parameterOptionExpressionWithPath, nextPathId);
                                final String label = split[1];
                                final boolean directRoll;
                                final String cleanLable;
                                if (label.startsWith("!") && Parameter.NO_PATH.equals(nextPathId) && label.length() > 1) {
                                    directRoll = true;
                                    cleanLable = label.substring(1);
                                } else {
                                    cleanLable = label;
                                    directRoll = false;
                                }
                                if (!Strings.isNullOrEmpty(parameterOptionExpression) && !Strings.isNullOrEmpty(split[1])) {
                                    return new Parameter.ParameterOption(parameterOptionExpression, cleanLable, createParameterOptionIdFromIndex(counter.getAndIncrement(), inputType, name), directRoll, nextPathId);
                                }
                            }
                            //without label
                            final String nextPathId = getPathId(s);
                            final String parameterOptionExpression = removePathString(s, nextPathId);
                            return new Parameter.ParameterOption(parameterOptionExpression, parameterOptionExpression, createParameterOptionIdFromIndex(counter.getAndIncrement(), inputType, name), false, nextPathId);

                        })
                        .toList(), pathId));
            } else {
                builder.add(new Parameter(parameterExpression, name, IntStream.range(1, 16)
                        .boxed()
                        .map(s -> new Parameter.ParameterOption(String.valueOf(s), String.valueOf(s), createParameterOptionIdFromIndex(s, inputType, name), false, Parameter.NO_PATH))
                        .toList(), Parameter.NO_PATH));
            }
        }

        return builder.build();
    }

    private static String createParameterOptionIdFromIndex(int index, CustomParameterConfig.InputType inputType, String parameterName) {
        if (inputType == CustomParameterConfig.InputType.button_legacy) {
            return "id%d".formatted(index);
        }
        return "%s-id%d".formatted(parameterName, index);
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

    public static CustomParameterConfig deserializeConfig(MessageConfigDTO messageConfigDTO) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(messageConfigDTO.getConfig(), CustomParameterConfig.class);
    }

    static String removeSuffixLabelFromExpression(@NonNull String expression, @Nullable String label) {
        String atWithLabel = "@" + label;
        if (label != null && expression.endsWith(atWithLabel)) { //only remove if the label is from the suffix
            return expression.substring(0, expression.length() - atWithLabel.length());
        }
        return expression;
    }

    static String getLabel(CustomParameterConfig config, State<CustomParameterStateData> state) {
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
                        //skipped by path have value "", skipped by direct roll have "''" as value
                        .filter(sp -> !Set.of(SKIPPED_BY_PATH_VALUE, SKIPPED_BY_DIRECT_ROLL_VALUE).contains(sp.getSelectedValue()) && sp.getSelectedValue() != null)
                        .map(sp -> "%s: %s".formatted(sp.getName(), sp.getLabelOfSelectedValue()))
                        .collect(Collectors.joining(", "));
            }
        }
        return label;
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
        return List.of(
                CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("custom_parameter.option.expression.name"))
                        .description(I18n.getMessage("custom_parameter.option.expression.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("custom_parameter.option.expression.description"))
                        .required(true)
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(INPUT_TYPE_NAME)
                        .description(I18n.getMessage("custom_parameter.option.inputType.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("custom_parameter.option.inputType.description"))
                        .required(false)
                        .choices(List.of(
                                CommandDefinitionOptionChoice.builder().name(INPUT_TYPE_BUTTON).value(INPUT_TYPE_BUTTON).build(),
                                CommandDefinitionOptionChoice.builder().name(INPUT_TYPE_DROPDOWN).value(INPUT_TYPE_DROPDOWN).build()
                        ))
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(CustomParameterConfig config, State<CustomParameterStateData> state, long channelId, long userId) {
        if (!hasMissingParameter(state)) {
            final String expression = getFilledExpression(config, state);
            final String label = getLabel(config, state);
            final String expressionWithoutSuffixLabel = removeSuffixLabelFromExpression(expression, label);
            final String expressionWithoutSuffixLabelAndAlias = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, expressionWithoutSuffixLabel);
            return Optional.of(diceEvaluatorAdapter.answerRollWithGivenLabel(expressionWithoutSuffixLabelAndAlias,
                    label,
                    false,
                    config.getAnswerFormatType(),
                    config.getDiceStyleAndColor(),
                    config.getConfigLocale()));
        }
        return Optional.empty();
    }

    @Override
    protected @NonNull CustomParameterConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION_NAME).orElse("").trim().replace("\\n", "\n");
        Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(AnswerFormatType.without_expression);
        final String name = BaseCommandOptions.getNameFromStartCommandOption(options).orElse(null);
        final CustomParameterConfig.InputType inputType = options.getStringSubOptionWithName(INPUT_TYPE_NAME)
                .map(CustomParameterConfig.InputType::fromString)
                .orElse(CustomParameterConfig.InputType.button);
        return new CustomParameterConfig(answerTargetChannelId,
                baseExpression,
                answerType,
                BaseCommandOptions.getAnswerInteractionFromStartCommandOption(options),
                null,
                new DiceStyleAndColor(
                        BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                        BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor())),
                userLocale,
                null,
                name,
                inputType
        );
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configUUID, @NonNull CustomParameterConfig config, long channelId) {
        return EmbedOrMessageDefinition.builder()
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .descriptionOrContent(formatMessageContent(config, null, null))
                .componentRowDefinitions(getComponentLayoutWithOptionalState(configUUID, config, null))
                .build();
    }


    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, CustomParameterConfig config, State<CustomParameterStateData> state, long channelId, long userId, boolean keepExistingButtonMessage) {
        if (!hasMissingParameter(state)) {
            if (keepExistingButtonMessage) {
                //reset on roll and keep message
                return Optional.of(getComponentLayoutWithOptionalState(configUUID, config, null));
            }
            return Optional.empty();
        }
        if (config.getInputType() == CustomParameterConfig.InputType.dropdown) {
            return Optional.empty();
        }
        return Optional.of(getComponentLayoutWithOptionalState(configUUID, config, state));
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

        final CustomParameterConfig loadedConfig = deserializeConfig(messageConfigDTO);
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
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, long userId, @NonNull CustomParameterConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config), config.getName(), userId));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId, long messageId, @NonNull CustomParameterConfig config, @NonNull State<CustomParameterStateData> state) {
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
    public @NonNull Optional<String> getCurrentMessageContentChange(CustomParameterConfig config, State<CustomParameterStateData> state, boolean keepExistingButtonMessage) {
        if (!hasMissingParameter(state)) {
            if (keepExistingButtonMessage) {
                //reset message after roll and keep massage
                return Optional.of(formatMessageContent(config, null, null));
            }
            return Optional.empty();
        }
        if (config.getInputType() == CustomParameterConfig.InputType.dropdown) {
            return Optional.empty();
        }
        String cleanName = Optional.ofNullable(state.getData())
                .map(CustomParameterStateData::getLockedForUserName)
                .orElse("");
        return Optional.of(formatMessageContent(config, state, cleanName));
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID,
                                                                                          @NonNull CustomParameterConfig config,
                                                                                          @Nullable State<CustomParameterStateData> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId,
                                                                                          long userId) {
        if (state == null) {
            return Optional.of(createSlashResponseMessage(configUUID, config, channelId));
        }

        if (!hasMissingParameter(state)) {
            return Optional.of(EmbedOrMessageDefinition.builder()
                    .type(EmbedOrMessageDefinition.Type.MESSAGE)
                    .descriptionOrContent(formatMessageContent(config, state, null))
                    .componentRowDefinitions(getComponentLayoutWithOptionalState(configUUID, config, null))
                    .build());
        }
        return Optional.empty();
    }

    private String formatMessageContent(CustomParameterConfig config, State<CustomParameterStateData> state, String userName) {
        if (config.getInputType() == CustomParameterConfig.InputType.dropdown) {
            return I18n.getMessage("custom_parameter.select.dropdown", config.getConfigLocale());
        }
        Parameter currentParameter = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .flatMap(s -> getParameterForParameterExpression(config, s.getParameterExpression()))
                .orElse(config.getParameters().getFirst());
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

    private List<ComponentRowDefinition> getComponentLayoutWithOptionalState(@NonNull UUID configUUID,
                                                                             @NonNull CustomParameterConfig config,
                                                                             @Nullable State<CustomParameterStateData> state) {
        if (config.getInputType() == CustomParameterConfig.InputType.dropdown) {
            return getDropdownLayout(configUUID, config);
        } else if (config.getInputType() == CustomParameterConfig.InputType.button || config.getInputType() == CustomParameterConfig.InputType.button_legacy) {
            return getButtonLayout(configUUID, config, state);
        }
        throw new IllegalArgumentException("Unsupported input type: " + config.getInputType());
    }

    private List<ComponentRowDefinition> getButtonLayout(@NonNull UUID configUUID,
                                                         @NonNull CustomParameterConfig config,
                                                         @Nullable State<CustomParameterStateData> state) {
        String currentParameterExpression = Optional.ofNullable(state)
                .map(State::getData)
                .flatMap(CustomParameterStateData::getNextUnselectedParameterExpression)
                .map(SelectedParameter::getParameterExpression)
                .orElse(config.getParameters().getFirst().getParameterExpression());
        Parameter parameter = config.getParameters().stream()
                .filter(p -> Objects.equals(p.getParameterExpression(), currentParameterExpression))
                .findFirst().orElse(config.getParameters().getFirst());
        final Set<String> openPaths;
        //for a new message the state is null
        if (state == null) {
            openPaths = Set.of(Parameter.NO_PATH);
        } else {
            openPaths = Optional.of(state).map(State::getData).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()).stream()
                    .filter(s -> !s.isFinished())
                    .filter(s -> !s.getParameterExpression().equals(currentParameterExpression))
                    .map(SelectedParameter::getPathId)
                    .collect(Collectors.toSet());
        }

        List<ButtonDefinition> buttons = parameter.getParameterOptions().stream()
                .map(vl -> {
                    final BotEmojiUtil.LabelAndEmoji labelAndEmoji = BotEmojiUtil.splitLabel(vl.label());
                    final ButtonDefinition.Style style = vl.directRoll() || !openPaths.contains(vl.nextPathId()) ? ButtonDefinition.Style.SUCCESS : ButtonDefinition.Style.PRIMARY;
                    return ButtonDefinition.builder()
                            .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), vl.id(), configUUID))
                            .style(style)
                            .label(labelAndEmoji.labelWithoutLeadingEmoji())
                            .emoji(labelAndEmoji.emoji())
                            .build();
                })
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
                .map(bl -> ComponentRowDefinition.builder().componentDefinitions(bl)
                        .build())
                .collect(Collectors.toList());
    }

    private List<ComponentRowDefinition> getDropdownLayout(@NonNull UUID configUUID,
                                                           @NonNull CustomParameterConfig config) {
        return config.getParameters().stream()
                .map(p -> ComponentRowDefinition.builder()
                        .componentDefinition(DropdownDefinition.builder()
                                .id(p.getName())
                                .options(p.getParameterOptions().stream()
                                        .map(b -> {
                                            final BotEmojiUtil.LabelAndEmoji labelAndEmoji = BotEmojiUtil.splitLabel(b.label());
                                            return DropdownDefinition.DropdownOption.builder()
                                                    .value(BottomCustomIdUtils.createButtonCustomId(getCommandId(), b.id(), configUUID))
                                                    .label(labelAndEmoji.labelWithoutLeadingEmoji())
                                                    .emoji(labelAndEmoji.emoji())
                                                    .build();
                                        })
                                        .toList()
                                )
                                .maxValues(1)
                                .placeholder(p.getName())
                                .build())
                        .build())
                .collect(Collectors.toList());
    }

    private boolean hasAnySelectedValues(@Nullable State<CustomParameterStateData> state) {
        return Optional.ofNullable(state)
                .map(State::getData)
                .map(CustomParameterStateData::getSelectedParameterValues)
                .orElse(List.of()).stream()
                .anyMatch(SelectedParameter::isFinished);
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options,
                                                                         long channelId, long userId, @NonNull Locale userLocale) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION_NAME).orElse("");
        log.trace("Start validating: {}", baseExpression.replace("\n", " "));
        int variableCount = 0;
        //todo validation dropdown -> no path, no variable with same name
        Matcher variableMatcher = PARAMETER_VARIABLE_PATTERN.matcher(baseExpression);
        while (variableMatcher.find()) {
            variableCount++;
        }
        if (variableCount == 0) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.variable.count.zero", userLocale));
        }
        if (PARAMETER_NESTED_PATTERN.matcher(baseExpression).find()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.nested.brackets", userLocale));
        }
        if (StringUtils.countMatches(baseExpression, "{") != StringUtils.countMatches(baseExpression, "}")) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.unclosed.bracket", userLocale));
        }
        if (PARAMETER_EMPTY_PATTERN.matcher(baseExpression).find()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.empty.brackets", userLocale));
        }
        if (PARAMETER_OPTION_EMPTY_PATTERN.matcher(baseExpression).find()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.empty.option", userLocale));
        }
        if (baseExpression.contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER)) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.character", userLocale, BottomCustomIdUtils.CUSTOM_ID_DELIMITER));
        }
        if (baseExpression.contains(SELECTED_PARAMETER_DELIMITER)) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.character", userLocale, SELECTED_PARAMETER_DELIMITER));
        }
        CustomParameterConfig config = getConfigFromStartOptions(options, userLocale);
        String nextParameterExpression = getNextParameterExpression(config.getBaseExpression());
        if (createParameterListFromBaseExpression(nextParameterExpression, config.getInputType()).isEmpty()) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.invalid.parameter.option", userLocale, nextParameterExpression));
        }
        return CustomParameterValidator.validateStates(config, channelId, userId, persistenceManager, diceEvaluatorAdapter);
    }
}
