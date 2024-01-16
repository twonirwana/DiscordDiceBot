package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Value
@Builder
public class ComponentDefinition {
    //todo use!
    @Singular
    List<ComponentRowDefinition> rows;

    public ComponentDefinition(@NonNull List<ComponentRowDefinition> rows) {
        this.rows = rows;
        Preconditions.checkArgument(rows.size() <= 5, "Too many rows in %s, max is 5", rows);
        List<String> duplicatedComponentId = rows.stream()
                .flatMap(r -> r.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        Preconditions.checkArgument(duplicatedComponentId.isEmpty(), "The following componentId are not unique: %s", duplicatedComponentId);
    }

}
