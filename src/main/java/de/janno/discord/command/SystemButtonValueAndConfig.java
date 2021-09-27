package de.janno.discord.command;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SystemButtonValueAndConfig {
    private String systemId;
    private String buttonValue;
    private List<String> configParameter;
    private String delimiter = ",";

    public SystemButtonValueAndConfig(String eventValue) {
        String[] split = eventValue.split(",");
        systemId = split[0];
        buttonValue = split[1];
        configParameter = new ArrayList<>();
        for (int i = 2; i < split.length; i++) {
            configParameter.add(split[i]);
        }
    }
}
