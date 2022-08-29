package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BotConstants;
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
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
public class SumCustomSetCommand extends AbstractCommand<SumCustomSetConfig, SumCustomSetStateData> {
    private static final String COMMAND_NAME = "sum_custom_set";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String NO_ACTION = "no action";
    private static final String EMPTY_MESSAGE = "Click the buttons to add dice to the set and then on Roll";
    private static final String EMPTY_MESSAGE_LEGACY = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String BACK_BUTTON_ID = "back";
    private static final List<String> DICE_COMMAND_OPTIONS_IDS = IntStream.range(1, 22).mapToObj(i -> i + "_button").toList();
    private static final String INVOKING_USER_NAME_DELIMITER = "\u2236 ";
    private static final String LABEL_DELIMITER = "@";
    private static final String CONFIG_TYPE_ID = "SumCustomSetConfig";
    private static final String STATE_DATA_TYPE_ID = "SumCustomSetStateData";
    private final DiceParserHelper diceParserHelper;

    public SumCustomSetCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceParserHelper());
    }

    @VisibleForTesting
    public SumCustomSetCommand(MessageDataDAO messageDataDAO, DiceParserHelper diceParserHelper) {
        super(messageDataDAO);
        this.diceParserHelper = diceParserHelper;
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
        if (state.getData() == null) {
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
                loadedConfig.getLabelAndExpression());
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID, long channelId, long messageId, @NonNull Config config, @Nullable State<SumCustomSetStateData> state) {
        Preconditions.checkArgument(config instanceof SumCustomSetConfig, "Wrong config: %s", config);
        return Optional.of(new MessageDataDTO(configUUID, channelId, messageId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of dice";
    }


    @Override
    protected @NonNull EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Creates up to 22 buttons with custom dice expression, that can be combined afterwards. e.g. '/sum_custom_set start 1_button:3d6 2_button:10d10 3_button:3d20'. \n" + DiceParserHelper.HELP)
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }


    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        List<String> diceExpressionWithOptionalLabel = DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .distinct()
                .collect(Collectors.toList());
        String expressionsWithMultiRoll = diceExpressionWithOptionalLabel.stream()
                .filter(s -> s.contains("x["))
                .collect(Collectors.joining(","));
        if (!Strings.isNullOrEmpty(expressionsWithMultiRoll)) {
            return Optional.of(String.format("This command doesn't support multiple rolls, the following expression are not allowed: %s", expressionsWithMultiRoll));
        }
        String expressionWithUserNameDelimiter = diceExpressionWithOptionalLabel.stream()
                .filter(s -> s.contains(INVOKING_USER_NAME_DELIMITER))
                .collect(Collectors.joining(","));
        if (!Strings.isNullOrEmpty(expressionWithUserNameDelimiter)) {
            return Optional.of(String.format("This command doesn't allow '%s' in the dice expression and label, the following expression are not allowed: %s", INVOKING_USER_NAME_DELIMITER, expressionWithUserNameDelimiter));
        }
        int maxCharacter = 2000; //2000 is the max message length
        return diceParserHelper.validateListOfExpressions(diceExpressionWithOptionalLabel, LABEL_DELIMITER, BotConstants.CUSTOM_ID_DELIMITER, "/sum_custom_set help", maxCharacter);
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return DICE_COMMAND_OPTIONS_IDS.stream()
                .map(id -> CommandDefinitionOption.builder()
                        .name(id)
                        .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull Optional<EmbedDefinition> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
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
        return Optional.of(diceParserHelper.roll(combineExpressions(state.getData().getDiceExpressions()), label));
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(SumCustomSetConfig config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(config))
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
                    .componentRowDefinitions(createButtonLayout(config))
                    .build());
        }
        return Optional.empty();
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
        String[] split = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        Deque<String> buttonIds = new ArrayDeque<>(DICE_COMMAND_OPTIONS_IDS);
        return new SumCustomSetConfig(answerTargetChannelId, event.getAllButtonIds().stream()
                .filter(lv -> !ImmutableSet.of(ROLL_BUTTON_ID, CLEAR_BUTTON_ID, BACK_BUTTON_ID).contains(getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .map(lv -> new ButtonIdLabelAndDiceExpression(buttonIds.pop(), lv.getLabel(), getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .collect(Collectors.toList()));
    }

    private State<SumCustomSetStateData> updateStateWithButtonValue(@NonNull final String buttonValue,
                                                                    @NonNull final List<String> currentExpressions,
                                                                    @NonNull final String invokingUserName,
                                                                    @Nullable final String lockedToUser,
                                                                    @NonNull final List<ButtonIdLabelAndDiceExpression> buttonIdLabelAndDiceExpressions) {
        String currentExpression = combineExpressions(currentExpressions);
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateData(ImmutableList.of(), null));
        }
        if (!Strings.isNullOrEmpty(lockedToUser) && !lockedToUser.equals(invokingUserName)) {
            return new State<>(NO_ACTION, new SumCustomSetStateData(currentExpressions, lockedToUser));
        }
        if (!currentExpressions.isEmpty() && !diceParserHelper.validExpression(currentExpression)) {
            //invalid expression -> clear
            return new State<>(NO_ACTION, new SumCustomSetStateData(ImmutableList.of(), null));
        }
        if (BACK_BUTTON_ID.equals(buttonValue)) {
            List<String> newExpressionList = ImmutableList.copyOf(currentExpressions.subList(0, currentExpressions.size() - 1));
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

        if (!Strings.isNullOrEmpty(combineExpressions(expressionWithNewValue)) && !diceParserHelper.validExpression(combineExpressions(expressionWithNewValue))) {
            //invalid expression -> clear
            return new State<>(NO_ACTION, new SumCustomSetStateData(ImmutableList.of(), null));
        }
        return new State<>(buttonValue, new SumCustomSetStateData(expressionWithNewValue, invokingUserName));
    }

    private String combineExpressions(List<String> expressions) {
        String expressionBuilder = "";
        for (String currentExpression : expressions) {
            String operator = "+";
            final String buttonValueWithoutOperator;
            if (currentExpression.startsWith("-")) {
                operator = "-";
                buttonValueWithoutOperator = currentExpression.substring(1);
            } else if (currentExpression.startsWith("+")) {
                buttonValueWithoutOperator = currentExpression.substring(1);
            } else {
                buttonValueWithoutOperator = currentExpression;
            }

            if (Strings.isNullOrEmpty(expressionBuilder)) {
                expressionBuilder = operator.equals("-") ? String.format("-%s", buttonValueWithoutOperator) : buttonValueWithoutOperator;
            } else {
                expressionBuilder = String.format("%s%s%s", expressionBuilder, operator, buttonValueWithoutOperator);
            }
        }
        return expressionBuilder;
    }

    @Override
    protected @NonNull State<SumCustomSetStateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        String buttonValue = getButtonValueFromLegacyCustomId(event.getCustomId());
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
        return updateStateWithButtonValue(buttonId, currentExpression, event.getInvokingGuildMemberName(), lockedToUser, config.getLabelAndExpression());
    }

    @Override
    protected @NonNull SumCustomSetConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return getConfigOptionStringList(DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream()
                        .map(e -> new ButtonIdAndExpression(id, e)))
                .collect(Collectors.toList()), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));
    }

    @VisibleForTesting
    SumCustomSetConfig getConfigOptionStringList(List<ButtonIdAndExpression> startOptions, Long answerTargetChannelId) {
        return new SumCustomSetConfig(answerTargetChannelId, startOptions.stream()
                .filter(be -> !be.getExpression().contains(BotConstants.CUSTOM_ID_DELIMITER))
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
                    if (!diceExpression.startsWith("+") && !diceExpression.startsWith("-")) {
                        diceExpression = "+" + diceExpression;
                    }
                    if (label == null) {
                        label = diceExpression;
                    }
                    return new ButtonIdLabelAndDiceExpression(be.getButtonId(), label, diceExpression);
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> diceParserHelper.validExpression(lv.getDiceExpression()))
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(22)
                .collect(Collectors.toList()));
    }

    private List<ComponentRowDefinition> createButtonLayout(SumCustomSetConfig config) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(createButtonCustomId(d.getButtonId()))
                        .label(d.getLabel())
                        .build())
                .collect(Collectors.toList());
        buttons.add(ButtonDefinition.builder()
                .id(createButtonCustomId(ROLL_BUTTON_ID))
                .label("Roll")
                .style(ButtonDefinition.Style.SUCCESS)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(createButtonCustomId(CLEAR_BUTTON_ID))
                .label("Clear")
                .style(ButtonDefinition.Style.DANGER)
                .build());
        buttons.add(ButtonDefinition.builder()
                .id(createButtonCustomId(BACK_BUTTON_ID))
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
