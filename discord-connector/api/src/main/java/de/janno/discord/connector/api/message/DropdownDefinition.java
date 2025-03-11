package de.janno.discord.connector.api.message;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class DropdownDefinition implements ComponentDefinition {
    //todo validate
    String id;
    String placeholder;
    int minValues;
    int maxValues;
    boolean disabled;
    List<DropdownOption> options;

    @Override
    public String getLabelOrPlaceholder() {
        return placeholder;
    }

    @Data
    @Builder
    public static class DropdownOption {
        String label;
        @NonNull
        //todo validate 1 and 100 in length
        String value;
        String description;
        boolean isDefault;
        String emoji;
    }
}
