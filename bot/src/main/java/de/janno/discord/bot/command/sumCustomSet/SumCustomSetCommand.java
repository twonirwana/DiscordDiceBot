package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.*;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageConfigDTO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
public class SumCustomSetCommand extends AbstractCommand<SumCustomSetConfig, SumCustomSetStateData> {
    private static final String COMMAND_NAME = "sum_custom_set";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String NO_ACTION = "no action";
    private static final String BUTTONS_COMMAND_OPTIONS_ID = "buttons";
    private static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID = "always_sum_result";
    private static final String EMPTY_MESSAGE = "Click the buttons to add dice to the set and then on Roll";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String BACK_BUTTON_ID = "back";

    private static final String LABEL_DELIMITER = "@";
    private static final String CONFIG_TYPE_ID = "SumCustomSetConfig";
    private static final String STATE_DATA_TYPE_ID = "SumCustomSetStateData";
    private final DiceSystemAdapter diceSystemAdapter;

    public SumCustomSetCommand(PersistenceManager persistenceManager, CachingDiceEvaluator cachingDiceEvaluator) {
        this(persistenceManager, new DiceParser(), cachingDiceEvaluator);
    }

    @VisibleForTesting
    public SumCustomSetCommand(PersistenceManager persistenceManager, Dice dice, CachingDiceEvaluator cachingDiceEvaluator) {
        super(persistenceManager);
        this.diceSystemAdapter = new DiceSystemAdapter(cachingDiceEvaluator, dice);
    }

    @Override
    protected ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> getMessageDataAndUpdateWithButtonValue(@NonNull MessageConfigDTO messageConfigDTO,
                                                                                                               @NonNull MessageDataDTO messageDataDTO,
                                                                                                               @NonNull String buttonValue,
                                                                                                               @NonNull String invokingUserName) {
        return deserializeAndUpdateState(messageConfigDTO, messageDataDTO, buttonValue, invokingUserName);
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull SumCustomSetConfig config, @NonNull State<SumCustomSetStateData> state) {
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
    ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> deserializeAndUpdateState(
            @NonNull MessageConfigDTO messageConfigDTO,
            @NonNull MessageDataDTO messageDataDTO,
            @NonNull String buttonValue,
            @NonNull String invokingUserName) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final SumCustomSetStateData loadedStateData = Optional.of(messageDataDTO)
                .map(MessageDataDTO::getStateData)
                .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateData.class))
                .orElse(null);
        final SumCustomSetConfig loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), SumCustomSetConfig.class);
        final State<SumCustomSetStateData> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getDiceExpressions).orElse(ImmutableList.of()),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getLockedForUserName).orElse(""),
                loadedConfig.getLabelAndExpression());
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull SumCustomSetConfig config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of dice";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates buttons with custom dice expression components, that can be combined afterwards.. \n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/sum_custom_set start buttons:+;d6;1;2;3;4;5;6;7;8;9;0`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return List.of(CommandDefinitionOption.builder()
                        .name(BUTTONS_COMMAND_OPTIONS_ID)
                        .description("Define one or more buttons separated by ';'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .required(true)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID)
                        .description("Always sum the results of the dice expressions")
                        .type(CommandDefinitionOption.Type.BOOLEAN)
                        .required(false)
                        .build());
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateData> state, long channelId, long userId) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumCustomSetStateData::getDiceExpressions)
                        .map(List::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        String label = config.getLabelAndExpression().stream()
                .filter(ld -> ld.getButtonId().equals(state.getButtonValue()))
                .map(ButtonIdLabelAndDiceExpression::getLabel)
                .findFirst().orElse(null);
        String newExpression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions()));

        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(newExpression,
                label,
                config.isAlwaysSumResult(),
                config.getDiceParserSystem(),
                config.getAnswerFormatType(),
                config.getResultImage()));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configUUID, SumCustomSetConfig config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID, config, true))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID customUuid, SumCustomSetConfig config, State<SumCustomSetStateData> state, long guildId, long channelId) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue()) && !Optional.ofNullable(state.getData())
                .map(SumCustomSetStateData::getDiceExpressions)
                .map(List::isEmpty)
                .orElse(false)) {
            return Optional.of(MessageDefinition.builder()
                    .content(EMPTY_MESSAGE)
                    .componentRowDefinitions(createButtonLayout(customUuid, config, true))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(UUID customUuid, SumCustomSetConfig config, State<SumCustomSetStateData> state, long channelId, long userId) {
        if (state.getData() == null) {
            return Optional.empty();
        }
        String expression = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, combineExpressions(state.getData().getDiceExpressions()));

        return Optional.of(createButtonLayout(customUuid, config, !diceSystemAdapter.isValidExpression(expression, config.getDiceParserSystem())));
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(EMPTY_MESSAGE);
        } else if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(EMPTY_MESSAGE);
        } else {
            if (Optional.ofNullable(state.getData())
                    .map(SumCustomSetStateData::getDiceExpressions)
                    .map(List::isEmpty)
                    .orElse(false)) {
                return Optional.of(EMPTY_MESSAGE);
            }
            if (Optional.ofNullable(state.getData()).map(SumCustomSetStateData::getLockedForUserName).isEmpty()) {
                return Optional.ofNullable(state.getData()).map(SumCustomSetStateData::getDiceExpressions).map(this::combineExpressions);
            } else {
                String cleanName = state.getData().getLockedForUserName();
                return Optional.of(String.format("%s: %s", cleanName, combineExpressions(state.getData().getDiceExpressions())));
            }
        }
    }


    private State<SumCustomSetStateData> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                    @NonNull final List<String> currentExpressions,
                                                                    @NonNull final String invokingUserName,
                                                                    @Nullable final String lockedToUser,
                                                                    @NonNull final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateData(ImmutableList.of(), null));
        }
        if (!Strings.isNullOrEmpty(lockedToUser) && !lockedToUser.equals(invokingUserName)) {
            return new State<>(NO_ACTION, new SumCustomSetStateData(currentExpressions, lockedToUser));
        }
        if (BACK_BUTTON_ID.equals(buttonValue)) {
            final List<String> newExpressionList;
            if (currentExpressions.isEmpty()) {
                newExpressionList = ImmutableList.of();
            } else {
                newExpressionList = ImmutableList.copyOf(currentExpressions.subList(0, currentExpressions.size() - 1));
            }
            return new State<>(buttonValue, new SumCustomSetStateData(newExpressionList, newExpressionList.isEmpty() ? null : lockedToUser));
        }
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateData(currentExpressions, lockedToUser));
        }
        final Optional<String> addExpression = buttonIdLabelAndDiceExpressions.stream()
                .filter(bld -> bld.getButtonId().equals(buttonValue))
                .map(ButtonIdLabelAndDiceExpression::getDiceExpression)
                .findFirst();
        if (addExpression.isEmpty()) {
            return new State<>(NO_ACTION, new SumCustomSetStateData(ImmutableList.of(), null));
        }
        final List<String> expressionWithNewValue = ImmutableList.<String>builder()
                .addAll(currentExpressions)
                .add(addExpression.get())
                .build();

        return new State<>(buttonValue, new SumCustomSetStateData(expressionWithNewValue, invokingUserName));
    }

    private String combineExpressions(List<String> expressions) {
        return String.join("", expressions);
    }

    @Override
    protected @NonNull SumCustomSetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        List<ButtonIdAndExpression> buttons = getButtonsFromCommandInteractionOption(options);
        boolean alwaysSumResults = options.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID).orElse(true);
        final DiceParserSystem diceParserSystem = DiceParserSystem.DICE_EVALUATOR;
        Long answerTargetChannelId = DefaultCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = DefaultCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        ResultImage resultImage = DefaultCommandOptions.getResultImageOptionFromStartCommandOption(options).orElse(defaultResultImage());
        return getConfigOptionStringList(buttons, answerTargetChannelId, diceParserSystem, alwaysSumResults, answerType, resultImage);
    }

    private List<ButtonIdAndExpression> getButtonsFromCommandInteractionOption(@NonNull CommandInteractionOption options) {
        ImmutableList.Builder<ButtonIdAndExpression> builder = ImmutableList.builder();
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_ID).orElseThrow();
        int idCounter = 1;
        for (String button : buttons.split(";")) {
            builder.add(new ButtonIdAndExpression(idCounter++ + "_button", button));
        }
        return builder.build();
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID customUUID, SumCustomSetConfig config, boolean rollDisabled) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), d.getButtonId(), customUUID))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, customUUID))
                .label("Roll")
                .disabled(rollDisabled)
                .style(rollDisabled ? ButtonDefinition.Style.PRIMARY : ButtonDefinition.Style.SUCCESS)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, customUUID))
                .label("Clear")
                .style(ButtonDefinition.Style.DANGER)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), BACK_BUTTON_ID, customUUID))
                .label("Back")
                .style(ButtonDefinition.Style.SECONDARY)
                .build());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    SumCustomSetConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions,
                                                 Long answerTargetChannelId,
                                                 DiceParserSystem diceParserSystem,
                                                 boolean alwaysSumResult,
                                                 AnswerFormatType answerFormatType,
                                                 ResultImage resultImage) {
        return new SumCustomSetConfig(answerTargetChannelId, startOptions.stream()
                .filter(be -> !be.getExpression().contains(BottomCustomIdUtils.CUSTOM_ID_DELIMITER))
                .filter(be -> !be.getExpression().contains(LABEL_DELIMITER) || be.getExpression().split(LABEL_DELIMITER).length == 2)
                .map(be -> {
                    String label = null;
                    String diceExpression;
                    if (be.getExpression().contains(LABEL_DELIMITER)) {
                        String[] split = be.getExpression().split(LABEL_DELIMITER);
                        label = split[1].trim();
                        diceExpression = split[0].trim();
                    } else {
                        diceExpression = be.getExpression().trim();
                    }
                    if (!diceExpression.startsWith("+") && !diceExpression.startsWith("-")
                            && diceParserSystem == DiceParserSystem.DICEROLL_PARSER) {
                        diceExpression = "+" + diceExpression;
                    }
                    if (label == null) {
                        label = diceExpression;
                    }
                    return new ButtonIdLabelAndDiceExpression(be.getButtonId(), label, diceExpression);
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> {
                    if (DiceParserSystem.DICEROLL_PARSER == diceParserSystem) {
                        return diceSystemAdapter.isValidExpression(lv.getDiceExpression(), diceParserSystem);
                    }
                    return true;
                })
                .distinct()
                .limit(22)
                .collect(Collectors.toList()),
                diceParserSystem, alwaysSumResult, answerFormatType, resultImage);
    }

    @Value
    private static class ButtonIdAndExpression {
        @NonNull
        String buttonId;
        @NonNull
        String expression;
    }

}
