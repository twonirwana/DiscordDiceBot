package de.janno.discord.connector.api;

import lombok.Value;

import java.util.List;

@Value
public class AutoCompleteRequest {
    String focusedOptionName;
    String focusedOptionValue;
    List<OptionValue> optionValues;

}
