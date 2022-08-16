package de.janno.discord.bot.command.holdReroll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.CommandUtils;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.dice.DiceUtils;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
    private static final String EMPTY = "EMPTY";
    private final DiceUtils diceUtils;

    public HoldRerollCommand(MessageDataDAO messageDataDAO) {
        this(messageDataDAO, new DiceUtils());
    }

    @VisibleForTesting
    public HoldRerollCommand(MessageDataDAO messageDataDAO, DiceUtils diceUtils) {
        super(messageDataDAO);
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
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/hold_reroll start' " +
                        "to get message, where the user can roll dice")
                .build();
    }

    @Override
    public String getCommandId() {
        return COMMAND_NAME;
    }

    String createButtonCustomId(@NonNull String action, @NonNull HoldRerollConfig config, @Nullable HoldRerollStateData state) {

        Preconditions.checkArgument(!action.contains(BotConstants.LEGACY_DELIMITER_V2));

        return String.join(BotConstants.LEGACY_DELIMITER_V2,
                COMMAND_NAME,
                action,
                state == null ? EMPTY : state.getCurrentResults().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                String.valueOf(config.getSidesOfDie()),
                config.getRerollSet().isEmpty() ? EMPTY : config.getRerollSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getSuccessSet().isEmpty() ? EMPTY : config.getSuccessSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getFailureSet().isEmpty() ? EMPTY : config.getFailureSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                state == null ? "0" : String.valueOf(state.getRerollCounter()),
                Optional.ofNullable(config.getAnswerTargetChannelId()).map(Object::toString).orElse("")
        );
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
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
    protected Optional<EmbedDefinition> getAnswer(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (CLEAR_BUTTON_ID.equals(state.getButtonValue())) {
            return Optional.empty();
        }
        if (!(FINISH_BUTTON_ID.equals(state.getButtonValue()) || rollFinished(state, config))) {
            return Optional.empty();
        }
        int successes = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getData().getCurrentResults(), config.getFailureSet());
        int rerollCount = state.getData().getRerollCounter();
        String title;
        if (rerollCount == 0) {
            title = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            title = String.format("Success: %d, Failure: %d and Rerolls: %d", successes, failures, rerollCount);
        }

        return Optional.of(new EmbedDefinition(title, CommandUtils.markIn(state.getData().getCurrentResults(), getToMark(config)), ImmutableList.of()));
    }

    @Override
    protected HoldRerollConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        int sideOfDie = Integer.parseInt(customIdSplit[3]);
        Set<Integer> rerollSet = CommandUtils.toSet(customIdSplit[4], SUBSET_DELIMITER, EMPTY);
        Set<Integer> successSet = CommandUtils.toSet(customIdSplit[5], SUBSET_DELIMITER, EMPTY);
        Set<Integer> failureSet = CommandUtils.toSet(customIdSplit[6], SUBSET_DELIMITER, EMPTY);
        Long answerTargetChannelId = getOptionalLongFromArray(customIdSplit, 8);
        return new HoldRerollConfig(answerTargetChannelId, sideOfDie, rerollSet, successSet, failureSet);
    }


    @Override
    protected State<HoldRerollStateData> getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.LEGACY_CONFIG_SPLIT_DELIMITER_REGEX);
        List<Integer> currentResult = getCurrentRollResult(customIdSplit[2]);
        int rerollCount = Integer.parseInt(customIdSplit[7]);
        String buttonValue = customIdSplit[1];
        HoldRerollConfig config = getConfigFromEvent(event);

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
        return new State<>(buttonValue, new HoldRerollStateData(currentResult, rerollCount));
    }


    private List<Integer> getCurrentRollResult(String currentRollResultString) {
        if (EMPTY.equals(currentRollResultString)) {
            return ImmutableList.of();
        }
        return Arrays.stream(currentRollResultString.split(SUBSET_DELIMITER))
                .filter(NumberUtils::isParsable)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private boolean rollFinished(State<HoldRerollStateData> state, HoldRerollConfig config) {
        return state.getData().getCurrentResults().stream().noneMatch(i -> config.getRerollSet().contains(i));
    }

    @Override
    protected HoldRerollConfig getConfigFromStartOptions(CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(SIDES_OF_DIE_ID)
                .map(l -> Math.min(l, 1000))
                .orElse(6L));
        Set<Integer> rerollSet = CommandUtils.getSetFromCommandOptions(options, REROLL_SET_ID, ",");
        Set<Integer> successSet = CommandUtils.getSetFromCommandOptions(options, SUCCESS_SET_ID, ",");
        Set<Integer> failureSet = CommandUtils.getSetFromCommandOptions(options, FAILURE_SET_ID, ",");
        return new HoldRerollConfig(getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null), sideValue, rerollSet, successSet, failureSet);
    }

    @Override
    public MessageDefinition createNewButtonMessage(HoldRerollConfig config) {
        return MessageDefinition.builder()
                .content(String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                        config.getRerollSet(), config.getSuccessSet(), config.getFailureSet()))
                .componentRowDefinitions(createButtonLayout(config))
                .build();
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(HoldRerollConfig config, State<HoldRerollStateData> state) {
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
    protected Optional<ConfigAndState<HoldRerollConfig, HoldRerollStateData>> getMessageDataAndUpdateWithButtonValue(long channelId, long messageId, String buttonValue) {
        //todo
        return Optional.empty();
    }
    @VisibleForTesting
    ConfigAndState<HoldRerollConfig, HoldRerollStateData> deserializeAndUpdateState(@NonNull MessageDataDTO messageDataDTO, @NonNull String buttonValue) {
        final HoldRerollStateData loadedStateData = Optional.ofNullable(messageDataDTO.getStateData())
                .map(sd -> Mapper.deserializeObject(sd, HoldRerollStateData.class))
                .orElse(null);
        final HoldRerollConfig loadedConfig = Mapper.deserializeObject(messageDataDTO.getConfig(), HoldRerollConfig.class);
        //Todo update state
        return new ConfigAndState<>(messageDataDTO.getConfigUUID(),
                loadedConfig,
                new State<>(buttonValue, loadedStateData));
    }
    @Override
    protected MessageDataDTO createMessageDataForNewMessage(@NonNull UUID configUUID, long channelId, long messageId, @NonNull HoldRerollConfig config, @Nullable State<HoldRerollStateData> state) {
        //todo
        return null;
    }

    @Override
    public Optional<String> getCurrentMessageContentChange(HoldRerollConfig config, State<HoldRerollStateData> state) {
        if (config.getRerollSet().isEmpty()
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
                                .id(createButtonCustomId(REROLL_BUTTON_ID, config, state.getData()))
                                .label("Reroll")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(createButtonCustomId(FINISH_BUTTON_ID, config, state.getData()))
                                .label("Finish")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(createButtonCustomId(CLEAR_BUTTON_ID, config, state.getData()))
                                .label("Clear")
                                .build())
                        .build()
        );
    }

    private List<ComponentRowDefinition> createButtonLayout(HoldRerollConfig config) {
        List<ButtonDefinition> buttons = IntStream.range(1, 16)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(createButtonCustomId(String.valueOf(i), config, null))
                        .label(String.format("%d%s%s", i, DICE_SYMBOL, config.getSidesOfDie()))
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
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
