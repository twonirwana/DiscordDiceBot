package de.janno.discord.bot.command.holdReroll;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.command.AbstractCommand;
import de.janno.discord.bot.command.CommandUtils;
import de.janno.discord.bot.dice.DiceUtils;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class HoldRerollCommand extends AbstractCommand<HoldRerollConfig, HoldRerollState> {

    private static final String COMMAND_NAME = "hold_reroll";
    private static final String REROLL_BUTTON_ID = "reroll";
    private static final String FINISH_BUTTON_ID = "finish";
    private static final String SIDES_OF_DIE_ID = "sides";
    private static final String REROLL_SET_ID = "reroll_set";
    private static final String SUCCESS_SET_ID = "success_set";
    private static final String FAILURE_SET_ID = "failure_set";
    private static final String CLEAR_BUTTON_ID = "clear";
    static final String SUBSET_DELIMITER = ";";
    private static final String DICE_SYMBOL = "d";
    private static final String EMPTY = "EMPTY";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);

    private final DiceUtils diceUtils;

    public HoldRerollCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public HoldRerollCommand(DiceUtils diceUtils) {
        super(BUTTON_MESSAGE_CACHE);
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
    public String getName() {
        return COMMAND_NAME;
    }

    String createButtonCustomId(@NonNull String action, @NonNull HoldRerollConfig config, @Nullable HoldRerollState state) {

        Preconditions.checkArgument(!action.contains(BotConstants.CONFIG_DELIMITER));

        return String.join(BotConstants.CONFIG_DELIMITER,
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
                        .build(),
                ANSWER_TARGET_CHANNEL_COMMAND_OPTION
        );
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(HoldRerollState state, HoldRerollConfig config) {
        if (CLEAR_BUTTON_ID.equals(state.getState())) {
            return Optional.empty();
        }
        if (!(FINISH_BUTTON_ID.equals(state.getState()) || rollFinished(state, config))) {
            return Optional.empty();
        }
        int successes = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getFailureSet());
        int rerollCount = state.getRerollCounter();
        String title;
        if (rerollCount == 0) {
            title = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            title = String.format("Success: %d, Failure: %d and Rerolls: %d", successes, failures, rerollCount);
        }

        return Optional.of(new EmbedDefinition(title, CommandUtils.markIn(state.getCurrentResults(), getToMark(config)), ImmutableList.of()));
    }

    @Override
    protected HoldRerollConfig getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
        int sideOfDie = Integer.parseInt(customIdSplit[3]);
        Set<Integer> rerollSet = CommandUtils.toSet(customIdSplit[4], SUBSET_DELIMITER, EMPTY);
        Set<Integer> successSet = CommandUtils.toSet(customIdSplit[5], SUBSET_DELIMITER, EMPTY);
        Set<Integer> failureSet = CommandUtils.toSet(customIdSplit[6], SUBSET_DELIMITER, EMPTY);
        Long answerTargetChannelId = getOptionalLongFromArray(customIdSplit, 8);
        return new HoldRerollConfig(sideOfDie, rerollSet, successSet, failureSet, answerTargetChannelId);
    }


    @Override
    protected HoldRerollState getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);
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
        return new HoldRerollState(buttonValue, currentResult, rerollCount);
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

    private boolean rollFinished(HoldRerollState state, HoldRerollConfig config) {
        return state.getCurrentResults().stream().noneMatch(i -> config.getRerollSet().contains(i));
    }

    @Override
    protected HoldRerollConfig getConfigFromStartOptions(CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(SIDES_OF_DIE_ID)
                .map(l -> Math.min(l, 1000))
                .orElse(6L));
        Set<Integer> rerollSet = CommandUtils.getSetFromCommandOptions(options, REROLL_SET_ID, ",");
        Set<Integer> successSet = CommandUtils.getSetFromCommandOptions(options, SUCCESS_SET_ID, ",");
        Set<Integer> failureSet = CommandUtils.getSetFromCommandOptions(options, FAILURE_SET_ID, ",");
        return new HoldRerollConfig(sideValue, rerollSet, successSet, failureSet, getAnswerTargetChannelIdFromStartCommandOption(options).orElse(null));
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
    protected Optional<MessageDefinition> createNewButtonMessageWithState(HoldRerollState state, HoldRerollConfig config) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getState())
                || FINISH_BUTTON_ID.equals(state.getState())
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
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(HoldRerollState state, HoldRerollConfig config) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getState())
                || FINISH_BUTTON_ID.equals(state.getState())
                || rollFinished(state, config)) {
            return Optional.empty();
        }

        return Optional.of(getButtonLayoutWithState(state, config));
    }

    @Override
    public Optional<String> getCurrentMessageContentChange(HoldRerollState state, HoldRerollConfig config) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getState())
                || FINISH_BUTTON_ID.equals(state.getState())
                || rollFinished(state, config)) {
            return Optional.empty();
        }
        int successes = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getFailureSet());
        return Optional.of(String.format("%s = %d successes and %d failures",
                CommandUtils.markIn(state.getCurrentResults(), getToMark(config)), successes, failures));
    }


    private List<ComponentRowDefinition> getButtonLayoutWithState(HoldRerollState state, HoldRerollConfig config) {
        if (CLEAR_BUTTON_ID.equals(state.getState()) ||
                FINISH_BUTTON_ID.equals(state.getState()) ||
                rollFinished(state, config)) {
            return createButtonLayout(config);
        }
        //further rerolls are possible

        return ImmutableList.of(
                ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(createButtonCustomId(REROLL_BUTTON_ID, config, state))
                                .label("Reroll")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(createButtonCustomId(FINISH_BUTTON_ID, config, state))
                                .label("Finish")

                                .build())
                        .buttonDefinition(ButtonDefinition.builder()
                                .id(createButtonCustomId(CLEAR_BUTTON_ID, config, state))
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

    @Override
    protected Optional<Long> getAnswerTargetChannelId(HoldRerollConfig config) {
        return Optional.ofNullable(config.getAnswerTargetChannelId());
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
