package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.cache.ButtonMessageCache;
import de.janno.discord.bot.dice.DiceParserHelper;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterCommand.Config, CustomParameterCommand.State> {

    private static final String COMMAND_NAME = "custom_parameter";

    private static final String EXPRESSION_OPTION = "expression";

    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final static Pattern variablePattern = Pattern.compile("\\Q{\\E.*?\\Q}\\E");

    private final DiceParserHelper diceParserHelper;

    public CustomParameterCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomParameterCommand(DiceParserHelper diceParserHelper) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceParserHelper = diceParserHelper;
    }

    private static String getCurrentParameter(String expression) {

        Matcher matcher = variablePattern.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException();
    }

    private static boolean hasMissingParameter(String expression) {
        return variablePattern.matcher(expression).find();
    }

    @Override
    protected @NonNull String getCommandDescription() {
        return "Roll dice and against a variable target";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/pool_target start' to get message, where the user can roll dice")
                .build();
    }

    @Override
    public String getName() {
        return COMMAND_NAME;
    }

    @Override
    protected List<CommandDefinitionOption> getStartOptions() {
        return ImmutableList.of(
                CommandDefinitionOption.builder()
                        .name(EXPRESSION_OPTION)
                        .required(true)
                        .description("Exprssion")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        if (hasMissingParameter(state.getCurrentExpression())) {
            return Optional.empty();
        }
        return Optional.of(diceParserHelper.roll(state.getCurrentExpression(), null));
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new Config(customIdSplit[1]);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new State(customIdSplit[2]);
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        String baseExpression = options.getStingSubOptionWithName(EXPRESSION_OPTION).orElse("");
        return new Config(baseExpression);
    }

    @Override
    protected MessageDefinition getButtonMessage(Config config) {
        String currentParameter = getCurrentParameter(config.getBaseExpression());
        return MessageDefinition.builder()
                .content(currentParameter)
                .componentRowDefinitions(createPoolButtonLayout(config))
                .build();
    }

    @Override
    protected Optional<MessageDefinition> getButtonMessageWithState(State state, Config config) {
        if (hasMissingParameter(state.getCurrentExpression())) {
            String parameterForNextMessage = getCurrentParameter(state.getCurrentExpression());
            return Optional.of(MessageDefinition.builder()
                    .content(parameterForNextMessage)
                    .componentRowDefinitions(getButtonLayoutWithState(state, config))
                    .build());
        }
        String parameterForNextMessage = getCurrentParameter(config.getBaseExpression());
        return Optional.of(MessageDefinition.builder()
                .content(parameterForNextMessage)
                .componentRowDefinitions(createPoolButtonLayout(config))
                .build());
    }

    private List<ComponentRowDefinition> getButtonLayoutWithState(State state, Config config) {
        List<ButtonDefinition> buttons = IntStream.range(1, 26)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(createButtonCustomId(String.valueOf(i), config, state))
                        .label(i + "")
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    String createButtonCustomId(@NonNull String buttonValue, @NonNull Config config, @Nullable State state) {
        Preconditions.checkArgument(!buttonValue.contains(BotConstants.CONFIG_DELIMITER));
        String stateExpression = state == null ? config.getBaseExpression() : state.getCurrentExpression();
        String parameter = getCurrentParameter(stateExpression);
        String newExpression = stateExpression.replace(parameter, buttonValue);

        return String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, config.getBaseExpression(), newExpression);

    }

    private List<ComponentRowDefinition> createPoolButtonLayout(Config config) {
        List<ButtonDefinition> buttons = IntStream.range(1, 26)
                .mapToObj(i -> ButtonDefinition.builder()
                        .id(createButtonCustomId(String.valueOf(i), config, null))
                        .label(i + "")
                        .build())
                .collect(Collectors.toList());
        return Lists.partition(buttons, 5).stream()
                .map(bl -> ComponentRowDefinition.builder().buttonDefinitions(bl).build())
                .collect(Collectors.toList());
    }

    @Override
    protected Optional<String> getStartOptionsValidationMessage(CommandInteractionOption options) {
        Config conf = getConfigFromStartOptions(options);
        return validate(conf);
    }

    @VisibleForTesting
    Optional<String> validate(Config config) {

        return Optional.empty();
    }

    @Value
    protected static class Config implements IConfig {
        String baseExpression;

        @Override
        public String toShortString() {
            return ImmutableList.of(baseExpression).toString();
        }
    }

    @Value
    static class State implements IState {
        String currentExpression;

        @Override
        public String toShortString() {
            return ImmutableList.of(currentExpression).toString();
        }
    }
}
