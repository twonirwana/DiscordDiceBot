package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import de.janno.discord.cache.ActiveButtonsCache;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.janno.discord.dice.DiceUtils.makeBold;

@Slf4j
public class HoldRerollCommand extends AbstractCommand<HoldRerollCommand.Config> {

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
        //current roll and rerollCount should not be part of the hash
        super(new ActiveButtonsCache(COMMAND_NAME));
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

    private Set<Integer> getToMark(Config config) {
        return IntStream.range(1, config.getSidesOfDie() + 1)
                .filter(i -> !config.getRerollSet().contains(i))
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
    protected String createButtonCustomId(String system, String value, Config config) {

        Preconditions.checkArgument(!system.contains(CONFIG_DELIMITER));
        Preconditions.checkArgument(!value.contains(CONFIG_DELIMITER));

        return String.join(CONFIG_DELIMITER,
                system,
                value,
                config.getCurrentResults().isEmpty() ? EMPTY : config.getCurrentResults().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                String.valueOf(config.getSidesOfDie()),
                config.getRerollSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getSuccessSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                config.getFailureSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                String.valueOf(config.getRerollCounter())
        );
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
    protected List<DiceResult> getDiceResult(String buttonValue, Config config) {

        int successes = DiceUtils.numberOfDiceResultsEqual(config.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(config.getCurrentResults(), config.getFailureSet());
        int rerollCount = config.getRerollCounter();
        String title;
        if (rerollCount == 0) {
            title = String.format("Success: %d and Failure: %d", successes, failures);
        } else {
            title = String.format("Success: %d, Failure: %d and Rerolls: %d", successes, failures, rerollCount);
        }

        DiceResult diceResult = new DiceResult(title, markIn(config.getCurrentResults(), getToMark(config)));
        return ImmutableList.of(diceResult);
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(CONFIG_DELIMITER);
        String buttonValue = getButtonValueFromEvent(event);
        List<Integer> currentResult = getCurrentRollResult(customIdSplit[2]);
        int sideOfDie = Integer.parseInt(customIdSplit[3]);
        Set<Integer> rerollSet = toSet(customIdSplit[4]);
        Set<Integer> successSet = toSet(customIdSplit[5]);
        Set<Integer> failureSet = toSet(customIdSplit[6]);
        int rerollCount = Integer.parseInt(customIdSplit[7]);

        if (REROLL_BUTTON_ID.equals(buttonValue)) {
            currentResult = currentResult.stream()
                    .map(i -> {
                        if (rerollSet.contains(i)) {
                            return diceUtils.rollDice(sideOfDie);
                        }
                        return i;
                    }).collect(Collectors.toList());
            rerollCount++;
        } else if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            currentResult = ImmutableList.of();
            rerollCount = 0;
        } else if (StringUtils.isNumeric(buttonValue)) {
            int numberOfDice = Integer.parseInt(buttonValue);
            currentResult = diceUtils.rollDiceOfType(numberOfDice, sideOfDie);
            rerollCount = 0;
        }
        return new Config(currentResult, sideOfDie, rerollSet, successSet, failureSet, rerollCount);
    }

    private List<Integer> getCurrentRollResult(String currentRollResultString) {
        if (EMPTY.equals(currentRollResultString)) {
            return ImmutableList.of();
        }
        return Arrays.stream(currentRollResultString.split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    private boolean rollFinished(Config config) {
        return config.currentResults.stream().noneMatch(i -> config.getRerollSet().contains(i));
    }

    private Set<Integer> toSet(String value) {
        return Arrays.stream(value.split(SUBSET_DELIMITER))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .collect(ImmutableSet.toImmutableSet());
    }

    protected boolean createAnswerMessage(String buttonId, Config config) {
        if (CLEAR_BUTTON_ID.equals(buttonId)) {
            return false;
        }
        return FINISH_BUTTON_ID.equals(buttonId) || rollFinished(config);
    }

    private Set<Integer> getConfigSetFromCommandOptions(ApplicationCommandInteractionOption options, String optionId) {
        return options.getOption(optionId)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .map(s -> s.split(","))
                .map(Arrays::asList)
                .orElse(ImmutableList.of())
                .stream()
                .map(String::trim)
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .filter(i -> i > 0)
                .collect(ImmutableSet.toImmutableSet());
    }

    @Override
    protected Config getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getOption(SIDES_OF_DIE_ID)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, 1000))
                .orElse(6L));
        Set<Integer> rerollSet = getConfigSetFromCommandOptions(options, REROLL_SET_ID);
        Set<Integer> successSet = getConfigSetFromCommandOptions(options, SUCCESS_SET_ID);
        Set<Integer> failureSet = getConfigSetFromCommandOptions(options, FAILURE_SET_ID);
        return new Config(ImmutableList.of(), sideValue, rerollSet, successSet, failureSet, 0);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected String getButtonMessage(String buttonValue, Config config) {
        if (config.getRerollSet().isEmpty() || CLEAR_BUTTON_ID.equals(buttonValue)
                || FINISH_BUTTON_ID.equals(buttonValue)
                || rollFinished(config)) {
            return String.format("Click on the buttons to roll dice. Reroll set: %s, Success Set: %s and Failure Set: %s",
                    config.getRerollSet(), config.getSuccessSet(), config.getFailureSet());
        }

        int successes = DiceUtils.numberOfDiceResultsEqual(config.getCurrentResults(), config.getSuccessSet());
        int failures = DiceUtils.numberOfDiceResultsEqual(config.getCurrentResults(), config.getFailureSet());
        return String.format("%s = %d successes and %d failures", markIn(config.getCurrentResults(), getToMark(config)), successes, failures);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(String buttonValue, Config config) {
        if (buttonValue == null || CLEAR_BUTTON_ID.equals(buttonValue) || FINISH_BUTTON_ID.equals(buttonValue) || rollFinished(config)) {
            List<Button> buttons = IntStream.range(1, 16)
                    .mapToObj(i -> Button.primary(createButtonCustomId(COMMAND_NAME, String.valueOf(i), config),
                            String.format("%d%s%s", i, DICE_SYMBOL, config.getSidesOfDie())))
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

    @Override
    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {
        Config conf = getConfigValuesFromStartOptions(options);
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
        return null;
    }

    @Value
    protected static class Config implements IConfig {
        List<Integer> currentResults;
        int sidesOfDie;
        Set<Integer> rerollSet;
        Set<Integer> successSet;
        Set<Integer> failureSet;
        int rerollCounter;

        @Override
        public String toMetricString() {
            return ImmutableList.of(
                    String.valueOf(sidesOfDie),
                    rerollSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    successSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    failureSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER))
            ).toString();
        }

        @Override
        public int getHashForCache() {
            return Objects.hash(sidesOfDie, rerollCounter, successSet, failureSet);
        }
    }
}
