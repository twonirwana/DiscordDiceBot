package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.janno.discord.dice.DiceUtils.makeBold;

@Slf4j
public class HoldRerollCommand extends AbstractCommand {

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
    private static final int CURRENT_ROLL_CONFIG_INDEX = 0;
    private static final int SIDES_OF_DIE_CONFIG_INDEX = 1;
    private static final int REROLL_SET_CONFIG_INDEX = 2;
    private static final int SUCCESS_SET_CONFIG_INDEX = 3;
    private static final int FAILURE_SET_CONFIG_INDEX = 4;
    private static final int REROLL_COUNT_CONFIG_INDEX = 5;
    private static final String EMPTY = "EMPTY";

    private final DiceUtils diceUtils;

    public HoldRerollCommand() {
        this(new DiceUtils());
    }


    //for testing
    public HoldRerollCommand(DiceUtils diceUtils) {
        //current roll and rerollCount should not be part of the hash
        super(new ActiveButtonsCache(COMMAND_NAME, c -> c.subList(1, 5).hashCode()));
        this.diceUtils = diceUtils;
    }

    private static String markIn(List<Integer> diceResults, Set<Integer> toMark) {
        return "[" + diceResults.stream().map(i -> {
            if (toMark.contains(i)) {
                return makeBold(i);
            }
            return i + "";
        }).collect(Collectors.joining(",")) + "]";
    }

    private Set<Integer> getToMark(List<String> config) {
        int sidesOfDie = getSidesOfDie(config);
        Set<Integer> rerollSet = getRerollSet(config);
        return IntStream.range(1, sidesOfDie + 1)
                .filter(i -> !rerollSet.contains(i))
                .boxed()
                .collect(Collectors.toSet());
    }

    @Override
    protected String getCommandDescription() {
        return "Roll the dice and with option to reroll";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Use '/hold_reroll start' " +
                        "to get message, where the user can roll dice")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of(
                ApplicationCommandOptionData.builder()
                        .name(SIDES_OF_DIE_ID)
                        .required(true)
                        .description("Dice side")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(0d)
                        .maxValue(1000d).build(),
                ApplicationCommandOptionData.builder()
                        .name(REROLL_SET_ID)
                        .required(true)
                        .description("Dice numbers to reroll, seperated by ','")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(SUCCESS_SET_ID)
                        .required(true)
                        .description("Success dice numbers, seperated by ','")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(FAILURE_SET_ID)
                        .required(true)
                        .description("Failure dice numbers, seperated by ','")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build()
        );
    }

    @Override
    protected List<DiceResult> getDiceResult(String buttonValue, List<String> config) {
        List<Integer> rollResult = getCurrentRollResult(config);
        Set<Integer> successSet = getSuccessSet(config);
        Set<Integer> failureSet = getFailureSet(config);
        int successes = DiceUtils.numberOfDiceResultsEqual(rollResult, successSet);
        int failures = DiceUtils.numberOfDiceResultsEqual(rollResult, failureSet);
        String rerollCount = config.get(REROLL_COUNT_CONFIG_INDEX);
        String titel;
        if ("0".equals(rerollCount)) {
            titel = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            titel = String.format("Success: %d, Failure: %d and Rerolls: %s", successes, failures, rerollCount);
        }

        DiceResult diceResult = new DiceResult(titel, markIn(rollResult, getToMark(config)));
        return ImmutableList.of(diceResult);
    }

    @Override
    protected List<String> getConfigFromEvent(ComponentInteractionEvent event) {
        List<String> config = new ArrayList<>(super.getConfigFromEvent(event));
        String buttonValue = getButtonValueFromEvent(event);
        Set<Integer> rerollSet = getRerollSet(config);
        int sideOfDie = getSidesOfDie(config);
        if (REROLL_BUTTON_ID.equals(buttonValue)) {
            List<Integer> currentResult = getCurrentRollResult(config);
            List<Integer> rerollResult = currentResult.stream()
                    .map(i -> {
                        if (rerollSet.contains(i)) {
                            return diceUtils.rollDice(sideOfDie);
                        }
                        return i;
                    }).collect(Collectors.toList());
            config.set(CURRENT_ROLL_CONFIG_INDEX, getConfigStringFromResultList(rerollResult));
            int rerollCount = Integer.parseInt(config.get(REROLL_COUNT_CONFIG_INDEX));
            rerollCount++;
            config.set(REROLL_COUNT_CONFIG_INDEX, String.valueOf(rerollCount));
        } else if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            config.set(CURRENT_ROLL_CONFIG_INDEX, EMPTY);
            config.set(REROLL_COUNT_CONFIG_INDEX, "0");
        } else if (StringUtils.isNumeric(buttonValue)) {
            int numberOfDice = Integer.parseInt(buttonValue);
            List<Integer> rollResult = diceUtils.rollDiceOfType(numberOfDice, sideOfDie);
            config.set(CURRENT_ROLL_CONFIG_INDEX, getConfigStringFromResultList(rollResult));
            config.set(REROLL_COUNT_CONFIG_INDEX, "0");
        }
        return ImmutableList.copyOf(config);
    }

    private String getConfigStringFromResultList(List<Integer> result) {
        return result.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(SUBSET_DELIMITER));
    }

    private List<Integer> getCurrentRollResult(List<String> config) {
        if (config.size() < 1) {
            return ImmutableList.of();
        }
        String currentRollResultString = config.get(CURRENT_ROLL_CONFIG_INDEX);
        if (EMPTY.equals(currentRollResultString)) {
            return ImmutableList.of();
        }
        return Arrays.stream(currentRollResultString.split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private boolean rollFinished(List<String> config) {
        Set<Integer> rerollSet = getRerollSet(config);
        return getCurrentRollResult(config).stream().noneMatch(rerollSet::contains);
    }

    private int getSidesOfDie(List<String> config) {
        return Integer.parseInt(config.get(SIDES_OF_DIE_CONFIG_INDEX));
    }

    private Set<Integer> getRerollSet(List<String> config) {
        return Arrays.stream(config.get(REROLL_SET_CONFIG_INDEX).split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(ImmutableSet.toImmutableSet());
    }

    private Set<Integer> getSuccessSet(List<String> config) {
        return Arrays.stream(config.get(SUCCESS_SET_CONFIG_INDEX).split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(ImmutableSet.toImmutableSet());
    }

    private Set<Integer> getFailureSet(List<String> config) {
        return Arrays.stream(config.get(FAILURE_SET_CONFIG_INDEX).split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(ImmutableSet.toImmutableSet());
    }

    protected boolean createAnswerMessage(String buttonId, List<String> config) {
        if (CLEAR_BUTTON_ID.equals(buttonId)) {
            return false;
        }
        return FINISH_BUTTON_ID.equals(buttonId) || rollFinished(config);
    }

    private String getConfigStringFromCommandOptions(ApplicationCommandInteractionOption options, String optionId) {
        return options.getOption(optionId)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(ImmutableList.of())
                .stream()
                .map(String::trim)
                .filter(StringUtils::isNumeric)
                .filter(s -> Integer.parseInt(s) > 0)
                .collect(Collectors.joining(SUBSET_DELIMITER));
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        String sideValue = options.getOption(SIDES_OF_DIE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, 1000))
                .map(Object::toString)
                .orElse("6");
        String rerollSet = getConfigStringFromCommandOptions(options, REROLL_SET_ID);
        String successSet = getConfigStringFromCommandOptions(options, SUCCESS_SET_ID);
        String failureSet = getConfigStringFromCommandOptions(options, FAILURE_SET_ID);
        return ImmutableList.of(EMPTY, sideValue, rerollSet, successSet, failureSet, "0");
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected String getButtonMessage(String buttonValue, List<String> config) {
        List<Integer> currentRollResult = getCurrentRollResult(config);
        Set<Integer> successSet = getSuccessSet(config);
        Set<Integer> failureSet = getFailureSet(config);
        Set<Integer> rerollSet = getRerollSet(config);
        if (currentRollResult.isEmpty() || CLEAR_BUTTON_ID.equals(buttonValue)
                || FINISH_BUTTON_ID.equals(buttonValue)
                || rollFinished(config)) {
            return String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                    rerollSet, successSet, failureSet);
        }

        int successes = DiceUtils.numberOfDiceResultsEqual(currentRollResult, successSet);
        int failures = DiceUtils.numberOfDiceResultsEqual(currentRollResult, failureSet);
        return String.format("%s = %d successes and %d failures", markIn(currentRollResult, getToMark(config)), successes, failures);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(String buttonValue, List<String> config) {
        int sidesOfDie = getSidesOfDie(config);
        if (buttonValue == null || CLEAR_BUTTON_ID.equals(buttonValue) || FINISH_BUTTON_ID.equals(buttonValue) || rollFinished(config)) {
            List<Button> buttons = IntStream.range(1, 16)
                    .mapToObj(i -> Button.primary(createButtonCustomId(COMMAND_NAME, String.valueOf(i), config),
                            String.format("%d%s%s", i, DICE_SYMBOL, sidesOfDie)))
                    .collect(Collectors.toList());
            return Lists.partition(buttons, 5).stream().map(ActionRow::of).collect(Collectors.toList());
        }
        return ImmutableList.of(
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, REROLL_BUTTON_ID, config), "Reroll"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, FINISH_BUTTON_ID, config), "Finish"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, CLEAR_BUTTON_ID, config), "Clear")
                ));
    }
}
