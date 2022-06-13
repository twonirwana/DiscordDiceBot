package de.janno.discord.bot.command;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
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
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class CustomParameterCommand extends AbstractCommand<CustomParameterCommand.Config, CustomParameterCommand.State> {

    //todo clear button, user lock, button range, message format, input field?, tests, config validation, doc

    private static final String COMMAND_NAME = "custom_parameter";

    private static final String EXPRESSION_OPTION = "expression";

    private static final ButtonMessageCache BUTTON_MESSAGE_CACHE = new ButtonMessageCache(COMMAND_NAME);
    private final static Pattern PARAMETER_VARIABLE_PATTERN = Pattern.compile("\\Q{\\E.*?\\Q}\\E");
    private final DiceParserHelper diceParserHelper;

    public CustomParameterCommand() {
        this(new DiceParserHelper());
    }

    @VisibleForTesting
    public CustomParameterCommand(DiceParserHelper diceParserHelper) {
        super(BUTTON_MESSAGE_CACHE);
        this.diceParserHelper = diceParserHelper;
    }


    @Override
    protected @NonNull String getCommandDescription() {
        return "Fill the parameter of a given dice expression and roll it when all parameter are provided";
    }

    @Override
    protected EmbedDefinition getHelpMessage() {
        return EmbedDefinition.builder()
                .description("Use '/custom_parameter start' and provide a dice expression with parameter variables with the format {parameter_name}")
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
                        .description("Expression")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build()
        );
    }

    @Override
    protected Optional<EmbedDefinition> getAnswer(State state, Config config) {
        if (!state.isExpressionComplete()) {
            return Optional.empty();
        }
        return Optional.of(diceParserHelper.roll(state.getFilledExpression(), null));
    }

    @Override
    protected Config getConfigFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new Config(customIdSplit[1]);
    }

    @Override
    protected State getStateFromEvent(IButtonEventAdaptor event) {
        String[] customIdSplit = event.getCustomId().split(BotConstants.CONFIG_SPLIT_DELIMITER_REGEX);

        return new State(customIdSplit);
    }

    @Override
    protected Config getConfigFromStartOptions(CommandInteractionOption options) {
        String baseExpression = options.getStringSubOptionWithName(EXPRESSION_OPTION).orElse("");
        return new Config(baseExpression);
    }

    @Override
    protected MessageDefinition createNewButtonMessage(Config config) {
        return MessageDefinition.builder()
                .content(String.format("%s: Please select value for %s", config.getBaseExpression(), config.getFirstParameterName()))
                .componentRowDefinitions(createPoolButtonLayout(config))
                .build();
    }


    @Override
    protected Optional<Long> getAnswerTargetChannelId(Config config) {
        //todo
        return Optional.empty();
    }

    @Override
    protected Optional<List<ComponentRowDefinition>> getCurrentMessageComponentChange(State state, Config config) {
        if (!state.isExpressionComplete()) {
            return Optional.of(getButtonLayoutWithState(state, config));
        }
        return Optional.empty();
    }

    @Override
    protected Optional<String> getCurrentMessageContentChange(State state, Config config) {
        if (!state.isExpressionComplete() && state.isHasSelectedParameter()) {
            return Optional.of(String.format("%s: Please select value for %s", state.getFilledExpression(), state.getCurrentParameterName()));
        }
        return Optional.empty();
    }

    @Override
    protected Optional<MessageDefinition> createNewButtonMessageWithState(State state, Config config) {
        if (!state.isExpressionComplete()) {
            return Optional.empty();
        }
        return Optional.of(MessageDefinition.builder()
                .content(String.format("%s: Please select value for %s", config.getBaseExpression(), config.getFirstParameterName()))
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

        String selectedParameterString = Optional.ofNullable(state).map(State::toIdString).orElse("");
        return String.join(BotConstants.CONFIG_DELIMITER, COMMAND_NAME, config.getBaseExpression(), selectedParameterString, buttonValue);
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
    @EqualsAndHashCode(callSuper = true)
    static class State extends CustomParameter implements IState {
        private static final String SELECTED_PARAMETER_DELIMITER = "\t";
        @NonNull
        List<String> selectedParameterValues;
        @NonNull
        String filledExpression;
        boolean expressionComplete;
        boolean hasSelectedParameter;

        String currentParameterExpression; //null if expression is complete

        String currentParameterName; //null if expression is complete


        public State(@NonNull String[] customIdComponents) {
            String baseString = customIdComponents[1];
            String alreadySelectedParameter = customIdComponents[2];
            String buttonValue = customIdComponents[3];

            this.selectedParameterValues = ImmutableList.<String>builder()
                    .addAll(Arrays.stream(alreadySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                            .filter(s -> !Strings.isNullOrEmpty(s))
                            .collect(Collectors.toList()))
                    .add(buttonValue)
                    .build();
            this.hasSelectedParameter = !selectedParameterValues.isEmpty();
            this.filledExpression = getFilledExpression(baseString);
            this.expressionComplete = !hasMissingParameter(filledExpression);
            this.currentParameterExpression = expressionComplete ? null : getCurrentParameterExpression(filledExpression);
            this.currentParameterName = expressionComplete ? null : removeBrackets(currentParameterExpression);
        }

        public String toIdString() {
            return String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues);
        }

        private String getFilledExpression(String baseExpression) {
            String filledExpression = baseExpression;
            for (String parameterValue : selectedParameterValues) {
                String nextParameterName = getCurrentParameterExpression(filledExpression);
                filledExpression = filledExpression.replace(nextParameterName, parameterValue);
            }
            return filledExpression;
        }

        @Override
        public String toShortString() {
            return selectedParameterValues.toString();
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    protected static class Config extends CustomParameter implements IConfig {
        @NonNull
        String baseExpression;
        @NonNull
        String firstParameterName;

        public Config(@NonNull String baseExpression) {
            this.baseExpression = baseExpression;
            this.firstParameterName = removeBrackets(getCurrentParameterExpression(baseExpression));
        }

        @Override
        public String toShortString() {
            return ImmutableList.of(baseExpression).toString();
        }
    }

    @EqualsAndHashCode
    private abstract static class CustomParameter {
        protected static String getCurrentParameterExpression(String expression) {
            Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
            if (matcher.find()) {
                return matcher.group(0);
            }
            throw new IllegalStateException();
        }

        protected static String removeBrackets(String input) {
            return input.replace("{", "").replace("}", "");
        }

        protected static boolean hasMissingParameter(String expression) {
            return PARAMETER_VARIABLE_PATTERN.matcher(expression).find();
        }
    }
}
