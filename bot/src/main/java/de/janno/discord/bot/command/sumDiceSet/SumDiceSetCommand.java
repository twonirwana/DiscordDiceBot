package de.janno.discord.bot.command.sumDiceSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SumDiceSetCommand extends AbstractCommand<Config, SumDiceSetStateData> {
    private static final String COMMAND_NAME = "sum_dice_set";
    private static final String DICE_SET_DELIMITER = " ";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String EMPTY_MESSAGE = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String X2_BUTTON_ID = "x2";
    private static final String DICE_SYMBOL = "d";
    private static final String MODIFIER_KEY = "m";
    private static final String CONFIG_TYPE_ID = "Config";
    private static final String STATE_DATA_TYPE_ID = "SumDiceSetStateData";
    private final DiceUtils diceUtils;

    public SumDiceSetCommand(PersistenceManager persistenceManager) {
        this(persistenceManager, new DiceUtils());
    }

    @VisibleForTesting
    public SumDiceSetCommand(PersistenceManager persistenceManager, DiceUtils diceUtils) {
        super(persistenceManager);
        this.diceUtils = diceUtils;
    }

    private static String parseDiceMapToMessageString(List<DiceKeyAndValue> diceSet) {
        String message = diceSet.stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getDiceKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getDiceKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .map(e -> {
                    if (MODIFIER_KEY.equals(e.getDiceKey())) {
                        return String.format("%s%d", e.getValue() > 0 ? "+" : "", e.getValue());
                    }
                    return String.format("%s%d%s", e.getValue() > 0 ? "+" : "", e.getValue(), e.getDiceKey());
                })
                .collect(Collectors.joining(DICE_SET_DELIMITER));
        //remove leading +
        if (message.startsWith("+")) {
            message = message.substring(1);
        }
        return message;
    }


    @Override
    protected Optional<ConfigAndState<Config, SumDiceSetStateData>> getMessageDataAndUpdateWithButtonValue(@Nullable UUID configUUID,
                                                                                                           long channelId,
                                                                                                           long messageId,
                                                                                                           @NonNull String buttonValue,
                                                                                                           @NonNull String invokingUserName) {
        final Optional<MessageConfigDTO> messageConfigDTO = getMessageConfigDTO(configUUID, channelId, messageId);
        final Optional<MessageDataDTO> messageStateDTO = persistenceManager.getStateForMessage(channelId, messageId);
        return messageConfigDTO.map(configDTO -> deserializeAndUpdateState(configDTO, messageStateDTO.orElse(null), buttonValue));
    }

    @VisibleForTesting
    ConfigAndState<Config, SumDiceSetStateData> deserializeAndUpdateState(@NonNull MessageConfigDTO messageConfigDTO, @Nullable MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageConfigDTO.getConfigClassId()), "Unknown configClassId: %s", messageConfigDTO.getConfigClassId());
        Preconditions.checkArgument(Optional.ofNullable(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId)
                .map(c -> Set.of(STATE_DATA_TYPE_ID, Mapper.NO_PERSISTED_STATE).contains(c))
                .orElse(true), "Unknown stateDataClassId: %s", Optional.ofNullable(messageDataDTO)
                .map(MessageDataDTO::getStateDataClassId).orElse("null"));

        final SumDiceSetStateData loadedStateData = Optional.ofNullable(messageDataDTO)
                .map(MessageDataDTO::getStateData)
                .map(sd -> Mapper.deserializeObject(sd, SumDiceSetStateData.class))
                .orElse(null);
        final Config loadedConfig = Mapper.deserializeObject(messageConfigDTO.getConfig(), Config.class);
        final State<SumDiceSetStateData> updatedState = updateState(buttonValue, loadedStateData);
        return new ConfigAndState<>(messageConfigDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageConfigDTO> createMessageConfig(@NonNull UUID configUUID, long guildId, long channelId, @NonNull Config config) {
        return Optional.of(new MessageConfigDTO(configUUID, guildId, channelId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(UUID configUUID, long guildId, long channelId, long messageId, @NonNull Config config, @NonNull State<SumDiceSetStateData> state) {
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
    State<SumDiceSetStateData> updateState(@NonNull String buttonValue, @Nullable SumDiceSetStateData stateData) {
        final List<DiceKeyAndValue> updatedList = updateDiceSet(Optional.ofNullable(stateData)
                .map(SumDiceSetStateData::getDiceSet)
                .orElse(ImmutableList.of()), buttonValue);
        return new State<>(buttonValue, new SumDiceSetStateData(updatedList));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of d4 to d20 dice";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Use `/sum_dice_set start` to get message, where the user can create a dice set and roll it.")
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build();
    }

    @Override
    protected boolean supportsResultImages() {
        return false;
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    @Override
    protected @NonNull List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(Config config, State<SumDiceSetStateData> state) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumDiceSetStateData::getDiceSet)
                        .map(List::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        List<Integer> diceResultValues = state.getData().getDiceSet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getDiceKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getDiceKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .flatMap(e -> {
                    if (MODIFIER_KEY.equals(e.getDiceKey())) {
                        return Stream.of(e.getValue());
                    }
                    int diceSides = Integer.parseInt(e.getDiceKey().substring(1));
                    return diceUtils.rollDiceOfType(Math.abs(e.getValue()), diceSides).stream()
                            .map(dv -> {
                                //modify the result if the dice count is negative
                                if (e.getValue() < 0) {
                                    return dv * -1;
                                }
                                return dv;
                            });
                }).toList();
        long sumResult = diceResultValues.stream().mapToLong(Integer::longValue).sum();
        String expression = parseDiceMapToMessageString(state.getData().getDiceSet());
        return Optional.of(RollAnswer.builder()
                .answerFormatType(config.getAnswerFormatType())
                .expression(expression)
                .result(String.valueOf(sumResult))
                .rollDetails(diceResultValues.toString())
                .build());
    }

    private int limit(int input) {
        if (input > 100) {
            return 100;
        }
        return Math.max(input, -100);
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(UUID configUUID, Config config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(UUID configUUID, Config config, State<SumDiceSetStateData> state, long guildId, long channelId) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumDiceSetStateData::getDiceSet)
                        .map(List::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        return Optional.of(MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout(configUUID))
                .build());
    }

    @VisibleForTesting
    List<DiceKeyAndValue> updateDiceSet(List<DiceKeyAndValue> currentDiceSet, String buttonValue) {
        switch (buttonValue) {
            case ROLL_BUTTON_ID -> {
                return currentDiceSet;
            }
            case CLEAR_BUTTON_ID -> {
                return ImmutableList.of();
            }
            case X2_BUTTON_ID -> {
                return currentDiceSet.stream()
                        .map(kv -> new DiceKeyAndValue(kv.getDiceKey(), limit(kv.getValue() * 2)))
                        .toList();
            }
            default -> {
                Map<String, Integer> updatedDiceSet = currentDiceSet.stream().collect(Collectors.toMap(DiceKeyAndValue::getDiceKey, DiceKeyAndValue::getValue));
                int diceModifier;
                String die;
                if (buttonValue.contains(DICE_SYMBOL)) {
                    diceModifier = "-".equals(buttonValue.substring(0, 1)) ? -1 : +1;
                    die = buttonValue.substring(2);
                } else {
                    diceModifier = Integer.parseInt(buttonValue);
                    die = MODIFIER_KEY;
                }
                int currentCount = updatedDiceSet.getOrDefault(die, 0);
                int newCount = currentCount + diceModifier;
                newCount = limit(newCount);
                if (newCount == 0) {
                    updatedDiceSet.remove(die);
                } else {
                    updatedDiceSet.put(die, newCount);
                }
                return updatedDiceSet.entrySet().stream()
                        .map(dv -> new DiceKeyAndValue(dv.getKey(), dv.getValue()))
                        .sorted(Comparator.comparing(DiceKeyAndValue::getDiceKey)) //make the list order deterministic
                        .toList();
            }
        }
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(Config config, State<SumDiceSetStateData> state) {
        List<DiceKeyAndValue> currentDiceSet = Optional.ofNullable(state.getData()).map(SumDiceSetStateData::getDiceSet).orElse(ImmutableList.of());

        if (currentDiceSet.isEmpty()) {
            return Optional.of(EMPTY_MESSAGE);
        }
        return Optional.of(parseDiceMapToMessageString(currentDiceSet));
    }


    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        Long answerTargetChannelId = DefaultCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = DefaultCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        ResultImage resultImage = DefaultCommandOptions.getResultImageOptionFromStartCommandOption(options).orElse(defaultResultImage());
        return new Config(answerTargetChannelId,
                answerType,
                resultImage
        );
    }

    private List<ComponentRowDefinition> createButtonLayout(UUID configUUID) {
        return ImmutableList.of(
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        //              ID,  label
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d4", configUUID)).label("+1d4").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d4", configUUID)).label("-1d4").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d6", configUUID)).label("+1d6").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d6", configUUID)).label("-1d6").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), X2_BUTTON_ID, configUUID)).style(ButtonDefinition.Style.SECONDARY).label("x2").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d8", configUUID)).label("+1d8").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d8", configUUID)).label("-1d8").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d10", configUUID)).label("+1d10").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d10", configUUID)).label("-1d10").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID, configUUID)).style(ButtonDefinition.Style.DANGER).label("Clear").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d12", configUUID)).label("+1d12").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d12", configUUID)).label("-1d12").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1d20", configUUID)).label("+1d20").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1d20", configUUID)).label("-1d20").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), ROLL_BUTTON_ID, configUUID)).style(ButtonDefinition.Style.SUCCESS).label("Roll").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+1", configUUID)).label("+1").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-1", configUUID)).label("-1").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+5", configUUID)).label("+5").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "-5", configUUID)).label("-5").build(),
                        ButtonDefinition.builder().id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), "+10", configUUID)).label("+10").build()
                )).build());
    }


}
