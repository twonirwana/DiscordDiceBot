package de.janno.discord.bot.command.sumCustomSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.MessageDataDAO;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BotConstants;
import de.janno.discord.connector.api.IButtonEventAdaptor;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, "SumCustomSetStateData", Mapper.serializedObject(state.getData()));
        }
    }

    @VisibleForTesting
    ConfigAndState<SumCustomSetConfig, SumCustomSetStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO,
                                                                                        @NonNull String buttonValue,
                                                                                        @NonNull String invokingUserName) {

        final SumCustomSetStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, SumCustomSetStateData.class))
                .orElse(null);
        final SumCustomSetConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), SumCustomSetConfig.class);
        final State<SumCustomSetStateData> updatedState = updateStateWithButtonValue(buttonValue,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getDiceExpression).orElse(""),
                invokingUserName,
                Optional.ofNullable(loadedStateData).map(SumCustomSetStateData::getLockedForUserName).orElse(""));
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    protected MessageDataDTO createMessageDataForNewMessage(@NonNull UUID configUUID, long channelId, long messageId, @NonNull SumCustomSetConfig config, @Nullable State<SumCustomSetStateData> state) {
        return new MessageDataDTO(configUUID, channelId, messageId, getCommandId(), "SumCustomSetConfig", Mapper.serializedObject(config));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of dice";
    }


    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Creates up to 22 buttons with custom dice expression, that can be combined afterwards. e.g. '/sum_custom_set start 1_button:3d6 2_button:10d10 3_button:3d20'. \n" + DiceParserHelper.HELP)
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }


    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
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
        String answerTargetChannel = getAnswerTargetChannelIdFromStartCommandOption(options).map(Object::toString).orElse("");
        //todo update to new max
        int maxCharacter = 100 - COMMAND_NAME.length()
                - 2 // delimiter;
                - answerTargetChannel.length();
        return diceParserHelper.validateListOfExpressions(diceExpressionWithOptionalLabel, LABEL_DELIMITER, BotConstants.LEGACY_DELIMITER_V2, "/sum_custom_set help", maxCharacter);
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return DICE_COMMAND_OPTIONS_IDS.stream()
                .map(id -> CommandDefinitionOption.builder()
                        .name(id)
                        .description("xdy for a set of x dice with y sides, e.g. '3d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumCustomSetStateData::getDiceExpression)
                        .map(String::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        String label = config.getLabelAndExpression().stream()
                .filter(ld -> !ld.getDiceExpression().equals(ld.getLabel()))
                .filter(ld -> ld.getDiceExpression().equals(state.getData().getDiceExpression()))
                .map(LabelAndDiceExpression::getLabel)
                .findFirst().orElse(null);
        return Optional.of(diceParserHelper.roll(state.getData().getDiceExpression(), label));
    }

    @Override
    public MessageDefinition createNewButtonMessage(SumCustomSetConfig config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue()) && !Optional.ofNullable(state.getData())
                .map(SumCustomSetStateData::getDiceExpression)
                .map(Strings::isNullOrEmpty)
                .orElse(false)) {
            return Optional.of(MessageDefinition.builder()
                    .content(EMPTY_MESSAGE)
                    .componentRowDefinitions(createButtonLayout(config))
                    .build());
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getCurrentMessageContentChange(SumCustomSetConfig config, State<SumCustomSetStateData> state) {
        if (ROLL_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(EMPTY_MESSAGE);
        } else if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.of(EMPTY_MESSAGE);
        } else {
            if (Optional.ofNullable(state.getData())
                    .map(SumCustomSetStateData::getDiceExpression)
                    .map(Strings::isNullOrEmpty)
                    .orElse(false)) {
                return Optional.of(EMPTY_MESSAGE);
            }
            if (Optional.ofNullable(state.getData()).map(SumCustomSetStateData::getLockedForUserName).isEmpty()) {
                return Optional.ofNullable(state.getData()).map(SumCustomSetStateData::getDiceExpression);
            } else {
                String cleanName = state.getData().getLockedForUserName().replace(INVOKING_USER_NAME_DELIMITER, "");
                return Optional.of(String.format("%s%s%s", cleanName, INVOKING_USER_NAME_DELIMITER, state.getData().getDiceExpression()));
            }
        }
    }


    @Override
    protected SumCustomSetConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        Long answerTargetChannelId = getOptionalLongFromArray(split, 2);
        return new SumCustomSetConfig(answerTargetChannelId, event.getAllButtonIds().stream()
                .filter(lv -> !ImmutableSet.of(ROLL_BUTTON_ID, CLEAR_BUTTON_ID, BACK_BUTTON_ID).contains(getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .map(lv -> new LabelAndDiceExpression(lv.getLabel(), getButtonValueFromLegacyCustomId(lv.getCustomId())))
                .collect(Collectors.toList()));
    }

    private State<SumCustomSetStateData> updateStateWithButtonValue(@NonNull String buttonValue,
                                                                    @NonNull String currentExpression,
                                                                    @NonNull String invokingUserName,
                                                                    @Nullable String lockedToUser) {
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateData("", null));
        }
        currentExpression = ImmutableSet.of(EMPTY_MESSAGE, EMPTY_MESSAGE_LEGACY).contains(currentExpression) ? "" : currentExpression;
        if (!Strings.isNullOrEmpty(lockedToUser) && !lockedToUser.equals(invokingUserName)) {
            return new State<>(NO_ACTION, new SumCustomSetStateData(currentExpression, lockedToUser));
        }
        if (!Strings.isNullOrEmpty(currentExpression) && !diceParserHelper.validExpression(currentExpression)) {
            //invalid expression -> clear
            return new State<>(NO_ACTION, new SumCustomSetStateData("", null));
        }
        if (BACK_BUTTON_ID.equals(buttonValue)) {
            int indexOfLastMinusOrPlus = Math.max(currentExpression.lastIndexOf("+"), currentExpression.lastIndexOf("-"));
            String newButtonMessage;
            if (indexOfLastMinusOrPlus > 0) {
                newButtonMessage = currentExpression.substring(0, indexOfLastMinusOrPlus);
            } else {
                newButtonMessage = "";
            }
            return new State<>(buttonValue, new SumCustomSetStateData(newButtonMessage, lockedToUser));
        }
        if (ROLL_BUTTON_ID.equals(buttonValue)) {
            return new State<>(buttonValue, new SumCustomSetStateData(currentExpression, lockedToUser));
        }

        String operator = "+";
        if (buttonValue.startsWith("-")) {
            operator = "-";
            buttonValue = buttonValue.substring(1);
        } else if (buttonValue.startsWith("+")) {
            buttonValue = buttonValue.substring(1);
        }

        String newContent;
        if (Strings.isNullOrEmpty(currentExpression)) {
            newContent = operator.equals("-") ? String.format("-%s", buttonValue) : buttonValue;
        } else {
            newContent = String.format("%s%s%s", currentExpression, operator, buttonValue);
        }
        if (!Strings.isNullOrEmpty(newContent) && !diceParserHelper.validExpression(newContent)) {
            //invalid expression -> clear
            return new State<>(NO_ACTION, new SumCustomSetStateData("", null));
        }
        return new State<>(buttonValue, new SumCustomSetStateData(newContent, invokingUserName));
    }

    @Override
    protected State<SumCustomSetStateData> getStateFromEvent(IButtonEventAdaptor event) {
        String buttonValue = getButtonValueFromLegacyCustomId(event.getCustomId());
        String buttonMessageWithOptionalUser = event.getMessageContent();

        String lockedToUser = null;
        String currentExpression;
        if (buttonMessageWithOptionalUser.contains(INVOKING_USER_NAME_DELIMITER)) {
            int firstDelimiter = buttonMessageWithOptionalUser.indexOf(INVOKING_USER_NAME_DELIMITER);
            lockedToUser = buttonMessageWithOptionalUser.substring(0, firstDelimiter);
            currentExpression = buttonMessageWithOptionalUser.substring(firstDelimiter + INVOKING_USER_NAME_DELIMITER.length());
        } else {
            currentExpression = buttonMessageWithOptionalUser;
        }
        return updateStateWithButtonValue(buttonValue, currentExpression, event.getInvokingGuildMemberName(), lockedToUser);
    }

    @Override
    protected SumCustomSetConfig getConfigFromStartOptions(CommandInteractionOption options) {
        return getConfigOptionStringList(DICE_COMMAND_OPTIONS_IDS.stream()
                .flatMap(id -> options.getStringSubOptionWithName(id).stream())
                .collect(Collectors.toList()), getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));
    }

    @VisibleForTesting
    SumCustomSetConfig getConfigOptionStringList(List<String> startOptions, Long answerTargetChannelId) {
        return new SumCustomSetConfig(answerTargetChannelId, startOptions.stream()
                .filter(s -> !s.contains(BotConstants.LEGACY_DELIMITER_V2))
                .filter(s -> !s.contains(LABEL_DELIMITER) || s.split(LABEL_DELIMITER).length == 2)
                .map(s -> {
                    String label = null;
                    String diceExpression;
                    if (s.contains(LABEL_DELIMITER)) {
                        String[] split = s.split(LABEL_DELIMITER);
                        label = split[1].trim();
                        diceExpression = split[0].trim();
                    } else {
                        diceExpression = s.trim();
                    }
                    if (!diceExpression.startsWith("+") && !diceExpression.startsWith("-")) {
                        diceExpression = "+" + diceExpression;
                    }
                    if (label == null) {
                        label = diceExpression;
                    }
                    return new LabelAndDiceExpression(label, diceExpression);
                })
                .filter(s -> !s.getDiceExpression().isEmpty())
                .filter(s -> !s.getLabel().isEmpty())
                .filter(lv -> diceParserHelper.validExpression(lv.getDiceExpression()))
                .filter(s -> s.getDiceExpression().length() <= 80) //limit for the ids are 100 characters and we need also some characters for the type...
                .filter(s -> s.getLabel().length() <= 80) //https://discord.com/developers/docs/interactions/message-components#buttons
                .distinct()
                .limit(22)
                .collect(Collectors.toList()));
    }


    private List<ComponentRowDefinition> createButtonLayout(SumCustomSetConfig config) {
        List<ButtonDefinition> buttons = config.getLabelAndExpression().stream()
                .map(d -> ButtonDefinition.builder()
                        .id(createButtonCustomId(d.getDiceExpression()))
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


}
