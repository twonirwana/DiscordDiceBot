package de.janno.discord.bot.command.customParameter;

import lombok.Value;

import java.util.List;

@Value
public class Parameter {
    //parameter with {} and range
    String parameterExpression;
    String name;
    List<ValueAndLabel> parameterOptions;

    @Value
    public static class ValueAndLabel{
        String value;
        String label;
    }
}
