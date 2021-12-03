package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
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
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.janno.discord.dice.DiceUtils.makeBold;


@Slf4j
public class CountSuccessesCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "count_successes";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final String ACTION_MAX_DICE_OPTION = "max_dice";
    private static final int MAX_NUMBER_OF_DICE = 25;
    private static final String ACTION_GLITCH_OPTION = "glitch";
    private static final int MAX_NUMBER_SIDES_OR_TARGET_NUMBER = 1000;
    private static final String GLITCH_OPTION_HALF_ONES = "half_dice_one";
    private static final String GLITCH_NO_OPTION = "no_glitch";

    public CountSuccessesCommand() {
        super(new ActiveButtonsCache(COMMAND_NAME));
    }

    private static String createButtonLabel(String value, List<String> config) {
        return String.format("%sd%s", value, config.get(0));
    }

    public static String markSuccessesAndGlitches(List<Integer> diceResults, int target, boolean markOnes) {
        return "[" + diceResults.stream()
                .map(i -> {
                    if (i >= target) {
                        return makeBold(i);
                    } else if (i == 1 && markOnes) {
                        return makeBold(i);
                    }
                    return i + "";
                }).collect(Collectors.joining(",")) + "]";
    }

    @Override
    protected List<String> getConfigFromEvent(ComponentInteractionEvent event) {
        List<String> config = super.getConfigFromEvent(event);

        //handling legacy buttons without glitch option
        if (config.size() < 3) {
            return ImmutableList.<String>builder().addAll(config).add(GLITCH_NO_OPTION).add("15").build();
        }

        //handling legacy buttons without max number of dice option
        if (config.size() < 4) {
            return ImmutableList.<String>builder().addAll(config).add("15").build();
        }
        return config;
    }

    @Override
    protected String getCommandDescription() {
        return "Register the x sided Dice with the target number y system in the channel.";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Use '/count_successes start dice_sides:X target_number:Y' " +
                        "to get Buttons that roll with X sided dice against the target of Y and count the successes." +
                        " A successes are all dice that have a result greater or equal then the target number")
                .addField("Example", "/count_successes start dice_sides:10 target_number:7", false)
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of(ApplicationCommandOptionData.builder()
                        .name(ACTION_SIDE_OPTION)
                        .required(true)
                        .description("Dice side")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(0d)
                        .maxValue((double) MAX_NUMBER_SIDES_OR_TARGET_NUMBER)
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_TARGET_OPTION)
                        .required(true)
                        .description("Target number")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(0d)
                        .maxValue((double) MAX_NUMBER_SIDES_OR_TARGET_NUMBER)
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_GLITCH_OPTION)
                        .required(false)
                        .description("Glitch option")
                        .type(ApplicationCommandOption.Type.STRING.getValue())
                        .addChoice(ApplicationCommandOptionChoiceData.builder()
                                .name(GLITCH_OPTION_HALF_ONES)
                                .value(GLITCH_OPTION_HALF_ONES)
                                .build())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_MAX_DICE_OPTION)
                        .required(false)
                        .description("Max number of dice")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .minValue(1d)
                        .maxValue(Double.valueOf(MAX_NUMBER_OF_DICE))
                        .build());
    }

    @Override
    protected DiceResult rollDice(String buttonValue, List<String> config) {
        int numberOfDice = Integer.parseInt(buttonValue);
        int sidesOfDie = Integer.parseInt(config.get(0));
        int targetNumber = Integer.parseInt(config.get(1));
        String glitchOption = config.get(2);

        List<Integer> rollResult = DiceUtils.rollDiceOfType(numberOfDice, sidesOfDie)
                .stream().sorted().collect(Collectors.toList());
        int numberOf6s = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);

        boolean isGlitch = GLITCH_OPTION_HALF_ONES.equals(glitchOption) && DiceUtils.numberOfDiceResultsEqual(rollResult, 1) > (numberOfDice / 2);
        String details = "Target: " + targetNumber + " = " + markSuccessesAndGlitches(rollResult, targetNumber, isGlitch);
        String glitch = isGlitch ? " - Glitch!" : "";
        String title = String.format("%dd%d = %d%s", numberOfDice, sidesOfDie, numberOf6s, glitch);
        log.info(String.format("%s:%s -> %s: %s", getName(), config, title, details.replace("*", "")));
        return new DiceResult(title, details);
    }

    @Override
    protected String getButtonMessage(List<String> config) {
        return String.format("Click a button to roll the dice against %s", config.get(1));
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        String sideValue = options.getOption(ACTION_SIDE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, MAX_NUMBER_SIDES_OR_TARGET_NUMBER))
                .map(Object::toString)
                .orElse("6");
        String targetValue = options.getOption(ACTION_TARGET_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, MAX_NUMBER_SIDES_OR_TARGET_NUMBER))
                .map(Object::toString)
                .orElse("6");
        String glitchOption = options.getOption(ACTION_GLITCH_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asString)
                .orElse(GLITCH_NO_OPTION);
        String maxDice = options.getOption(ACTION_MAX_DICE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(l -> Math.min(l, MAX_NUMBER_OF_DICE))
                .map(Object::toString)
                .orElse("15");
        return ImmutableList.of(sideValue, targetValue, glitchOption, maxDice);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        List<Button> buttons = IntStream.range(1, Integer.parseInt(config.get(3)) + 1)
                .mapToObj(i -> Button.primary(createButtonCustomId(COMMAND_NAME, String.valueOf(i), config), createButtonLabel(String.valueOf(i), config)))
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(ActionRow::of)
                .collect(Collectors.toList());
    }
}
