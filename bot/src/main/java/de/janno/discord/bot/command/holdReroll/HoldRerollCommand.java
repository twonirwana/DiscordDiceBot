package de.janno.discord.bot.command.holdReroll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.*;
import de.janno.discord.bot.dice.DiceUtils;
import de.janno.discord.bot.persistance.Mapper;
import de.janno.discord.bot.persistance.PersistanceManager;
import de.janno.discord.bot.persistance.MessageDataDTO;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.message.MessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class HoldRerollCommand extends AbstractCommand<HoldRerollConfig, HoldRerollStateData> {

    static final String SUBSET_DELIMITER = ";";
    private static final String COMMAND_NAME = "hold_reroll";
    private static final String REROLL_BUTTON_ID = "reroll";
    private static final String FINISH_BUTTON_ID = "finish";
    private static final String SIDES_OF_DIE_ID = "sides";
    private static final String REROLL_SET_ID = "reroll_set";
    private static final String SUCCESS_SET_ID = "success_set";
    private static final String FAILURE_SET_ID = "failure_set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String DICE_SYMBOL = "d";
    private static final String CONFIG_TYPE_ID = "HoldRerollConfig";
    private static final String STATE_DATA_TYPE_ID = "HoldRerollStateData";
    private final DiceUtils diceUtils;

    public HoldRerollCommand(PersistanceManager persistanceManager) {
        this(persistanceManager, new DiceUtils());
    }

    @VisibleForTesting
    public HoldRerollCommand(PersistanceManager persistanceManager, DiceUtils diceUtils) {
        super(persistanceManager);
        this.diceUtils = diceUtils;
    }


    private Set<Integer> getToMark(HoldRerollConfig config) {
        return IntStream.range(1, config.getSidesOfDie() + 1)
                .filter(i -> !config.getRerollSet().contains(i))
                .boxed()
                .collect(Collectors.toSet());
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Roll dice and with a option to reroll";
    }

    @Override
    protected @NonNull EmbedOrMessageDefinition getHelpMessage() {
        return EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Use '/hold_reroll start' to get message, where the user can roll dice")
                .field(new EmbedOrMessageDefinition.Field("Example", "`/hold_reroll start sides:6 reroll_set:2,3,4 success_set:5,6 failure_set:1`", false))
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
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(SIDES_OF_DIE_ID)
                        .required(true)
                        .description("Dice side")
                        .type(CommandDefinitionOption.Type.INTEGER)
                        .minValue(2L)
                        .maxValue(1000L).build(),
                CommandDefinitionOption.builder()
                        .name(REROLL_SET_ID)
                        .required(false)
                        .description("Dice numbers to reroll, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(SUCCESS_SET_ID)
                        .required(false)
                        .description("Success dice numbers, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build(),
                CommandDefinitionOption.builder()
                        .name(FAILURE_SET_ID)
                        .required(false)
                        .description("Failure dice numbers, seperated by ','")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected boolean supportsResultImages() {
        return false;
    }

    @Override
    protected @NonNull Optional<RollAnswer> getAnswer(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.empty();
        }
        if (state.getData() == null) {
            return Optional.empty();
        }
        if (!(FINISH_BUTTON_ID.equals(state.getButtonValue()) || rollFinished(state, config))) {
            return Optional.empty();
        }
        int successes = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getFailureSet());
        int rerollCount = state.getData().getRerollCounter();
        String result;
        if (rerollCount == 0) {
            result = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            result = String.format("Success: %d, Failure: %d and Rerolls: %d", successes, failures, rerollCount);
        }
        return Optional.of(RollAnswer.builder()
                .answerFormatType(config.getAnswerFormatType())
                .expression("%dd%d".formatted(state.getData().getCurrentResults().size(), config.getSidesOfDie()))
                .result(result)
                .rollDetails(CommandUtils.markIn(state.getData().getCurrentResults(), getToMark(config)))
                .build());
    }

    private HoldRerollStateData updateStateWithButtonValue(@NonNull String buttonValue,
                                                           @NonNull HoldRerollConfig config,
                                                           @NonNull List<Integer> currentResult,
                                                           int rerollCount) {
        if (REROLL_BUTTON_ID.equals(buttonValue)) {
            currentResult = currentResult.stream()
                    .map(i -> {
                        if (config.getRerollSet().contains(i)) {
                            return diceUtils.rollDice(config.getSidesOfDie());
                        }
                        return i;
                    }).collect(Collectors.toList());
            rerollCount++;
        } else if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            currentResult = ImmutableList.of();
            rerollCount = 0;
        } else if (NumberUtils.isParsable(buttonValue)) {
            int numberOfDice = Integer.parseInt(buttonValue);
            currentResult = diceUtils.rollDiceOfType(numberOfDice, config.getSidesOfDie());
            rerollCount = 0;
        }
        return new HoldRerollStateData(currentResult, rerollCount);
    }

    private boolean rollFinished(State<HoldRerollStateData> state, HoldRerollConfig config) {
        if (state.getData() == null) {
            return false;
        }
        return state.getData().getCurrentResults().stream().noneMatch(i -> config.getRerollSet().contains(i));
    }

    @Override
    protected @NonNull HoldRerollConfig getConfigFromStartOptions(@NonNull CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(SIDES_OF_DIE_ID)
                .map(l -> Math.min(l, 1000))
                .orElse(6L));
        Set<Integer> rerollSet = CommandUtils.getSetFromCommandOptions(options, REROLL_SET_ID, ",");
        Set<Integer> successSet = CommandUtils.getSetFromCommandOptions(options, SUCCESS_SET_ID, ",");
        Set<Integer> failureSet = CommandUtils.getSetFromCommandOptions(options, FAILURE_SET_ID, ",");
        Long answerTargetChannelId = DefaultCommandOptions.getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null);
        AnswerFormatType answerType = DefaultCommandOptions.getAnswerTypeFromStartCommandOption(options).orElse(defaultAnswerFormat());
        ResultImage resultImage = DefaultCommandOptions.getResultImageOptionFromStartCommandOption(options).orElse(defaultResultImage());

        return new HoldRerollConfig(answerTargetChannelId,
                sideValue,
                rerollSet,
                successSet,
                failureSet,
                answerType,
                resultImage);
    }

    @Override
    public @NonNull MessageDefinition createNewButtonMessage(HoldRerollConfig config) {
        return MessageDefinition.builder()
                .content(String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                        config.getRerollSet(), config.getSuccessSet(), config.getFailureSet()))
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    @Override
    protected @NonNull Optional<MessageDefinition> createNewButtonMessageWithState(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getButtonValue())
                || FINISH_BUTTON_ID.equals(state.getButtonValue())
                || rollFinished(state, config)) {
            return Optional.of(MessageDefinition.builder()
                    .content(String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                            config.getRerollSet(), config.getSuccessSet(), config.getFailureSet()))
                    .componentRowDefinitions(getButtonLayoutWithState(state, config))
                    .build());
        }

        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getButtonValue())
                || FINISH_BUTTON_ID.equals(state.getButtonValue())
                || rollFinished(state, config)) {
            return Optional.empty();
        }

        return Optional.of(getButtonLayoutWithState(state, config));
    }

    @Override
    protected Optional<ConfigAndState<HoldRerollConfig, HoldRerollStateData>> getMessageDataAndUpdateWithButtonValue(long channelId,
                                                                                                                     long messageId,
                                                                                                                     @NonNull String buttonValue,
                                                                                                                     @NonNull String invokingUserName) {
        final Optional<MessageDataDTO> messageDataDTO = persistanceManager.getDataForMessage(channelId, messageId);
        return messageDataDTO.map(dataDTO -> deserializeAndUpdateState(dataDTO, buttonValue));
    }

    @Override
    protected void updateCurrentMessageStateData(long channelId, long messageId, @NonNull HoldRerollConfig config, @NonNull State<HoldRerollStateData> state) {
        if (state.getData() == null || (FINISH_BUTTON_ID.equals(state.getButtonValue()) || rollFinished(state, config))) {
            persistanceManager.updateCommandConfigOfMessage(channelId, messageId, Mapper.NO_PERSISTED_STATE, null);
        } else {
            persistanceManager.updateCommandConfigOfMessage(channelId, messageId, STATE_DATA_TYPE_ID, Mapper.serializedObject(state.getData()));
        }
    }

    @VisibleForTesting
    ConfigAndState<HoldRerollConfig, HoldRerollStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        Preconditions.checkArgument(CONFIG_TYPE_ID.equals(messageDataDTO.getConfigClassId()), "Unknown configClassId: %s", messageDataDTO.getConfigClassId());
        Preconditions.checkArgument(STATE_DATA_TYPE_ID.equals(messageDataDTO.getStateDataClassId())
                || Mapper.NO_PERSISTED_STATE.equals(messageDataDTO.getStateDataClassId()), "Unknown stateDataClassId: %s", messageDataDTO.getStateDataClassId());

        final HoldRerollStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, HoldRerollStateData.class))
                .orElse(null);
        final HoldRerollConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), HoldRerollConfig.class);
        final HoldRerollStateData updatedState = updateStateWithButtonValue(buttonValue,
                loadedConfig,
                Optional.ofNullable(loadedStateData).map(HoldRerollStateData::getCurrentResults).orElse(ImmutableList.of()),
                Optional.ofNullable(loadedStateData).map(HoldRerollStateData::getRerollCounter).orElse(0)
        );
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                loadedConfig,
                new State<>(buttonValue, updatedState));
    }

    @Override
    public Optional<MessageDataDTO> createMessageDataForNewMessage(@NonNull UUID configUUID,
                                                                   long guildId,
                                                                   long channelId,
                                                                   long messageId,
                                                                   @NonNull HoldRerollConfig config,
                                                                   @Nullable State<HoldRerollStateData> state) {
        return Optional.of(new MessageDataDTO(configUUID, guildId, channelId, messageId, getCommandId(), CONFIG_TYPE_ID, Mapper.serializedObject(config)));
    }

    @Override
    public @NonNull Optional<String> getCurrentMessageContentChange(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (state.getData() == null ||
                config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getButtonValue())
                || FINISH_BUTTON_ID.equals(state.getButtonValue())
                || rollFinished(state, config)) {
            return Optional.empty();
        }
        int successes = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getFailureSet());
        return Optional.of(String.format("%s = %d successes and %d failures",
                CommandUtils.markIn(state.getData().getCurrentResults(), getToMark(config)), successes, failures));
    }


    private List<ComponentRowDefinition> getButtonLayoutWithState(State<HoldRerollStateData> state, HoldRerollConfig config) {
        if (CLEAR_BUTTON_ID.equals(state.getButtonValue()) ||
                FINISH_BUTTON_ID.equals(state.getButtonValue()) ||
                rollFinished(state, config)) {
            return createButtonLayout(config);
        }
        //further rerolls are possible

        return ImmutableList.of(
                ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), REROLL_BUTTON_ID))
                                .label("Reroll")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), FINISH_BUTTON_ID))
                                .label("Finish")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), CLEAR_BUTTON_ID))
                                .label("Clear")
                                .build())
                        .build()
        );
    }

    private List<ComponentRowDefinition> createButtonLayout(HoldRerollConfig config) {
        List<ButtonDefinition> buttons = IntStream.range(1, 16)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(BottomCustomIdUtils.createButtonCustomId(getCommandId(), String.valueOf(i)))
                        .label(String.format("%d%s%s", i, DICE_SYMBOL, config.getSidesOfDie()))
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected @NonNull Optional<String> getStartOptionsValidationMessage(@NonNull CommandInteractionOption options) {
        HoldRerollConfig conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    Optional<String> validate(HoldRerollConfig config) {

        if (config.getRerollSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return Optional.of(String.format("reroll set %s contains a number bigger then the sides of the die %s", config.getRerollSet(), config.getSidesOfDie()));
        }
        if (config.getSuccessSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return Optional.of(String.format("success set %s contains a number bigger then the sides of the die %s", config.getSuccessSet(), config.getSidesOfDie()));
        }
        if (config.getFailureSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return Optional.of(String.format("failure set %s contains a number bigger then the sides of the die %s", config.getFailureSet(), config.getSidesOfDie()));
        }
        if (config.getRerollSet().size() >= config.getSidesOfDie()) {
            return Optional.of("The reroll must not contain all numbers");
        }
        return Optional.empty();
    }

}
