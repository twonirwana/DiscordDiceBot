package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.BotMetrics;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.bot.dice.DiceParser;
import de.janno.discord.bot.dice.DiceParserSystem;
import de.janno.discord.bot.dice.DiceSystemAdapter;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
public class SumCustomSetCommand extends AbstractCommand<SumCustomSetConfig, SumCustomSetStateData> {
    private static final String COMMAND_NAME = "sum_custom_set";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String NO_ACTION = "no action";
    private static final String LEGACY_START_ACTION = "legacy_start";
    private static final String BUTTONS_COMMAND_OPTIONS_ID = "buttons";
    private static final String ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID = "always_sum_result";
    private static final String EMPTY_MESSAGE = "Click the buttons to add dice to the set and then on Roll";
    private static final String EMPTY_MESSAGE_LEGACY = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String BACK_BUTTON_ID = "back";
    private static final List<String> LEGACY_DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 22).mapToObj(i -> i + "_button").toList();
    private static final String INVOKING_USER_NAME_DELIMITER = "\u2236 ";
    private static final String LABEL_DELIMITER = "@";
    private static final String CONFIG_TYPE_ID = "SumCustomSetConfig";
    private static final String STATE_DATA_TYPE_ID = "SumCustomSetStateData";
    private final DiceSystemAdapter diceSystemAdapter;

    public SumCustomSetCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParser(), new RandomNumberSupplier());
    }

    @VisibleForTesting
    public SumCustomSetCommand(MessageDataDAO messageDataDAO, Dice dice, NumberSupplier numberSupplier) {
        super(messageDataDAO);
        this.diceSystemAdapter = new DiceSystemAdapter(numberSupplier, 1000, dice);
    }

    @Override
    protected Optional<ConfigAndState<SumCustomSetConfig, SumCustomSetStateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                                         long messageId,
                                                                                                                         @NonNull String buttonValue,
                                                                                                                         @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        if (messageDataDTO.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deserializeAndUpdateState(messageDataDTO.get(), buttonValue, invokingUserName));
    }

    @Override
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull SumCustomSetConfig config, @NonNull State<SumCustomSetStateData> state) {
        if (state.getData() == null || ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, Mapper.NO_PERSISTED_STATE, null);
        } else {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData()));
        }
    }

    @VisibleForTesting
    ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO,
                                                                                        @NonNull String buttonValue,
                                                                                        @NonNull String invokingUserName) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        Preconditions.checkArgument(STATE_DATA_TYPE_ID.equals(messageDataDTO.getStateDataClassId())
                || Mapper.NO_PERSISTED_STATE.equals(messageDataDTO.getStateDataClassId()), "Unknown stateDataClassId: %s", messageDataDTO.getStateDataClassId());

        final SumCustomSetStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateData.class))
                .orElse(null);
        final SumCustomSetConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), SumCustomSetConfig.class);
        final State<SumCustomSetStateData> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getDiceExpressions).orElse(ImmutableList.of()),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getLockedForUserName).orElse(""),
                loadedConfig.getLabelAndExpression(), loadedConfig.getDiceParserSystem());
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull SumCustomSetConfig config,
                                                                   @Nullable State<SumCustomSetStateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of dice";
    }


    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Creates buttons with custom dice expression components, that can be combined afterwards. e.g. '/sum_custom_set start buttons:+;1d6;1;2;3'. \n" + diceSystemAdapter.getHelpText(DiceParserSystem.DICEROLL_PARSER))
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        List<String> diceExpressionWithOptionalLabel = getButtonsFromCommandInteractionOption(options).stream()
                .map(ButtonIdAndExpression::getExpression)
                .distinct()
                .collect(Collectors.toList());
        DiceParserSystem diceParserSystem = options.getName().equals(LEGACY_START_ACTION) ? DiceParserSystem.DICEROLL_PARSER : DiceParserSystem.DICE_EVALUATOR;
        return diceSystemAdapter.validateListOfExpressions(diceExpressionWithOptionalLabel, "/sum_custom_set help", diceParserSystem);
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
    protected Collection<CommandDefinitionOption> additionalCommandOptions() {
        return List.of(CommandDefinitionOption.builder()
                .name(LEGACY_START_ACTION)
                .description("Old start command")
                .type(CommandDefinitionOption.Type.SUB_COMMAND)
                .options(LEGACY_DICE_COMMAND_OPTIONS_IDS.stream()
                        .map(id -> CommandDefinitionOption.builder()
                                .name(id)
                                .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                                .type(CommandDefinitionOption.Type.STRING)
                                .build())
                        .collect(Collectors.toList()))
                .option(ANSWER_TARGET_CHANNEL_COMMAND_OPTION)
                .build());
    }

    @Override
    protected Set<String> getStartOptionIds() {
        return Set.of(ACTION_START, LEGACY_START_ACTION);
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
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

        return Optional.of(diceSystemAdapter.answerRollWithGivenLabel(combineExpressions(state.getData().getDiceExpressions()),
                label,
                config.isAlwaysSumResult(),
                config.getDiceParserSystem(),
                config.getAnswerFormatType()));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(SumCustomSetConfig config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(config, true))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue()) && !Optional.ofNullable(state.getData())
                .map(SumCustomSetStateData::getDiceExpressions)
                .map(List::isEmpty)
                .orElse(false)) {
            return Optional.of(MessageDefinition.builder()
                    .content(EMPTY_MESSAGE)
                    .componentRowDefinitions(createButtonLayout(config, true))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (state.getData() == null) {
            return Optional.empty();
        }
        String expression = combineExpressions(state.getData().getDiceExpressions());
        return Optional.of(createButtonLayout(config, !diceSystemAdapter.isValidExpression(expression, config.getDiceParserSystem())));
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
                String cleanName = state.getData().getLockedForUserName().replace(INVOKING_USER_NAME_DELIMITER, "");
                return Optional.of(String.format("%s%s%s", cleanName, INVOKING_USER_NAME_DELIMITER, combineExpressions(state.getData().getDiceExpressions())));
            }
        }
    }


    @Override
    protected @NonNull SumCustomSetConfig getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BottomCustomIdUtils.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        Deque<String> buttonIds = new ArrayDeque<>(IntStream.range(1, 23).mapToObj(i -> i + "_button").toList()); //legacy can have 22 buttons
        return new SumCustomSetConfig(answerTargetChannelId, event.getAllButtonIds().stream()
                .filter(lv -> !ImmutableSet.of(ROLL_BUTTON_ID, CLEAR_BUTTON_ID, BACK_BUTTON_ID).contains(BottomCustomIdUtils.getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .map(lv -> new ButtonIdLabelAndDiceExpression(buttonIds.pop(), lv.getLabel(), BottomCustomIdUtils.getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .collect(Collectors.toList()), DiceParserSystem.DICEROLL_PARSER, true, AnswerFormatType.full);
    }

    private State<SumCustomSetStateData> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                    @NonNull final List<String> currentExpressions,
                                                                    @NonNull final String invokingUserName,
                                                                    @Nullable final String lockedToUser,
                                                                    @NonNull final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions,
                                                                    @NonNull final DiceParserSystem diceParserSystem) {
        String currentExpression = combineExpressions(currentExpressions);
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
            if (!diceSystemAdapter.isValidExpression(currentExpression, diceParserSystem)) {
                //should not happen, button only enabled if expression is valid
                log.error("Roll button pressed but expression is not valid: {}", currentExpression);
                return new State<>(NO_ACTION, new SumCustomSetStateData(currentExpressions, lockedToUser));
            }
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
    protected @NonNull State<SumCustomSetStateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        String buttonValue = BottomCustomIdUtils.getButtonValueFromLegacyCustomId(event.getCustomId());
        String buttonMessageWithOptionalUser = event.getMessageContent();

        String lockedToUser = null;
        List<String> currentExpression;

        if (buttonMessageWithOptionalUser.contains(INVOKING_USER_NAME_DELIMITER)) {
            int firstDelimiter = buttonMessageWithOptionalUser.indexOf(INVOKING_USER_NAME_DELIMITER);
            lockedToUser = buttonMessageWithOptionalUser.substring(0, firstDelimiter);
            currentExpression = ImmutableList.of(buttonMessageWithOptionalUser.substring(firstDelimiter + INVOKING_USER_NAME_DELIMITER.length()));
        } else if (EMPTY_MESSAGE.equals(buttonMessageWithOptionalUser) || EMPTY_MESSAGE_LEGACY.equals(buttonMessageWithOptionalUser)) {
            currentExpression = ImmutableList.of();
        } else {
            currentExpression = ImmutableList.of(buttonMessageWithOptionalUser);
        }
        SumCustomSetConfig config = getConfigFromEvent(event);
        final String buttonId;
        if (ImmutableSet.of(CLEAR_BUTTON_ID, BACK_BUTTON_ID, ROLL_BUTTON_ID).contains(buttonValue)) {
            buttonId = buttonValue;
        } else {
            buttonId = config.getLabelAndExpression().stream()
                    .filter(bld -> bld.getDiceExpression().equals(buttonValue))
                    .map(ButtonIdLabelAndDiceExpression::getButtonId)
                    .findFirst().orElse("");
        }
        return updateStateWithButtonValue(buttonId,
                currentExpression,
                event.getInvokingGuildMemberName(),
                lockedToUser,
                config.getLabelAndExpression(),
                config.getDiceParserSystem());
    }

    @Override
    protected @NonNull SumCustomSetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        List<ButtonIdAndExpression> buttons = getButtonsFromCommandInteractionOption(options);
        boolean alwaysSumResults = options.getBooleanSubOptionWithName(ALWAYS_SUM_RESULTS_COMMAND_OPTIONS_ID).orElse(true);
        boolean isLegacy = LEGACY_START_ACTION.equals(options.getName());
        final DiceParserSystem diceParserSystem;
        if (isLegacy) {
            BotMetrics.incrementLegacyStartCounter(getCommandId());
            diceParserSystem = DiceParserSystem.DICEROLL_PARSER;
        } else {
            diceParserSystem = DiceParserSystem.DICE_EVALUATOR;
        }
        Long answerTargetChannelId = getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        return getConfigOptionStringList(buttons, answerTargetChannelId, diceParserSystem, alwaysSumResults, getAnswerTypeFromStartCommandOption(options));
    }

    private List<ButtonIdAndExpression> getButtonsFromCommandInteractionOption(@NonNull CommandInteractionOption options) {
        if (LEGACY_START_ACTION.equals(options.getName())) {
            return LEGACY_DICE_COMMAND_OPTIONS_IDS.stream()
                    .flatMap(id -> options.getStringSubOptionWithName(id).stream()
                            .map(e -> new ButtonIdAndExpression(id, e)))
                    .toList();
        }
        ImmutableList.Builder<ButtonIdAndExpression> builder = ImmutableList.builder();
        String buttons = options.getStringSubOptionWithName(BUTTONS_COMMAND_OPTIONS_ID).orElseThrow();
        int idCounter = 1;
        for (String button : buttons.split(";")) {
            builder.add(new ButtonIdAndExpression(idCounter++ + "_button", button));
        }
        return builder.build();
    }

    @VisibleForTesting
    SumCustomSetConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions,
                                                 Long answerTargetChannelId,
                                                 DiceParserSystem diceParserSystem,
                                                 boolean alwaysSumResult,
                                                 AnswerFormatType answerFormatType) {
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
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(22)
                .collect(Collectors.toList()),
                diceParserSystem, alwaysSumResult, answerFormatType);
    }

    private List<ComponentRowDefinition> createButtonLayout(SumCustomSetConfig config, boolean rollDisabled) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), d.getButtonId()))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID))
                .label("Roll")
                .disabled(rollDisabled)
                .style(rollDisabled ? ButtonDefinition.Style.PRIMARY : ButtonDefinition.Style.SUCCESS)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID))
                .label("Clear")
                .style(ButtonDefinition.Style.DANGER)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), BACK_BUTTON_ID))
                .label("Back")
                .style(ButtonDefinition.Style.SECONDARY)
                .build());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Value
    private static class ButtonIdAndExpression {
        @NonNull
        String buttonId;
        @NonNull
        String expression;
    }

}
