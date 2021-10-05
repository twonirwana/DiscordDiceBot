package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class CountSuccessesCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "count_successes";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final int MAX_NUMBER_SIDES_OR_TARGET_NUMBER = 1000;

    public CountSuccessesCommand(Snowflake botUserId) {
        super(new ActiveButtonsCache(COMMAND_NAME), botUserId);
    }

    private static String createButtonLabel(String value, List<String> config) {
        return String.format("%sd%s", value, config.get(0));
    }

    @Override
    protected String getCommandDescription() {
        return "Register the x sided Dice with the target number y system in the channel.";
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
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_TARGET_OPTION)
                        .required(true)
                        .description("Target number")
                        .type(ApplicationCommandOption.Type.INTEGER.getValue())
                        .build());
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonValue, List<String> config) {
        int numberOfDice = Integer.parseInt(buttonValue);
        int sidesOfDie = Integer.parseInt(config.get(0));
        int targetNumber = Integer.parseInt(config.get(1));
        List<Integer> rollResult = DiceUtils.rollDiceOfType(numberOfDice, sidesOfDie);
        int numberOf6s = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        String details = "Target: " + targetNumber + " = " + DiceUtils.makeGreaterEqualTargetValuesBold(rollResult, targetNumber);
        String title = String.format("%dd%d = %d", numberOfDice, sidesOfDie, numberOf6s);
        log.info(String.format("%s - %s: %s", channelId.asString(), title, details.replace("**", "")));
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
        return ImmutableList.of(sideValue, targetValue);
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected String getConfigDescription(List<String> config) {
        return String.format(" Side of die: %s and target number: %s", config.get(0), config.get(1));
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        return ImmutableList.of(
                ActionRow.of(
                        //              ID,  label
                        Button.primary(createButtonCustomId(COMMAND_NAME, "1", config), createButtonLabel("1", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "2", config), createButtonLabel("2", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "3", config), createButtonLabel("3", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "4", config), createButtonLabel("4", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "5", config), createButtonLabel("5", config))
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "6", config), createButtonLabel("6", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "7", config), createButtonLabel("7", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "8", config), createButtonLabel("8", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "9", config), createButtonLabel("9", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "10", config), createButtonLabel("10", config))
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "11", config), createButtonLabel("11", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "12", config), createButtonLabel("12", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "13", config), createButtonLabel("13", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "14", config), createButtonLabel("14", config)),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "15", config), createButtonLabel("15", config))
                ));
    }
}
