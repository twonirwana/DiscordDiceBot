package de.janno.discord.bot.command.customParameter;

import lombok.Value;

import java.util.List;

@Value
public class Parameter {
    //parameter with {} and range
    String parameterExpression;
    String name;
    List<ParameterOption> parameterOptions;

    @Value
    public static class ParameterOption {
        String value;
        String label;
        String id;
    }
}
