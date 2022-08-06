package de.janno.discord.bot.command.customParameter;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.State;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static de.janno.discord.bot.command.customParameter.CustomParameterCommand.*;

@EqualsAndHashCode(callSuper = true)
@Getter
public class CustomParameterState extends State {
    static final String SELECTED_PARAMETER_DELIMITER = "\t";
    @NonNull
    List<String> selectedParameterValues;
    @NonNull
    @EqualsAndHashCode.Exclude
    String filledExpression;
    @EqualsAndHashCode.Exclude
    String currentParameterExpression; //null if expression is complete
    @EqualsAndHashCode.Exclude
    String currentParameterName; //null if expression is complete
    @Nullable
    String lockedForUserName;

    public CustomParameterState(@NonNull String[] customIdComponents, String messageContent, String invokingUser) {
        super(customIdComponents[CustomIdIndex.BUTTON_VALUE.index]);
        String baseString = customIdComponents[CustomIdIndex.BASE_EXPRESSION.index];
        String alreadySelectedParameter = customIdComponents[CustomIdIndex.SELECTED_PARAMETER.index];

        if (CLEAR_BUTTON_ID.equals(getButtonValue())) {
            this.selectedParameterValues = ImmutableList.of();
            this.lockedForUserName = null;
        } else {
            ImmutableList.Builder<String> selectedParameterBuilder =
                    ImmutableList.<String>builder()
                            .addAll(Arrays.stream(alreadySelectedParameter.split(SELECTED_PARAMETER_DELIMITER))
                                    .filter(s -> !Strings.isNullOrEmpty(s))
                                    .collect(Collectors.toList()));
            this.lockedForUserName = Optional.ofNullable(getUserNameFromMessage(messageContent)).orElse(invokingUser);
            if (lockedForUserName == null || lockedForUserName.equals(invokingUser)) {
                selectedParameterBuilder.add(getButtonValue());
            }
            this.selectedParameterValues = selectedParameterBuilder.build();
        }
        this.filledExpression = getFilledExpression(baseString, selectedParameterValues);

        this.currentParameterExpression = hasMissingParameter(filledExpression) ? getNextParameterExpression(filledExpression) : null;

        this.currentParameterName = currentParameterExpression != null ? cleanupExpressionForDisplay(currentParameterExpression) : null;
    }

    private static boolean hasMissingParameter(@NonNull String expression) {
        return PARAMETER_VARIABLE_PATTERN.matcher(expression).find();
    }

    private static String cleanupExpressionForDisplay(String expression) {
        return expression
                .replaceAll(RANGE_REPLACE_REGEX, "")
                .replace("{", "*{")
                .replace("}", "}*");
    }

    private static @NonNull String getNextParameterExpression(@NonNull String expression) {
        Matcher matcher = PARAMETER_VARIABLE_PATTERN.matcher(expression);
        if (matcher.find()) {
            return matcher.group(0);
        }
        throw new IllegalStateException(String.format("Expression '%s' missing a parameter definition like {name}", expression));
    }

    public boolean hasMissingParameter() {
        return hasMissingParameter(filledExpression);
    }

    private String getUserNameFromMessage(@NonNull String messageContent) {
        if (messageContent.contains(LOCKED_USER_NAME_DELIMITER)) {
            return messageContent.split(LOCKED_USER_NAME_DELIMITER)[0];
        }
        return null;
    }

    public Collection<CustomIdIndexWithValue> getIdComponents() {
        return ImmutableList.of(
                new CustomIdIndexWithValue(CustomIdIndex.SELECTED_PARAMETER, String.join(SELECTED_PARAMETER_DELIMITER, selectedParameterValues))
        );
    }

    private String getFilledExpression(String baseExpression, List<String> selectedParameterValues) {
        String filledExpression = baseExpression;
        for (String parameterValue : selectedParameterValues) {
            String nextParameterName = getNextParameterExpression(filledExpression);
            filledExpression = filledExpression.replace(nextParameterName, parameterValue);
        }
        return filledExpression;
    }

    public String filledExpressionWithoutRange() {
        return cleanupExpressionForDisplay(filledExpression);
    }

    @Override
    public String toShortString() {
        return ImmutableList.<String>builder()
                .addAll(selectedParameterValues)
                .add(Optional.ofNullable(lockedForUserName).orElse(""))
                .build().toString();
    }
}
