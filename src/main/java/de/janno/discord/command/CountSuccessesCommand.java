package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.util.ApplicationCommandOptionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class CountSuccessesCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "count_successes";
    private static final String BUTTON_MESSAGE = "Click a button to roll the number of dice";
    private static final String ACTION_SIDE_OPTION = "dice_sides";
    private static final String ACTION_TARGET_OPTION = "target_number";
    private static final int MAX_NUMBER_OF_DICE = 100;

    public CountSuccessesCommand(Snowflake botUserId) {
        super(new ActiveButtonsCache(), botUserId);
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
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .build(),
                ApplicationCommandOptionData.builder()
                        .name(ACTION_TARGET_OPTION)
                        .required(true)
                        .description("Target number")
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .build());
    }

    @Override
    protected DiceResult rollDice(Snowflake channelId, String buttonValue, List<String> config) {
        int numberOfDice = Math.min(Integer.parseInt(buttonValue), MAX_NUMBER_OF_DICE);
        int sidesOfDie = Integer.parseInt(config.get(0));
        int targetNumber = Integer.parseInt(config.get(1));
        List<Integer> rollResult = DiceUtils.rollDiceOfType(numberOfDice, sidesOfDie);
        int numberOf6s = DiceUtils.numberOfDiceResultsGreaterEqual(rollResult, targetNumber);
        String details = "Target: " + targetNumber + " = " + DiceUtils.makeGreaterEqualTargetValuesBold(rollResult, targetNumber);
        String title = String.format("%dd%d = %d", numberOfDice, sidesOfDie, numberOf6s);
        log.info(String.format("%s %s", title, details));
        return new DiceResult(title, details);
    }


    @Override
    protected String getButtonMessage() {
        return BUTTON_MESSAGE;
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        String sideValue = options.getOption(ACTION_SIDE_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Object::toString)
                .orElse("6");
        String targetValue = options.getOption(ACTION_TARGET_OPTION)
                .flatMap(ApplicationCommandInteractionOption::getValue)
                .map(ApplicationCommandInteractionOptionValue::asLong)
                .map(Object::toString)
                .orElse("6");
        return ImmutableList.of(sideValue, targetValue);
    }


    @Override
    protected boolean matchingButtonCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        return ImmutableList.of(
                ActionRow.of(
                        //              ID,  label
                        Button.primary(createButtonCustomId(COMMAND_NAME, "1", config), "1"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "2", config), "2"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "3", config), "3"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "4", config), "4"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "5", config), "5")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "6", config), "6"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "7", config), "7"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "8", config), "8"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "9", config), "9"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "10", config), "10")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "11", config), "11"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "12", config), "12"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "13", config), "13"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "14", config), "14"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "15", config), "15")
                ));
    }
}
