package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.DiceResult;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * TODO:
 * - doc
 * - x2 Button that multiplies the number of all dice
 * - only the result is in the channel and the adding of dice is ephemeral. This is currently not possible because ephemeral can't be deleted
 */
@Slf4j
public class SumDiceSetCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "sum_dice_set";
    private static final String DICE_SET_DELIMITER = " + ";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String EMPTY_MESSAGE = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final int MAX_NUMBER_OF_DICE = 100;

    public SumDiceSetCommand() {
        super(new ActiveButtonsCache(COMMAND_NAME));
    }

    @Override
    protected String getCommandDescription() {
        return "Creates a personal message to create a dice set an rolling it";
    }

    @Override
    protected EmbedCreateSpec getHelpMessage() {
        return EmbedCreateSpec.builder()
                .description("Use '/sum_dice_set start' " +
                        "to get message, where the user can create a dice set and roll it.")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<ApplicationCommandOptionData> getStartOptions() {
        return ImmutableList.of();
    }

    @Override
    protected DiceResult rollDice(String buttonValue, List<String> config) {
        DiceResult diceResult = DiceParserHelper.rollWithDiceParser(String.join(DICE_SET_DELIMITER, config));
        log.info(String.format("%s:%s -> %s: %s", getName(), config, diceResult.getResultTitle(), diceResult.getResultDetails()));
        return diceResult;
    }

    @Override
    protected String editMessage(String buttonId, List<String> config) {
        if (ROLL_BUTTON_ID.equals(buttonId)) {
            return EMPTY_MESSAGE;
        } else if (CLEAR_BUTTON_ID.equals(buttonId)) {
            return EMPTY_MESSAGE;
        } else {
            Map<String, Integer> currentDiceSet = config.stream()
                    .collect(Collectors.toMap(s -> s.substring(s.indexOf("d")), s -> Integer.valueOf(s.substring(0, s.indexOf("d")))));
            String diceModifier = buttonId.substring(0, 1);
            String die = buttonId.substring(2);
            int currentCount = currentDiceSet.getOrDefault(die, 0);
            if ("-".equals(diceModifier)) {
                if (currentCount > 1) {
                    currentDiceSet.put(die, currentCount - 1);
                } else if (currentCount == 1) {
                    currentDiceSet.remove(die);
                }
            } else if ("+".equals(diceModifier)) {
                int currentNumberOfDice = currentDiceSet.values().stream().mapToInt(i -> i).sum();
                if (currentNumberOfDice < MAX_NUMBER_OF_DICE) {
                    if (currentCount >= 1) {
                        currentDiceSet.put(die, currentCount + 1);
                    } else if (currentCount == 0) {
                        currentDiceSet.put(die, 1);
                    }
                }
            }
            if (currentDiceSet.isEmpty()) {
                return EMPTY_MESSAGE;
            }
            return currentDiceSet.entrySet().stream()
                    .sorted(Comparator.comparing(e -> Integer.parseInt(e.getKey().substring(1))))
                    .map(e -> e.getValue() + e.getKey())
                    .collect(Collectors.joining(DICE_SET_DELIMITER));
        }
    }

    protected boolean createNewMessage(String buttonId, List<String> config) {
        return ROLL_BUTTON_ID.equals(buttonId) && !config.isEmpty();
    }

    protected boolean copyButtonMessageToTheEnd(String buttonId, List<String> config) {
        return ROLL_BUTTON_ID.equals(buttonId) && !config.isEmpty();
    }

    @Override
    protected List<String> getConfigFromEvent(ComponentInteractionEvent event) {
        String buttonMessage = event.getMessage().map(Message::getContent).orElse("");
        if (EMPTY_MESSAGE.equals(buttonMessage)) {
            return ImmutableList.of();
        }
        return event.getMessage()
                .map(Message::getContent)
                .map(s -> s.split(Pattern.quote(DICE_SET_DELIMITER)))
                .map(Arrays::asList)
                .orElse(ImmutableList.of());
    }

    @Override
    protected String getButtonMessage(List<String> config) {
        return EMPTY_MESSAGE;
    }

    @Override
    protected List<String> getConfigValuesFromStartOptions(ApplicationCommandInteractionOption options) {
        return ImmutableList.of();
    }

    @Override
    public boolean matchingComponentCustomId(String buttonCustomId) {
        return buttonCustomId.startsWith(COMMAND_NAME + CONFIG_DELIMITER);
    }

    @Override
    protected List<LayoutComponent> getButtonLayout(List<String> config) {
        return ImmutableList.of(
                ActionRow.of(
                        //              ID,  label
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d4", ImmutableList.of()), "+1d4"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d4", ImmutableList.of()), "-1d4"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d6", ImmutableList.of()), "+1d6"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d6", ImmutableList.of()), "-1d6")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d8", ImmutableList.of()), "+1d8"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d8", ImmutableList.of()), "-1d8"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d10", ImmutableList.of()), "+1d10"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d10", ImmutableList.of()), "-1d10"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, CLEAR_BUTTON_ID, ImmutableList.of()), "Clear")
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d12", ImmutableList.of()), "+1d12"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d12", ImmutableList.of()), "-1d12"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1d20", ImmutableList.of()), "+1d20"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d20", ImmutableList.of()), "-1d20"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, ROLL_BUTTON_ID, ImmutableList.of()), "Roll")
                ));
    }
}
