package de.janno.discord.bot.command.customParameter;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class Parameter {

    public static String NO_PATH = "";

    /**
     * parameter with {} and range
     */
    @NonNull
    String parameterExpression;
    @NonNull
    String name;

    @NonNull
    List<ParameterOption> parameterOptions;
    /**
     * The pathId of the parameter, the pathId musst match the nextPathId of the last selected parameterOption or this parameter will be skipped
     */
    @NonNull
    String pathId;

    @Override
    public String toString() {
        return "%s of %s".formatted(name, parameterOptions).replace("\n", " ");
    }

    /**
     * The nextPathId filters the next parameter, all parameter with a different pathId will be skipped
     */
    public record ParameterOption(@NonNull String value,
                                  @NonNull String label,
                                  @NonNull String id,
                                  boolean directRoll,
                                  @NonNull String nextPathId) {
        @Override
        public String toString() {
            return "%s@%s".formatted(value, label).replace("\n", " ");
        }
    }
}
