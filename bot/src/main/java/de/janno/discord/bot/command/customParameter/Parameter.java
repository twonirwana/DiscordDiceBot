package de.janno.discord.bot.command.customParameter;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
public class Parameter {

    //parameter with {} and range
    @NonNull String parameterExpression;
    @NonNull String name;
    @NonNull List<ParameterOption> parameterOptions;

    public Parameter(@NonNull String parameterExpression, @NonNull String name, @NonNull List<ParameterOption> parameterOptions) {
        this.parameterExpression = parameterExpression;
        this.name = name.replace("\n", " ");
        this.parameterOptions = parameterOptions;
    }

    @Override
    public String toString() {
        return "%s of %s".formatted(name, parameterOptions);
    }

    public record ParameterOption(@NonNull String value, @NonNull String label, @NonNull String id) {

        public ParameterOption(@NonNull String value, @NonNull String label, @NonNull String id) {
            this.value = value;
            this.label = label.replace("\n", " ");
            this.id = id;
        }

        @Override
        public String toString() {
            return "%s@%s".formatted(value, label);
        }
    }
}
