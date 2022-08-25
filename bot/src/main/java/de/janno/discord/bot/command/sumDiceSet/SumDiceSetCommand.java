package de.janno.discord.bot.command.sumDiceSet;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;
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

    public SumDiceSetCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceUtils());
    }

    @VisibleForTesting
    public SumDiceSetCommand(MessageDataDAO messageDataDAO, DiceUtils diceUtils) {
        super(messageDataDAO);
        this.diceUtils = diceUtils;
    }

    private static String parseDiceMapToMessageString(Map<String, Integer> diceSet) {
        String message = diceSet.entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .map(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
                        return String.format("%s%d", e.getValue() > 0 ? "+" : "", e.getValue());
                    }
                    return String.format("%s%d%s", e.getValue() > 0 ? "+" : "", e.getValue(), e.getKey());
                })
                .collect(Collectors.joining(DICE_SET_DELIMITER));
        //remove leading +
        if (message.startsWith("+")) {
            message = message.substring(1);
        }
        return message;
    }


    @Override
    protected Optional<ConfigAndState<Config, SumDiceSetStateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                           long messageId,
                                                                                                           @NonNull String buttonValue,
                                                                                                           @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = messageDataDAO.getDataForMessage(channelId, messageId);
        if (messageDataDTO.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(deserializeAndUpdateState(messageDataDTO.get(), buttonValue));
    }

    @VisibleForTesting
    ConfigAndState<Config, SumDiceSetStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        Preconditions.checkArgument(STATE_DATA_TYPE_ID.equals(messageDataDTO.getStateDataClassId())
                || Mapper.NO_PERSISTED_STATE.equals(messageDataDTO.getStateDataClassId()), "Unknown stateDataClassId: %s", messageDataDTO.getStateDataClassId());

        final SumDiceSetStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, SumDiceSetStateData.class))
                .orElse(null);
        final Config loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), Config.class);
        final State<SumDiceSetStateData> updatedState = updateState(buttonValue, loadedStateData);
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(), loadedConfig, updatedState);
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                      long channelId,
                                                                      long messageId,
                                                                      @NonNull Config config,
                                                                      @Nullable State<SumDiceSetStateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, channelId, messageId, getCommandId(),
                CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull Config config, @NonNull State<SumDiceSetStateData> state) {
        if (state.getData() == null) {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, Mapper.NO_PERSISTED_STATE, null);
        } else {
            messageDataDAO.updateCommandConfigOfMessage(channelId, messageId, STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData()));
        }
    }

    @VisibleForTesting
    State<SumDiceSetStateData> updateState(@NonNull String buttonValue, @Nullable SumDiceSetStateData stateData) {
        if (stateData == null) {
            return new State<>(buttonValue, new SumDiceSetStateData(ImmutableMap.of()));
        }
        return new State<>(buttonValue, new SumDiceSetStateData(updateDiceSet(stateData.getDiceSetMap(), buttonValue)));
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of d4 to d20 dice";
    }

    @Override
    protected @NonNull EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/sum_dice_set start' " +
                        "to get message, where the user can create a dice set and roll it.")
                .build();
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
    protected @NonNull Optional<EmbedDefinition> getAnswer(Config config, State<SumDiceSetStateData> state) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumDiceSetStateData::getDiceSetMap)
                        .map(Map::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        List<Integer> diceResultValues = state.getData().getDiceSetMap().entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .flatMap(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
                        return Stream.of(e.getValue());
                    }
                    int diceSides = Integer.parseInt(e.getKey().substring(1));
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
        String title = parseDiceMapToMessageString(state.getData().getDiceSetMap());
        return Optional.of(new EmbedDefinition(String.format("%s = %d", title, sumResult), diceResultValues.toString(), ImmutableList.of()));
    }

    private int limit(int input) {
        if (input > 100) {
            return 100;
        }
        if (input < -100) {
            return -100;
        }
        return input;
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout())
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(Config config, State<SumDiceSetStateData> state) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) &&
                !Optional.ofNullable(state.getData())
                        .map(SumDiceSetStateData::getDiceSetMap)
                        .map(Map::isEmpty)
                        .orElse(true))) {
            return Optional.empty();
        }
        return Optional.of(MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout())
                .build());
    }

    @VisibleForTesting
    Map<String, Integer> updateDiceSet(Map<String, Integer> currentDiceSet, String buttonValue) {
        switch (buttonValue) {
            case ROLL_BUTTON_ID:
                return currentDiceSet;
            case CLEAR_BUTTON_ID:
                return ImmutableMap.of();
            case X2_BUTTON_ID:
                return currentDiceSet.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> limit(e.getValue() * 2)));
            default:
                Map<String, Integer> updatedDiceSet = new HashMap<>(currentDiceSet);
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
                return updatedDiceSet;
        }
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(Config config, State<SumDiceSetStateData> state) {
        Map<String, Integer> currentDiceSet = Optional.ofNullable(state.getData()).map(SumDiceSetStateData::getDiceSetMap).orElse(ImmutableMap.of());

        if (currentDiceSet.isEmpty()) {
            return Optional.of(EMPTY_MESSAGE);
        }
        return Optional.of(parseDiceMapToMessageString(currentDiceSet));
    }

    @Override
    protected @NonNull Config getConfigFromEvent(@NonNull ButtonEventAdaptor event) {
        String[] split = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);

        return new Config(getOptionalLongFromArray(split, 2));
    }

    @Override
    protected @NonNull State<SumDiceSetStateData> getStateFromEvent(@NonNull ButtonEventAdaptor event) {
        String buttonMessage = event.getMessageContent();
        String buttonValue = getButtonValueFromLegacyCustomId(event.getCustomId());
        if (EMPTY_MESSAGE.equals(buttonMessage)) {
            return new State<>(buttonValue, new SumDiceSetStateData(ImmutableMap.of()));
        }

        return new State<>(buttonValue, new SumDiceSetStateData(Arrays.stream(buttonMessage.split(Pattern.quote(DICE_SET_DELIMITER)))
                //for handling legacy buttons with '1d4 + 1d6)
                .filter(s -> !"+".equals(s))
                .filter(Objects::nonNull)
                //adding the + for the first die type in the message
                .map(s -> {
                    if (!s.startsWith("-") && !s.startsWith("+")) {
                        return "+" + s;
                    }
                    return s;
                })
                .collect(Collectors.toMap(s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return s.substring(s.indexOf(DICE_SYMBOL));
                    } else {
                        return MODIFIER_KEY;
                    }
                }, s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return Integer.valueOf(s.substring(0, s.indexOf(DICE_SYMBOL)));
                    } else {
                        s = s.replace("+", "");
                        if (NumberUtils.isParsable(s)) {
                            return Integer.valueOf(s);
                        } else {
                            log.error(String.format("Can't parse number %s for buttonId: %s and buttonMessage: %s", s, event.getCustomId(), buttonMessage));
                            return 0;
                        }
                    }
                }))));
    }

    @Override
    protected @NonNull Config getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        return new Config(getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));
    }

    private List<ComponentRowDefinition> createButtonLayout() {
        return ImmutableList.of(
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        //              ID,  label
                        ButtonDefinition.builder().id(createButtonCustomId("+1d4")).label("+1d4").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d4")).label("-1d4").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d6")).label("+1d6").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d6")).label("-1d6").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(X2_BUTTON_ID)).style(ButtonDefinition.Style.SECONDARY).label("x2").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1d8")).label("+1d8").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d8")).label("-1d8").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d10")).label("+1d10").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d10")).label("-1d10").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(CLEAR_BUTTON_ID)).style(ButtonDefinition.Style.DANGER).label("Clear").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1d12")).label("+1d12").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d12")).label("-1d12").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d20")).label("+1d20").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d20")).label("-1d20").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(ROLL_BUTTON_ID)).style(ButtonDefinition.Style.SUCCESS).label("Roll").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1")).label("+1").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1")).label("-1").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+5")).label("+5").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-5")).label("-5").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+10")).label("+10").build()
                )).build());
    }


}
