package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.api.*;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.command.slash.CommandDefinitionOption;
import de.janno.discord.command.slash.CommandInteractionOption;
import de.janno.discord.dice.DiceUtils;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.janno.discord.command.CommandUtils.*;

@Slf4j
public class HoldRerollCommand extends AbstractCommand<HoldRerollCommand.Config, HoldRerollCommand.State> {

    private static final String COMMAND_NAME = "hold_reroll";
    private static final String REROLL_BUTTON_ID = "reroll";
    private static final String FINISH_BUTTON_ID = "finish";
    private static final String SIDES_OF_DIE_ID = "sides";
    private static final String REROLL_SET_ID = "reroll_set";
    private static final String SUCCESS_SET_ID = "success_set";
    private static final String FAILURE_SET_ID = "failure_set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String SUBSET_DELIMITER = ";";
    private static final String DICE_SYMBOL = "d";
    private static final String EMPTY = "EMPTY";

    private final DiceUtils diceUtils;

    public HoldRerollCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public HoldRerollCommand(DiceUtils diceUtils) {
        super(new ButtonMessageCache(COMMAND_NAME));
        this.diceUtils = diceUtils;
    }


    private Set<Integer> getToMark(Config config) {
        return IntStream.range(1, config.getSidesOfDie() + 1)
                .filter(i -> !config.getRerollSet().contains(i))
                .boxed()
                .collect(Collectors.toSet());
    }

    @Override
    protected String getCommandDescription() {
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

    String createButtonCustomId(@NonNull String action, @NonNull Config config, @Nullable State state) {

        Preconditions.checkArgument(!action.contains(CONFIG_DELIMITER));

        return String.join(CONFIG_DELIMITER,
                COMMAND_NAME,
                action,
                state == null ? EMPTY : state.getCurrentResults().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                String.valueOf(config.getSidesOfDie()),
                config.getRerollSet().isEmpty() ? EMPTY : config.getRerollSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getSuccessSet().isEmpty() ? EMPTY : config.getSuccessSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getFailureSet().isEmpty() ? EMPTY : config.getFailureSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                state == null ? "0" : String.valueOf(state.getRerollCounter())
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
    protected Answer getAnswer(State state, Config config) {

        int successes = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getFailureSet());
        int rerollCount = state.getRerollCounter();
        String title;
        if (rerollCount == 0) {
            title = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            title = String.format("Success: %d, Failure: %d and Rerolls: %d", successes, failures, rerollCount);
        }

        return new Answer(title, markIn(state.getCurrentResults(), getToMark(config)), ImmutableList.of());
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(CONFIG_DELIMITER);
        int sideOfDie = Integer.parseInt(customIdSplit[3]);
        Set<Integer> rerollSet = toSet(customIdSplit[4], SUBSET_DELIMITER, EMPTY);
        Set<Integer> successSet = toSet(customIdSplit[5], SUBSET_DELIMITER, EMPTY);
        Set<Integer> failureSet = toSet(customIdSplit[6], SUBSET_DELIMITER, EMPTY);
        return new Config(sideOfDie, rerollSet, successSet, failureSet);
    }


    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(CONFIG_DELIMITER);
        List<Integer> currentResult = getCurrentRollResult(customIdSplit[2]);
        int rerollCount = Integer.parseInt(customIdSplit[7]);
        String buttonValue = customIdSplit[1];
        Config config = getConfigFromEvent(event);

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
        return new State(buttonValue, currentResult, rerollCount);
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

    private boolean rollFinished(State state, Config config) {
        return state.getCurrentResults().stream().noneMatch(i -> config.getRerollSet().contains(i));
    }

    @Override
    protected boolean createAnswerMessage(State state, Config config) {
        if (CLEAR_BUTTON_ID.equals(state.getState())) {
            return false;
        }
        return FINISH_BUTTON_ID.equals(state.getState()) || rollFinished(state, config);
    }


    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getLongSubOptionWithName(SIDES_OF_DIE_ID)
                .map(l -> Math.min(l, 1000))
                .orElse(6L));
        Set<Integer> rerollSet = getSetFromCommandOptions(options, REROLL_SET_ID, ",");
        Set<Integer> successSet = getSetFromCommandOptions(options, SUCCESS_SET_ID, ",");
        Set<Integer> failureSet = getSetFromCommandOptions(options, FAILURE_SET_ID, ",");
        return new Config(sideValue, rerollSet, successSet, failureSet);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected String getButtonMessage(Config config) {
        return String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                config.getRerollSet(), config.getSuccessSet(), config.getFailureSet());
    }

    @Override
    protected String getButtonMessageWithState(State state, Config config) {
        if (config.getRerollSet().isEmpty()
                || CLEAR_BUTTON_ID.equals(state.getState())
                || FINISH_BUTTON_ID.equals(state.getState())
                || rollFinished(state, config)) {
            return String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                    config.getRerollSet(), config.getSuccessSet(), config.getFailureSet());
        }

        int successes = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(state.getCurrentResults(), config.getFailureSet());
        return String.format("%s = %d successes and %d failures", markIn(state.getCurrentResults(), getToMark(config)), successes, failures);
    }

    @Override
    protected List<ComponentRowDefinition> getButtonLayoutWithState(State state, Config config) {
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

    @Override
    protected List<ComponentRowDefinition> getButtonLayout(Config config) {
        return createButtonLayout(config);
    }

    private List<ComponentRowDefinition> createButtonLayout(Config config) {
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
    protected String getStartOptionsValidationMessage(CommandInteractionOption options) {
        Config conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    String validate(Config config) {

        if (config.getRerollSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return String.format("reroll set %s contains a number bigger then the sides of the die %s", config.getRerollSet(), config.getSidesOfDie());
        }
        if (config.getSuccessSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return String.format("success set %s contains a number bigger then the sides of the die %s", config.getSuccessSet(), config.getSidesOfDie());
        }
        if (config.getFailureSet().stream().anyMatch(i -> i > config.getSidesOfDie())) {
            return String.format("failure set %s contains a number bigger then the sides of the die %s", config.getFailureSet(), config.getSidesOfDie());
        }
        if (config.getRerollSet().size() >= config.getSidesOfDie()) {
            return "The reroll must not contain all numbers";
        }
        return null;
    }

    @Value
    protected static class Config implements IConfig {
        int sidesOfDie;
        @NonNull
        Set<Integer> rerollSet;
        @NonNull
        Set<Integer> successSet;
        @NonNull
        Set<Integer> failureSet;

        @Override
        public String toShortString() {
            return ImmutableList.of(
                    String.valueOf(sidesOfDie),
                    rerollSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    successSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    failureSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER))
            ).toString();
        }
    }

    @Value
    static class State implements IState {
        @NonNull
        String state; //last action number of the dice, reroll, finish and clear
        @NonNull
        List<Integer> currentResults;
        int rerollCounter;

        @Override
        public String toShortString() {
            return String.format("[%s, %s, %d]", state, currentResults, rerollCounter);
        }
    }
}
