package de.janno.discord.bot.command.customParameter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.State;
import de.janno.discord.bot.command.channelConfig.AliasHelper;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.PersistenceManager;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.*;

@Slf4j
public class CustomParameterValidator {
    private static final int MAX_PATH_DEPTH = 4;

    public static Optional<String> validateStates(CustomParameterConfig config, long channelId, long userId,
                                                  PersistenceManager persistenceManager,
                                                  DiceEvaluatorAdapter diceEvaluatorAdapter) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<StateWithCustomIdAndParameter> possibleStatePermutations = statePermutations(config);
        if (possibleStatePermutations.stream().anyMatch(s -> s.depth() > MAX_PATH_DEPTH)) {
            return Optional.of(I18n.getMessage("custom_parameter.validation.variable.count.max.four", config.getConfigLocale()));
        }

        Optional<String> result = possibleStatePermutations.parallelStream()
                .map(s -> validateStateWithCustomIdAndParameter(config, s, channelId, userId, persistenceManager, diceEvaluatorAdapter))
                .filter(Objects::nonNull)
                .findFirst();
        log.debug("{} with parameter options {} in {}ms validated",
                config.getBaseExpression().replace("\n", " "),
                config.getParameters(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return result;
    }

    private static List<StateWithCustomIdAndParameter> statePermutations(CustomParameterConfig config) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        String parameterExpression = getNextParameterExpression(config.getBaseExpression());

        Optional<Parameter> currentParameter = getParameterForParameterExpression(config, parameterExpression);
        List<ButtonLabelAndId> buttonLabelAndIds = filterToCornerCases(getButtons(config, parameterExpression), currentParameter.map(Parameter::getParameterOptions).orElse(List.of()));

        for (ButtonLabelAndId buttonLabelAndId : buttonLabelAndIds) {
            State<CustomParameterStateData> nextState = new State<>(buttonLabelAndId.id(), updateState(null, config, buttonLabelAndId.id(), null, "test"));
            out.add(new StateWithCustomIdAndParameter(buttonLabelAndId.id(), nextState, buttonLabelAndIds, 1));
            out.addAll(getCornerStatePermutations(config, nextState, 2));
        }
        return out;
    }

    private static String validateStateWithCustomIdAndParameter(CustomParameterConfig config,
                                                                StateWithCustomIdAndParameter aState,
                                                                long channelId,
                                                                long userId,
                                                                PersistenceManager persistenceManager,
                                                                DiceEvaluatorAdapter diceEvaluatorAdapter) {
        if (!hasMissingParameter(aState.state())) {
            final String expression = getFilledExpression(config, aState.state());
            final String label = getLabel(config, aState.state());
            final String expressionWithoutSuffixLabel = removeSuffixLabelFromExpression(expression, label);
            final String expressionWithoutSuffixLabelAndAlias = AliasHelper.getAndApplyAliaseToExpression(channelId, userId, persistenceManager, expressionWithoutSuffixLabel);

            Optional<String> validationMessage = diceEvaluatorAdapter.validateDiceExpressionWitOptionalLabel(expressionWithoutSuffixLabelAndAlias,
                    "/%s %s".formatted(I18n.getMessage("custom_parameter.name", config.getConfigLocale()), I18n.getMessage("base.option.help", config.getConfigLocale())),
                    config.getConfigLocale());
            if (validationMessage.isPresent()) {
                return validationMessage.get();
            }
        }
        if (hasMissingParameter(aState.state()) && getParameterForParameterExpression(config, getCurrentParameterExpression(aState.state()).orElse(null))
                .map(Parameter::getParameterOptions)
                .map(List::isEmpty)
                .orElse(true)) {
            return I18n.getMessage("custom_parameter.validation.invalid.parameter.option", config.getConfigLocale(), getCurrentParameterExpression(aState.state()).orElse(""));
        }
        return null;
    }

    private static List<StateWithCustomIdAndParameter> getCornerStatePermutations(CustomParameterConfig config, State<CustomParameterStateData> state, int depth) {
        List<StateWithCustomIdAndParameter> out = new ArrayList<>();
        Optional<String> parameterExpression = getCurrentParameterExpression(state);

        if (parameterExpression.isPresent()) {

            Optional<Parameter> currentParameter = getParameterForParameterExpression(config, parameterExpression.get());
            List<ButtonLabelAndId> parameterValues = filterToCornerCases(getButtons(config, parameterExpression.get()), currentParameter.map(Parameter::getParameterOptions).orElse(List.of()));

            for (ButtonLabelAndId parameterValue : parameterValues) {
                State<CustomParameterStateData> nextState = new State<>(parameterValue.id(),
                        updateState(Optional.ofNullable(state.getData()).map(CustomParameterStateData::getSelectedParameterValues).orElse(List.of()), config,
                                parameterValue.id(), null, "test"));
                out.add(new StateWithCustomIdAndParameter(parameterValue.id(), nextState, parameterValues, depth));
                //do one more than max so we see it in the results
                if (depth <= MAX_PATH_DEPTH) {
                    out.addAll(getCornerStatePermutations(config, nextState, depth + 1));
                }

            }
        }
        return out;
    }

    @VisibleForTesting
    static List<ButtonLabelAndId> filterToCornerCases(List<ButtonLabelAndId> in, List<Parameter.ParameterOption> parameterOptions) {

        List<ButtonLabelIdValueAndNextPath> withValue = in.stream()
                .map(b -> getValueFromId(b, parameterOptions)
                        .map(s -> new ButtonLabelIdValueAndNextPath(s, getNextPathFromId(b, parameterOptions).orElse(Parameter.NO_PATH), b))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
        return Stream.of(
                        getMaxNumeric(withValue).stream(),
                        getMinNumeric(withValue).stream(),
                        getZero(withValue).stream(),
                        allNoneNumeric(withValue).stream(),
                        getDirectRolls(withValue).stream(),
                        allWithPath(withValue).stream()
                )
                .flatMap(Function.identity())
                .map(ButtonLabelIdValueAndNextPath::buttonLabelAndId)
                .distinct()
                .toList();
    }

    private static Optional<String> getValueFromId(ButtonLabelAndId buttonLabelAndId, List<Parameter.ParameterOption> parameterOptions) {
        return parameterOptions.stream()
                .filter(po -> Objects.equals(buttonLabelAndId.id(), po.id()))
                .map(Parameter.ParameterOption::value)
                .findFirst();
    }

    private static Optional<String> getNextPathFromId(ButtonLabelAndId buttonLabelAndId, List<Parameter.ParameterOption> parameterOptions) {
        return parameterOptions.stream()
                .filter(po -> Objects.equals(buttonLabelAndId.id(), po.id()))
                .map(Parameter.ParameterOption::nextPathId)
                .findFirst();
    }

    private static List<ButtonLabelIdValueAndNextPath> getDirectRolls(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> bv.buttonLabelAndId().directRoll())
                .toList();
    }

    private static Optional<ButtonLabelIdValueAndNextPath> getMaxNumeric(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> isNumber(bv.value()))
                .max(Comparator.comparing(buttonLabelAndValue -> Long.parseLong(buttonLabelAndValue.value())));
    }

    private static Optional<ButtonLabelIdValueAndNextPath> getMinNumeric(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> isNumber(bv.value()))
                .min(Comparator.comparing(buttonLabelAndValue -> Long.parseLong(buttonLabelAndValue.value())));
    }

    private static List<ButtonLabelIdValueAndNextPath> getZero(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> Objects.equals(StringUtils.trim(bv.value()), "0"))
                .toList();
    }

    private static List<ButtonLabelIdValueAndNextPath> allNoneNumeric(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> !isNumber(bv.value()))
                .toList();
    }

    private static List<ButtonLabelIdValueAndNextPath> allWithPath(List<ButtonLabelIdValueAndNextPath> in) {
        return in.stream()
                .filter(bv -> !isNumber(bv.value()))
                .toList();
    }

    private static boolean isNumber(String in) {
        try {
            Long.parseLong(in);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    static List<ButtonLabelAndId> getButtons(CustomParameterConfig config, String parameterExpression) {
        return getParameterForParameterExpression(config, parameterExpression)
                .map(Parameter::getParameterOptions).orElse(List.of()).stream()
                .map(vl -> new ButtonLabelAndId(vl.label(), vl.id(), vl.directRoll()))
                .toList();
    }

    private record StateWithCustomIdAndParameter(@NonNull String customId,
                                                 @NonNull State<CustomParameterStateData> state,
                                                 @NonNull List<ButtonLabelAndId> buttonIdLabelAndDiceExpressions,
                                                 int depth) {
    }

    private record ButtonLabelIdValueAndNextPath(@NonNull String value, @NonNull String nextPathFromId,
                                                 @NonNull ButtonLabelAndId buttonLabelAndId) {
    }


    record ButtonLabelAndId(@NonNull String label, @NonNull String id, boolean directRoll) {
    }


}
