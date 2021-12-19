package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.dice.DiceResult;
import de.janno.discord.dice.DiceUtils;
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
import java.util.stream.Stream;

/**
 * TODO:
 * - only the result is in the channel and the adding of dice is ephemeral. This is currently not possible because ephemeral can't be deleted
 * - d100
 */
@Slf4j
public class SumDiceSetCommand extends AbstractCommand {

    private static final String COMMAND_NAME = "sum_dice_set";
    private static final String DICE_SET_DELIMITER = " ";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String EMPTY_MESSAGE = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String X2_BUTTON_ID = "x2";
    private static final int MAX_NUMBER_OF_DICE = 100;
    private final DiceUtils diceUtils;

    public SumDiceSetCommand() {
        this(new DiceUtils());
    }


    //for testing
    public SumDiceSetCommand(DiceUtils diceUtils) {
        super(new ActiveButtonsCache(COMMAND_NAME));
        this.diceUtils = diceUtils;
    }


    @Override
    protected String getCommandDescription() {
        return "Configure a variable set of d4 to d20 dice";
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
        Map<String, Integer> diceSetMap = currentDiceSet(config);
        List<Integer> diceResultValues = diceSetMap.entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains("d")) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .flatMap(e -> {
                    if ("".equals(e.getKey())) {
                        return Stream.of(e.getValue());
                    }
                    int diceSides = Integer.parseInt(e.getKey().substring(1));
                    return diceUtils.rollDiceOfType(Math.abs(e.getValue()), diceSides).stream()
                            .map(dv -> {
                                //modify the result if the dice count is negative
                                if (e.getValue() < 0) {
                                    return dv * -1;
                                }
                                return dv;
                            });
                })
                .collect(Collectors.toList());
        long sumResult = diceResultValues.stream().mapToLong(Integer::longValue).sum();
        String title = parseDiceMapToMessageString(diceSetMap);
        DiceResult diceResult = new DiceResult(String.format("%s = %d", title, sumResult), diceResultValues.toString());
        log.info(String.format("%s:%s -> %s: %s", getName(), config, diceResult.getResultTitle(), diceResult.getResultDetails()));
        return diceResult;
    }

    private Map<String, Integer> currentDiceSet(List<String> config) {
        return config.stream()
                .collect(Collectors.toMap(s -> {
                    if (s.contains("d")) {
                        return s.substring(s.indexOf("d"));
                    } else {
                        return "";
                    }
                }, s -> {
                    if (s.contains("d")) {
                        return Integer.valueOf(s.substring(0, s.indexOf("d")));
                    } else {
                        return Integer.valueOf(s);
                    }
                }));
    }

    private String parseDiceMapToMessageString(Map<String, Integer> diceSet) {
        String message = diceSet.entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains("d")) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .map(e -> String.format("%s%d%s", e.getValue() > 0 ? "+" : "", e.getValue(), e.getKey()))
                .collect(Collectors.joining(DICE_SET_DELIMITER));
        //remove leading +
        if (message.startsWith("+")) {
            message = message.substring(1);
        }
        return message;
    }

    private int limit(int input) {
        if (input > 100) {
            return 100;
        }
        if (input < -100) {
            return -100;
        }
        return input;
    }

    @Override
    protected String editMessage(String buttonId, List<String> config) {
        if (ROLL_BUTTON_ID.equals(buttonId)) {
            return EMPTY_MESSAGE;
        } else if (CLEAR_BUTTON_ID.equals(buttonId)) {
            return EMPTY_MESSAGE;
        } else if (X2_BUTTON_ID.equals(buttonId)) {
            return parseDiceMapToMessageString(currentDiceSet(config).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> limit(e.getValue() * 2))));
        } else {
            Map<String, Integer> currentDiceSet = currentDiceSet(config);
            int diceModifier;
            String die;

            if (buttonId.contains("d")) {
                diceModifier = "-".equals(buttonId.substring(0, 1)) ? -1 : +1;
                die = buttonId.substring(2);
            } else {
                diceModifier = Integer.parseInt(buttonId);
                die = "";
            }
            int currentCount = currentDiceSet.getOrDefault(die, 0);
            int newCount = currentCount + diceModifier;
            newCount = limit(newCount);

            if (newCount == 0) {
                currentDiceSet.remove(die);
            } else {
                currentDiceSet.put(die, newCount);
            }

            if (currentDiceSet.isEmpty()) {
                return EMPTY_MESSAGE;
            }
            return parseDiceMapToMessageString(currentDiceSet);
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
                .orElse(ImmutableList.of())
                .stream()
                //for handling legacy buttons with '1d4 + 1d6)
                .filter(s -> !"+".equals(s))
                //adding the + for the first die type in the message
                .map(s -> {
                    if (!s.startsWith("-") && !s.startsWith("+")) {
                        return "+" + s;
                    }
                    return s;
                })
                .collect(Collectors.toList());
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
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1d6", ImmutableList.of()), "-1d6"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, X2_BUTTON_ID, ImmutableList.of()), "x2")
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
                ),
                ActionRow.of(
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+1", ImmutableList.of()), "+1"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-1", ImmutableList.of()), "-1"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+5", ImmutableList.of()), "+5"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "-5", ImmutableList.of()), "-5"),
                        Button.primary(createButtonCustomId(COMMAND_NAME, "+10", ImmutableList.of()), "+10")
                ));
    }
}
