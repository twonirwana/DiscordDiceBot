package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.janno.discord.bot.cache.ButtonMessageCache;
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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class SumDiceSetCommand extends AbstractCommand<SumDiceSetCommand.Config, SumDiceSetCommand.State> {
    private static final String COMMAND_NAME = "sum_dice_set";
    private static final String DICE_SET_DELIMITER = " ";
    private static final String ROLL_BUTTON_ID = "roll";
    private static final String EMPTY_MESSAGE = "Click on the buttons to add dice to the set";
    private static final String CLEAR_BUTTON_ID = "clear";
    private static final String X2_BUTTON_ID = "x2";
    private static final String DICE_SYMBOL = "d";
    private static final String MODIFIER_KEY = "m";
    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final DiceUtils diceUtils;

    public SumDiceSetCommand() {
        this(new DiceUtils());
    }

    @VisibleForTesting
    public SumDiceSetCommand(DiceUtils diceUtils) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceUtils = diceUtils;
    }

    private static String parseDiceMapToMessageString(Map<String, Integer> diceSet) {
        String message = diceSet.entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .map(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
                        return String.format("%s%d", e.getValue() > 0 ? "+" : "", e.getValue());
                    }
                    return String.format("%s%d%s", e.getValue() > 0 ? "+" : "", e.getValue(), e.getKey());
                })
                .collect(Collectors.joining(DICE_SET_DELIMITER));
        //remove leading +
        if (message.startsWith("+")) {
            message = message.substring(1);
        }
        return message;
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Configure a variable set of d4 to d20 dice";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/sum_dice_set start' " +
                        "to get message, where the user can create a dice set and roll it.")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }


    @VisibleForTesting
    String createButtonCustomId(String action) {
        Preconditions.checkArgument(!action.contains(BotConstants.CONFIG_DELIMITER));

        return String.join(BotConstants.CONFIG_DELIMITER,
                COMMAND_NAME,
                action);
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of();
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) && !state.getDiceSetMap().isEmpty())) {
            return Optional.empty();
        }
        List<Integer> diceResultValues = state.getDiceSetMap().entrySet().stream()
                .sorted(Comparator.comparing(e -> {
                    if (e.getKey().contains(DICE_SYMBOL)) {
                        return Integer.parseInt(e.getKey().substring(1));
                    }
                    //modifiers should always be at the end
                    return Integer.MAX_VALUE;
                }))
                .flatMap(e -> {
                    if (MODIFIER_KEY.equals(e.getKey())) {
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
                }).toList();
        long sumResult = diceResultValues.stream().mapToLong(Integer::longValue).sum();
        String title = parseDiceMapToMessageString(state.getDiceSetMap());
        return Optional.of(new EmbedDefinition(String.format("%s = %d", title, sumResult), diceResultValues.toString(), ImmutableList.of()));
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
    protected MessageDefinition getButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout())
                .build();
    }

    @Override
    protected Optional<MessageDefinition> getButtonMessageWithState(State state, Config config) {
        if (!(ROLL_BUTTON_ID.equals(state.getButtonValue()) && !state.getDiceSetMap().isEmpty())) {
            return Optional.empty();
        }
        return Optional.of(MessageDefinition.builder()
                .content(EMPTY_MESSAGE)
                .componentRowDefinitions(createButtonLayout())
                .build());
    }

    @Override
    protected Optional<String> getEditButtonMessage(State state, Config config) {
        switch (state.getButtonValue()) {
            case ROLL_BUTTON_ID:
            case CLEAR_BUTTON_ID:
                return Optional.of(EMPTY_MESSAGE);
            case X2_BUTTON_ID:
                return Optional.of(parseDiceMapToMessageString(state.getDiceSetMap().entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> limit(e.getValue() * 2)))));
            default:
                Map<String, Integer> currentDiceSet = new HashMap<>(state.getDiceSetMap());
                int diceModifier;
                String die;

                if (state.getButtonValue().contains(DICE_SYMBOL)) {
                    diceModifier = "-".equals(state.getButtonValue().substring(0, 1)) ? -1 : +1;
                    die = state.getButtonValue().substring(2);
                } else {
                    diceModifier = Integer.parseInt(state.getButtonValue());
                    die = MODIFIER_KEY;
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
                    return Optional.of(EMPTY_MESSAGE);
                }
                return Optional.of(parseDiceMapToMessageString(currentDiceSet));
        }
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        return new Config();
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String buttonMessage = event.getMessageContent();
        String buttonValue = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX)[1];
        if (EMPTY_MESSAGE.equals(buttonMessage)) {
            return new State(buttonValue, ImmutableMap.of());
        }

        return new State(buttonValue, Arrays.stream(buttonMessage.split(Pattern.quote(DICE_SET_DELIMITER)))
                //for handling legacy buttons with '1d4 + 1d6)
                .filter(s -> !"+".equals(s))
                .filter(Objects::nonNull)
                //adding the + for the first die type in the message
                .map(s -> {
                    if (!s.startsWith("-") && !s.startsWith("+")) {
                        return "+" + s;
                    }
                    return s;
                })
                .collect(Collectors.toMap(s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return s.substring(s.indexOf(DICE_SYMBOL));
                    } else {
                        return MODIFIER_KEY;
                    }
                }, s -> {
                    if (s.contains(DICE_SYMBOL)) {
                        return Integer.valueOf(s.substring(0, s.indexOf(DICE_SYMBOL)));
                    } else {
                        return Integer.valueOf(s);
                    }
                })));
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        return new Config();
    }

    private List<ComponentRowDefinition> createButtonLayout() {
        return ImmutableList.of(
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        //              ID,  label
                        ButtonDefinition.builder().id(createButtonCustomId("+1d4")).label("+1d4").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d4")).label("-1d4").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d6")).label("+1d6").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d6")).label("-1d6").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(X2_BUTTON_ID)).label("x2").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1d8")).label("+1d8").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d8")).label("-1d8").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d10")).label("+1d10").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d10")).label("-1d10").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(CLEAR_BUTTON_ID)).label("Clear").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1d12")).label("+1d12").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d12")).label("-1d12").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+1d20")).label("+1d20").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1d20")).label("-1d20").build(),
                        ButtonDefinition.builder().id(createButtonCustomId(ROLL_BUTTON_ID)).label("Roll").build()
                )).build(),
                ComponentRowDefinition.builder().buttonDefinitions(ImmutableList.of(
                        ButtonDefinition.builder().id(createButtonCustomId("+1")).label("+1").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-1")).label("-1").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+5")).label("+5").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("-5")).label("-5").build(),
                        ButtonDefinition.builder().id(createButtonCustomId("+10")).label("+10").build()
                )).build());
    }

    @Value
    protected static class Config implements IConfig {
        @Override
        public String toShortString() {
            return "[]";
        }

    }

    @Value
    static class State implements IState {
        @NonNull
        String buttonValue;
        @NonNull
        Map<String, Integer> diceSetMap;

        @Override
        public String toShortString() {
            return String.format("[%s, %s]", buttonValue, diceSetMap);
        }
    }
}
