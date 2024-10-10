package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
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
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Slf4j
public class SumCustomSetCommand extends AbstractCommand<SumCustomSetConfig, SumCustomSetStateDataV2> {
    public static final String COMMAND_NAME = "sum_custom_set";
    static final String BUTTONS_COMMAND_OPTIONS_NAME = "buttons";
    static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME = "always_sum_result";
    static final String HIDE_EXPRESSION_IN_ANSWER_OPTIONS_NAME = "hide_expression_in_answer";
    static final String PREFIX_OPTIONS_NAME = "prefix";
    static final String POSTFIX_OPTIONS_NAME = "postfix";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String NO_ACTION = "no action";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String BACK_BUTTON_ID = "back";

    private static final String CONFIG_TYPE_ID = "SumCustomSetConfig";
    private static final String STATE_DATA_TYPE_ID = "SumCustomSetStateDataV2";
    private static final String STATE_DATA_TYPE_LEGACY_ID = "SumCustomSetStateData";
    private final static Pattern ENDS_WITH_DOUBLE_SEMICOLUMN_PATTERN = Pattern.compile(".*;\\s*;\\s*$", Pattern.DOTALL);
    private final DiceEvaluatorAdapter diceEvaluatorAdapter;

    public SumCustomSetCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, cachingDiceEvaluator, UUID::randomUUID);
    }

    @VisibleForTesting
    public SumCustomSetCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator, Supplier<UUID> uuidSupplier) {
        super(persistenceManager, uuidSupplier);
        this.diceEvaluatorAdapter = new DiceEvaluatorAdapter(cachingDiceEvaluator);
    }

    public static SumCustomSetConfig deserializeConfig(MessageConfigDTO messageConfigDTO) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        return Mapper.deserializeObject(messageConfigDTO.getConfig(), SumCustomSetConfig.class);
    }

    @Override
    protected ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                                 @NonNull MessageDataDTO messageDataDTO,
                                                                                                                 @NonNull String buttonValue,
                                                                                                                 @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, messageDataDTO, buttonValue, invokingUserName);
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, @Nullable Long guildId, long channelId, long messageId, @NonNull SumCustomSetConfig config, @NonNull State<SumCustomSetStateDataV2> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            //message data so we knew the button message exists
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), Mapper.NO_PERSISTED_STATE, null));
        } else if (state.getData() != null) {
            persistenceManager.deleteStateForMessage(channelId, messageId);
            persistenceManager.saveMessageData(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData())));
        }
    }

    @VisibleForTesting
    ConfigAndState<SumCustomSetConfig, SumCustomSetStateDataV2> deserializeAndUpdateState(
            @NonNull MessageConfigDTO messageConfigDTO,
            @NonNull MessageDataDTO messageDataDTO,
            @NonNull String buttonValue,
            @NonNull String invokingUserName) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, STATE_DATA_TYPE_LEGACY_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final SumCustomSetStateDataV2 loadedStateData;
        if (messageDataDTO.getStateDataClassId().equals(STATE_DATA_TYPE_ID)) {
            loadedStateData = Optional.of(messageDataDTO)
                    .map(MessageDataDTO::getStateData)
                    .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateDataV2.class))
                    .orElse(null);
        } else if (messageDataDTO.getStateDataClassId().equals(STATE_DATA_TYPE_LEGACY_ID)) {
            loadedStateData = Optional.of(messageDataDTO)
                    .map(MessageDataDTO::getStateData)
                    .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateData.class))
                    .map(l -> new SumCustomSetStateDataV2(l.getDiceExpressions().stream()
                            .map(e -> new ExpressionAndLabel(e, e))
                            .collect(Collectors.toList()),
                            l.getLockedForUserName()))
                    .orElse(null);
        } else {
            loadedStateData = null;
        }


        final SumCustomSetConfig loadedConfig = deserializeConfig(messageConfigDTO);
        final State<SumCustomSetStateDataV2> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateDataV2::getDiceExpressions).orElse(ImmutableList.of()),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateDataV2::getLockedForUserName).orElse(""),
                loadedConfig.getLabelAndExpression());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, @Nullable Long guildId, long channelId, @NonNull SumCustomSetConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage(Locale userLocale) {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("sum_custom_set.help.message", userLocale) + "\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field(I18n.getMessage("help.example.field.name", userLocale), I18n.getMessage("sum_custom_set.help.example.field.value", userLocale), false))
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
        return List.of(CommandDefinitionOption.builder()
                        .name(BUTTONS_COMMAND_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_custom_set.option.buttons.name"))
                        .description(I18n.getMessage("sum_custom_set.option.buttons.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_custom_set.option.buttons.description"))
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(true)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_custom_set.option.alwaysSum.name"))
                        .description(I18n.getMessage("sum_custom_set.option.alwaysSum.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_custom_set.option.alwaysSum.description"))
                        .type(CommandDefinitionOption.Type.BOOLEAN)
                        .required(false)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(HIDE_EXPRESSION_IN_ANSWER_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_custom_set.option.hideExpression.name"))
                        .description(I18n.getMessage("sum_custom_set.option.hiddeExpression.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_custom_set.option.hiddeExpression.description"))
                        .type(CommandDefinitionOption.Type.BOOLEAN)
                        .required(false)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(PREFIX_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_custom_set.option.prefix.name"))
                        .description(I18n.getMessage("sum_custom_set.option.prefix.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_custom_set.option.prefix.description"))
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(false)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(POSTFIX_OPTIONS_NAME)
                        .nameLocales(I18n.allNoneEnglishMessagesNames("sum_custom_set.option.postfix.name"))
                        .description(I18n.getMessage("sum_custom_set.option.postfix.description", Locale.ENGLISH))
                        .descriptionLocales(I18n.allNoneEnglishMessagesDescriptions("sum_custom_set.option.postfix.description"))
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(false)
                        .build()
        );
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateDataV2> state, long channelId, long userId) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumCustomSetStateDataV2::getDiceExpressions)
                        .map(List::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        String label = combineLabel(state.getData().getDiceExpressions(), config);
        String newExpression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions(), config.getPrefix(), config.getPostfix()));

        return Optional.of(diceEvaluatorAdapter.answerRollWithGivenLabel(newExpression,
                label,
                config.isAlwaysSumResult(),
                config.getAnswerFormatType(),
                config.getDiceStyleAndColor(),
                config.getConfigLocale()));
    }

    @Override
    public @NonNull EmbedOrMessageDefinition createSlashResponseMessage(@NonNull UUID configUUID, @NonNull SumCustomSetConfig config, long channelId) {
        Set<String> disabledIds = getDisabledButtonIds(config, null, channelId, null);

        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()))
                .type(EmbedOrMessageDefinition.Type.MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID, config, true, true, disabledIds, config.getConfigLocale()))
                .build();
    }

    @Override
    protected @NonNull Optional<EmbedOrMessageDefinition> createNewButtonMessageWithState(@NonNull UUID configUUID,
                                                                                          @NonNull SumCustomSetConfig config,
                                                                                          @Nullable State<SumCustomSetStateDataV2> state,
                                                                                          @Nullable Long guildId,
                                                                                          long channelId) {
        if (state == null) {
            return Optional.of(createSlashResponseMessage(configUUID, config, channelId));
        }
        if (ROLL_BUTTON_ID.equals(state.getButtonValue()) && !Optional.ofNullable(state.getData())
                .map(SumCustomSetStateDataV2::getDiceExpressions)
                .map(List::isEmpty)
                .orElse(false)) {
            Set<String> disabledIds = getDisabledButtonIds(config, null, channelId, null);
            return Optional.of(EmbedOrMessageDefinition.builder()
                    .descriptionOrContent(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()))
                    .type(EmbedOrMessageDefinition.Type.MESSAGE)
                    .componentRowDefinitions(createButtonLayout(configUUID, config, true, true, disabledIds, config.getConfigLocale()))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID configUUID, SumCustomSetConfig config, State<SumCustomSetStateDataV2> state, long channelId, long userId, boolean keepExistingButtonMessage) {
        if (state.getData() == null || ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            if (keepExistingButtonMessage) {
                return Optional.of(createButtonLayout(configUUID, config, true, true, getDisabledButtonIds(config, null, channelId, null), config.getConfigLocale()));
            }
            return Optional.empty();
        }
        String expression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions(), config.getPrefix(), config.getPostfix()));
        boolean expressionIsEmpty = Optional.of(state.getData())
                .map(SumCustomSetStateDataV2::getDiceExpressions)
                .map(List::isEmpty)
                .orElse(true);
        Set<String> disabledIds = getDisabledButtonIds(config, state, channelId, userId);
        //todo only update if the disabled button where changed, need current button state
        return Optional.of(createButtonLayout(configUUID, config, !diceEvaluatorAdapter.isValidExpression(expression), expressionIsEmpty, disabledIds, config.getConfigLocale()));
    }

    private Set<String> getDisabledButtonIds(@NonNull SumCustomSetConfig config, @Nullable State<SumCustomSetStateDataV2> state, long channelId, @Nullable Long userId) {
        return config.getLabelAndExpression().stream()
                .filter(ButtonIdLabelAndDiceExpression::isDirectRoll)
                .filter(b -> {
                    final State<SumCustomSetStateDataV2> updatedState = updateStateWithButtonValue(b.getButtonId(),
                            Optional.ofNullable(state).map(State::getData).map(SumCustomSetStateDataV2::getDiceExpressions).orElse(List.of()),
                            "",
                            "",
                            config.getLabelAndExpression());
                    List<ExpressionAndLabel> diceExpression = Optional.of(updatedState).map(State::getData).map(SumCustomSetStateDataV2::getDiceExpressions).orElse(List.of());
                    String expressionAfterSelection = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(diceExpression, config.getPrefix(), config.getPostfix()));
                    return !diceEvaluatorAdapter.isValidExpression(expressionAfterSelection);
                })
                .map(ButtonIdLabelAndDiceExpression::getButtonId)
                .collect(Collectors.toSet());
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(SumCustomSetConfig config, State<SumCustomSetStateDataV2> state, boolean keepExistingButtonMessage) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            if (keepExistingButtonMessage) {
                return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
            }
            return Optional.empty();
        } else if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            //todo handle better with starterId -> new message not edit?
            return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
        } else {
            if (Optional.ofNullable(state.getData())
                    .map(SumCustomSetStateDataV2::getDiceExpressions)
                    .map(List::isEmpty)
                    .orElse(false)) {
                return Optional.of(I18n.getMessage("sum_custom_set.buttonMessage.empty", config.getConfigLocale()));
            }
            if (Optional.ofNullable(state.getData()).map(SumCustomSetStateDataV2::getLockedForUserName).isEmpty()) {
                return Optional.ofNullable(state.getData()).map(SumCustomSetStateDataV2::getDiceExpressions).map(e -> combineLabel(e, config));
            } else {
                String cleanName = state.getData().getLockedForUserName();
                return Optional.of(String.format("%s: %s", cleanName, combineLabel(state.getData().getDiceExpressions(), config)));
            }
        }
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options, long channelId, long userId, @NonNull Locale userLocale) {
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_NAME).orElseThrow();
        return ButtonHelper.valdiate(buttons, userLocale, List.of("clear", "back", "roll"), ENDS_WITH_DOUBLE_SEMICOLUMN_PATTERN.matcher(buttons).matches());
    }

    private State<SumCustomSetStateDataV2> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                      @NonNull final List<ExpressionAndLabel> currentExpressions,
                                                                      @NonNull final String invokingUserName,
                                                                      @Nullable final String lockedToUser,
                                                                      @NonNull final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateDataV2(ImmutableList.of(), null));
        }
        if (!Strings.isNullOrEmpty(lockedToUser) && !lockedToUser.equals(invokingUserName)) {
            return new State<>(NO_ACTION, new SumCustomSetStateDataV2(currentExpressions, lockedToUser));
        }
        if (BACK_BUTTON_ID.equals(buttonValue)) {
            final List<ExpressionAndLabel> newExpressionList;
            if (currentExpressions.isEmpty()) {
                newExpressionList = ImmutableList.of();
            } else {
                newExpressionList = ImmutableList.copyOf(currentExpressions.subList(0, currentExpressions.size() - 1));
            }
            return new State<>(buttonValue, new SumCustomSetStateDataV2(newExpressionList, newExpressionList.isEmpty() ? null : lockedToUser));
        }
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateDataV2(currentExpressions, lockedToUser));
        }
        final Optional<ButtonIdLabelAndDiceExpression> selectedButton = buttonIdLabelAndDiceExpressions.stream()
                .filter(bld -> bld.getButtonId().equals(buttonValue))
                .findFirst();

        final Optional<ExpressionAndLabel> addExpression = selectedButton
                .map(bld -> new ExpressionAndLabel(bld.getDiceExpression(), bld.getLabel()));
        if (addExpression.isEmpty()) {
            return new State<>(NO_ACTION, new SumCustomSetStateDataV2(ImmutableList.of(), null));
        }
        final boolean directRoll = selectedButton.map(ButtonIdLabelAndDiceExpression::isDirectRoll).orElse(false);
        final List<ExpressionAndLabel> expressionWithNewValue = ImmutableList.<ExpressionAndLabel>builder()
                .addAll(currentExpressions)
                .add(addExpression.get())
                .build();

        return new State<>(directRoll ? ROLL_BUTTON_ID : buttonValue, new SumCustomSetStateDataV2(expressionWithNewValue, invokingUserName));
    }

    private String combineExpressions(List<ExpressionAndLabel> expressions, String prefix, String postfix) {
        String combinedExpression = expressions.stream()
                .map(ExpressionAndLabel::getExpression)
                .collect(Collectors.joining(""));
        return "%s%s%s".formatted(
                Optional.ofNullable(prefix).orElse(""),
                combinedExpression,
                Optional.ofNullable(postfix).orElse("")
        );
    }

    private String combineLabel(List<ExpressionAndLabel> expressions, SumCustomSetConfig config) {
        if (config.isHideExpressionInStatusAndAnswer()) {
            return expressions.stream()
                    .map(ExpressionAndLabel::getLabel)
                    .collect(Collectors.joining(" "));
        }
        return combineExpressions(expressions, config.getPrefix(), config.getPostfix());
    }

    @Override
    protected @NonNull SumCustomSetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options, @NonNull Locale userLocale) {
        final String buttonsOptionValue = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_NAME).orElseThrow();
        final List<ButtonIdLabelAndDiceExpression> buttons = ButtonHelper.parseString(buttonsOptionValue);
        final boolean alwaysSumResults = options.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_NAME).orElse(true);
        final Long answerTargetChannelId = BaseCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        final AnswerFormatType answerType = BaseCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(AnswerFormatType.full);
        final boolean hideExpressionInAnswer = options.getBooleanSubOptionWithName(HIDE_EXPRESSION_IN_ANSWER_OPTIONS_NAME).orElse(true);
        final boolean systemButtonNewLine = ENDS_WITH_DOUBLE_SEMICOLUMN_PATTERN.matcher(buttonsOptionValue).matches();
        final String prefix = options.getStringSubOptionWithName(PREFIX_OPTIONS_NAME).orElse(null);
        final String postfix = options.getStringSubOptionWithName(POSTFIX_OPTIONS_NAME).orElse(null);
        return new SumCustomSetConfig(answerTargetChannelId,
                buttons,
                alwaysSumResults,
                hideExpressionInAnswer,
                systemButtonNewLine,
                prefix,
                postfix,
                answerType,
                BaseCommandOptions.getAnswerInteractionFromStartCommandOption(options),
                null, new DiceStyleAndColor(
                BaseCommandOptions.getDiceStyleOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d),
                BaseCommandOptions.getDiceColorOptionFromStartCommandOption(options).orElse(DiceImageStyle.polyhedral_3d.getDefaultColor())),
                userLocale,
                null);
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID, SumCustomSetConfig config, boolean rollDisabled, boolean backDisabled, Set<String> disableButtonIds, Locale configLocale) {
        return ButtonHelper.extendButtonLayout(ButtonHelper.createButtonLayoutDetail(getCommandId(), configUUID, config.getLabelAndExpression().stream()
                        .map(b -> new ButtonHelper.ButtonIdLabelAndDiceExpressionExtension(b, disableButtonIds.contains(b.getButtonId()), null)).toList()),
                ImmutableList.<ButtonDefinition>builder()
                        .add(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, configUUID))
                                .label(I18n.getMessage("sum_custom_set.button.label.roll", configLocale))
                                .disabled(rollDisabled)
                                .style(ButtonDefinition.Style.SUCCESS)
                                .build())
                        .add(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, configUUID))
                                .label(I18n.getMessage("sum_custom_set.button.label.clear", configLocale))
                                .style(ButtonDefinition.Style.DANGER)
                                .build())
                        .add(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), BACK_BUTTON_ID, configUUID))
                                .label(I18n.getMessage("sum_custom_set.button.label.back", configLocale))
                                .style(ButtonDefinition.Style.SECONDARY)
                                .disabled(backDisabled)
                                .build()).build(), config.isSystemButtonNewLine());
    }

}
