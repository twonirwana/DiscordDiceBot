package de.janno.discord.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceUtils;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.janno.discord.command.CommandUtils.*;

@Slf4j
public class PoolTargetCommand extends AbstractCommand<PoolTargetCommand.Config, PoolTargetCommand.State> {

    private static final String COMMAND_NAME = "pool_target";
    private static final String SIDES_OF_DIE_OPTION = "sides";
    private static final String MAX_DICE_OPTION = "max_dice";
    private static final String REROLL_SET_OPTION = "reroll_set";
    private static final String BOTCH_SET_OPTION = "botch_set";

    private static final String REROLL_VARIANT_OPTION = "reroll_variant";
    private static final String ALWAYS_REROLL = "always";
    private static final String ASK_FOR_REROLL = "ask";

    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String DO_REROLL_ID = "do_reroll";
    private static final String DO_NOT_REROLL_ID = "no_reroll";
    private static final int MAX_NUMBER_OF_DICE = 25;

    private static final String DICE_SYMBOL = "d";

    private static final String SUBSET_DELIMITER = ";";
    private static final int BUTTON_VALUE_INDEX = 1;

    private static final int SIDE_OF_DIE_INDEX = 2;
    private static final int MAX_DICE_INDEX = 3;
    private static final int REROLL_SET_INDEX = 4;
    private static final int BOTCH_SET_INDEX = 5;
    private static final int REROLL_VARIANT_INDEX = 6;

    //state in id
    private static final int POOL_SIZE_VALUE_INDEX = 7;
    private static final int TARGET_INDEX = 8;
    private static final String EMPTY = "EMPTY";

    private final DiceUtils diceUtils;

    public PoolTargetCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public PoolTargetCommand(DiceUtils diceUtils) {
        super(new ButtonMessageCache(COMMAND_NAME));
        this.diceUtils = diceUtils;
    }

    @Override
    protected String getCommandDescription() {
        return "Roll dice and against a variable target";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Use '/pool_target start' to get message, where the user can roll dice")
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
                        .name(SIDES_OF_DIE_OPTION)
                        .required(true)
                        .description("Dice sides")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(2d)
                        .maxValue(25d).build(),
                ApplicationCommandOptionData.builder()
                        .name(MAX_DICE_OPTION)
                        .required(false)
                        .description("Max number of dice")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(1d)
                        .maxValue(Double.valueOf(MAX_NUMBER_OF_DICE))
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(REROLL_SET_OPTION)
                        .required(false)
                        .description("Result numbers that are reroll, seperated by ','")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(BOTCH_SET_OPTION)
                        .required(false)
                        .description("Failure dice numbers, seperated by ','")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(REROLL_VARIANT_OPTION)
                        .required(false)
                        .description("Options for special reroll handling")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name(ALWAYS_REROLL).value(ALWAYS_REROLL).build())
                        .addChoice(ApplicationCommandOptionChoiceData.builder().name(ASK_FOR_REROLL).value(ASK_FOR_REROLL).build())

                        .build()
        );
    }

    @Override
    protected Answer getAnswer(State state, Config config) {
        List<Integer> rollResult = diceUtils.rollDiceOfType(state.getDicePool(), config.getDiceSides());
        if (state.getDoReroll()) {
            rollResult = diceUtils.explodingReroll(config.getDiceSides(), rollResult, config.getRerollSet());
        }
        rollResult = rollResult.stream().sorted().collect(Collectors.toList());
        int numberOfSuccesses = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, state.getTargetNumber());
        int numberOfBotches = DiceUtils.numberOfDiceResultsEqual(rollResult, config.getBotchSet());

        int totalResults = numberOfSuccesses - numberOfBotches;

        Set<Integer> toMark = IntStream.range(state.getTargetNumber(), config.getDiceSides() + 1).boxed().collect(Collectors.toSet());
        toMark.addAll(config.getBotchSet());
        if (state.getDoReroll()) {
            toMark.addAll(config.getRerollSet());
        }
        String details = String.format("%s ≥%d = %s", markIn(rollResult, toMark), state.getTargetNumber(), totalResults);
        String title = String.format("%dd%d = %d", state.getDicePool(), config.getDiceSides(), totalResults);
        return new Answer(title, details, ImmutableList.of());
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(CONFIG_DELIMITER);

        int sideOfDie = Integer.parseInt(customIdSplit[SIDE_OF_DIE_INDEX]);
        int maxNumberOfButtons = Integer.parseInt(customIdSplit[MAX_DICE_INDEX]);
        Set<Integer> rerollSet = toSet(customIdSplit[REROLL_SET_INDEX], SUBSET_DELIMITER, EMPTY);
        Set<Integer> botchSet = toSet(customIdSplit[BOTCH_SET_INDEX], SUBSET_DELIMITER, EMPTY);
        String rerollVariant = customIdSplit[REROLL_VARIANT_INDEX];

        return new Config(sideOfDie, maxNumberOfButtons, rerollSet, botchSet, rerollVariant);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(CONFIG_DELIMITER);
        String buttonValue = customIdSplit[BUTTON_VALUE_INDEX];
        //clear button was pressed
        if (CLEAR_BUTTON_ID.equals(buttonValue)) {
            return new State(null, null, null);
        }
        //pool size in config is empty and button value is number -> pool size was set
        if (EMPTY.equals(customIdSplit[POOL_SIZE_VALUE_INDEX]) && StringUtils.isNumeric(buttonValue)) {
            Integer buttonNumber = Integer.valueOf(buttonValue);
            return new State(buttonNumber, null, null);
        }

        Config config = getConfigFromEvent(event);
        //pool size is already given and button value is number -> target was set
        if (StringUtils.isNumeric(customIdSplit[POOL_SIZE_VALUE_INDEX]) && StringUtils.isNumeric(buttonValue)) {
            //if the config is always reroll we can set it, else we need to ask
            Boolean doReroll = ALWAYS_REROLL.equals(config.getRerollVariant()) ? true : null;
            return new State(Integer.valueOf(customIdSplit[POOL_SIZE_VALUE_INDEX]), Integer.valueOf(buttonValue), doReroll);
        }

        //pool size is already given and target is given -> do reroll was asked
        if (StringUtils.isNumeric(customIdSplit[POOL_SIZE_VALUE_INDEX]) && StringUtils.isNumeric(customIdSplit[TARGET_INDEX])) {
            boolean doReroll = DO_REROLL_ID.equals(buttonValue);
            return new State(Integer.valueOf(customIdSplit[POOL_SIZE_VALUE_INDEX]), Integer.valueOf(customIdSplit[TARGET_INDEX]), doReroll);

        }

        log.error("CustomId:'{}}' correspond to no known state", event.getCustomId());
        return new State(null, null, null);
    }


    @Override
    protected boolean createAnswerMessage(State state, Config config) {
        return state.getDicePool() != null && state.getTargetNumber() != null && state.getDoReroll() != null;
    }

    @Override
    protected Config getConfigFromStartOptions(ApplicationCommandInteractionOption options) {
        int sideValue = Math.toIntExact(options.getOption(SIDES_OF_DIE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, 1000))
                .orElse(10L));
        int maxButton = Math.toIntExact(options.getOption(MAX_DICE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, 25))
                .orElse(15L));
        Set<Integer> rerollSet = getSetFromCommandOptions(options, REROLL_SET_OPTION, ",");
        Set<Integer> botchSet = getSetFromCommandOptions(options, BOTCH_SET_OPTION, ",");
        String rerollVariant = options.getOption(REROLL_VARIANT_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(ALWAYS_REROLL);
        return new Config(sideValue, maxButton, rerollSet, botchSet, rerollVariant);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected String getButtonMessage(Config config) {
        String configDescription = getConfigDescription(config);
        return String.format("Click on the buttons to roll dice%s", configDescription);
    }

    private String getConfigDescription(Config config) {
        String rerollDescription = null;
        String botchDescription = null;
        if (!config.getRerollSet().isEmpty()) {
            rerollDescription = String.format("%s reroll:%s", config.getRerollVariant(),
                    config.getRerollSet().stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        }
        if (!config.getBotchSet().isEmpty()) {
            botchDescription = "botch:" + config.getBotchSet().stream().sorted().map(String::valueOf).collect(Collectors.joining(","));
        }
        String configDescription = Stream.of(rerollDescription, botchDescription)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" and "));
        if (!configDescription.isEmpty()) {
            configDescription = String.format(", with %s", configDescription);
        }
        return configDescription;
    }


    @Override
    protected String getButtonMessageWithState(State state, Config config) {
        if (state.getDicePool() != null && state.getTargetNumber() != null && state.getDoReroll() == null) {
            String rerollNumbers = config.getRerollSet().stream()
                    .map(String::valueOf)
                    .map(s -> String.format("%ss", s))
                    .collect(Collectors.joining(","));
            return String.format("Should %s in %dd%d against %d be be rerolled?", rerollNumbers, state.getDicePool(), config.getDiceSides(), state.getTargetNumber());
        }
        if (state.getDicePool() != null && state.getTargetNumber() == null) {
            String configDescription = getConfigDescription(config);
            return String.format("Click on the target to roll %dd%d against it%s", state.getDicePool(), config.getDiceSides(), configDescription);
        }

        return getButtonMessage(config);
    }

    @Override
    protected List<LayoutComponent> getButtonLayoutWithState(State state, Config config) {
        if (state.getDicePool() != null && state.getTargetNumber() != null && state.getDoReroll() == null) {
            return ImmutableList.of(
                    ActionRow.of(
                            Button.primary(createButtonCustomId(DO_REROLL_ID, config, state), "Reroll"),
                            Button.primary(createButtonCustomId(DO_NOT_REROLL_ID, config, state), "No reroll")
                    ));
        }
        if (state.getDicePool() != null && state.getTargetNumber() == null) {
            List<Button> buttons = IntStream.range(2, config.getDiceSides() + 1)
                    .mapToObj(i -> Button.primary(createButtonCustomId(String.valueOf(i), config, state), String.format("%d", i)))
                    .collect(Collectors.toList());
            buttons.add(Button.primary(createButtonCustomId("clear", config, null), "Clear"));
            return Lists.partition(buttons, 5).stream().map(ActionRow::of).collect(Collectors.toList());
        }
        return createPoolButtonLayout(config);
    }


    String createButtonCustomId(@NonNull String buttonValue, @NonNull Config config, @Nullable State state) {
        Preconditions.checkArgument(!buttonValue.contains(CONFIG_DELIMITER));

        String[] values = new String[9];
        values[0] = COMMAND_NAME;
        values[BUTTON_VALUE_INDEX] = buttonValue;
        values[SIDE_OF_DIE_INDEX] = String.valueOf(config.getDiceSides());
        values[MAX_DICE_INDEX] = String.valueOf(config.getMaxNumberOfButtons());
        values[REROLL_SET_INDEX] = config.getRerollSet().isEmpty() ? EMPTY : config.getRerollSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER));
        values[BOTCH_SET_INDEX] = config.getBotchSet().isEmpty() ? EMPTY : config.getBotchSet().stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER));
        values[REROLL_VARIANT_INDEX] = config.getRerollVariant();
        values[POOL_SIZE_VALUE_INDEX] = Optional.ofNullable(state).map(State::getDicePool).map(String::valueOf).orElse(EMPTY);
        values[TARGET_INDEX] = Optional.ofNullable(state).map(State::getTargetNumber).map(String::valueOf).orElse(EMPTY);

        return String.join(CONFIG_DELIMITER, values);

    }

    @Override
    protected List<LayoutComponent> getButtonLayout(Config config) {
        return createPoolButtonLayout(config);
    }

    private List<LayoutComponent> createPoolButtonLayout(Config config) {
        List<Button> buttons = IntStream.range(1, config.getMaxNumberOfButtons() + 1)
                .mapToObj(i -> Button.primary(createButtonCustomId(String.valueOf(i), config, null),
                        String.format("%d%s%s", i, DICE_SYMBOL, config.getDiceSides())))
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream().map(ActionRow::of).collect(Collectors.toList());
    }

    @Override
    protected String getStartOptionsValidationMessage(ApplicationCommandInteractionOption options) {
        Config conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    String validate(Config config) {

        if (config.getRerollSet().stream().anyMatch(i -> i > config.getDiceSides())) {
            return String.format("Reroll set %s contains a number bigger then the sides of the die %s", config.getRerollSet(), config.getDiceSides());
        }
        if (config.getBotchSet().stream().anyMatch(i -> i > config.getDiceSides())) {
            return String.format("Botch set %s contains a number bigger then the sides of the die %s", config.getBotchSet(), config.getDiceSides());
        }
        if (config.getRerollSet().size() >= config.getDiceSides()) {
            return "The reroll must not contain all numbers";
        }

        return null;
    }

    @Value
    protected static class Config implements IConfig {
        int diceSides;
        int maxNumberOfButtons;
        @NonNull
        Set<Integer> rerollSet;
        @NonNull
        Set<Integer> botchSet;
        String rerollVariant;

        @Override
        public String toShortString() {
            return ImmutableList.of(
                    String.valueOf(diceSides),
                    String.valueOf(maxNumberOfButtons),
                    rerollSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    botchSet.stream().map(String::valueOf).collect(Collectors.joining(SUBSET_DELIMITER)),
                    rerollVariant
            ).toString();
        }
    }

    @Value
    static class State implements IState {
        Integer dicePool;
        Integer targetNumber;
        Boolean doReroll;
    }
}
