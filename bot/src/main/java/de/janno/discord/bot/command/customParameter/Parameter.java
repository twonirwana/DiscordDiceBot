package de.janno.discord.bot.command.customParameter;

import lombok.Value;

import java.util.List;

@Value
public class Parameter {
    //parameter with {} and range
    String parameterExpression;
    String name;
    List<ParameterOption> parameterOptions;

    @Override
    public String toString() {
        return "%s of %s".formatted(name, parameterOptions);
    }

    @Value
    public static class ParameterOption {
        String value;
        String label;
        String id;

        @Override
        public String toString() {
            return "%s@%s".formatted(value, label);
        }
    }
}
