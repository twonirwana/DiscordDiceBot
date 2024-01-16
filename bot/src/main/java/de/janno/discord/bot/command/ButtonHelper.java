package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ButtonHelper {

    private static final String LABEL_DELIMITER = "@";

    public static List<ButtonIdLabelAndDiceExpression> parseString(String buttons) {
        ImmutableList.Builder<ButtonIdLabelAndDiceExpression> builder = ImmutableList.builder();
        int idCounter = 1;
        boolean newLine = false;
        for (String button : buttons.split(";")) {
            if (button.isBlank()) {
                newLine = true;
            } else {
                final String id = idCounter++ + "_button";
                final String expression;
                final String label;
                if (StringUtils.countMatches(button, LABEL_DELIMITER) == 1) {
                    String[] split = button.split(LABEL_DELIMITER);
                    label = split[1].trim();
                    expression = split[0].trim();
                } else {
                    label = button.trim();
                    expression = button.trim();
                }
                builder.add(new ButtonIdLabelAndDiceExpression(id, label, expression, newLine));
                newLine = false;
            }
        }
        return builder.build();
    }

    public static List<ComponentRowDefinition> createButtonLayout(String commandId, UUID configUUID, List<ButtonIdLabelAndDiceExpression> buttons) {
        final List<ComponentRowDefinition> rows = new ArrayList<>();
        List<ButtonDefinition> currentRow = new ArrayList<>();
        for (ButtonIdLabelAndDiceExpression button : buttons) {
            if (currentRow.size() == 5 || (button.isNewLine() && !currentRow.isEmpty())) {
                rows.add(ComponentRowDefinition.builder().buttonDefinitions(currentRow).build());
                currentRow = new ArrayList<>();
            }
            currentRow.add(ButtonDefinition.builder()
                    .id(BottomCustomIdUtils.createButtonCustomId(commandId, button.getButtonId(), configUUID))
                    .label(button.getLabel())
                    .build());
        }
        if (!currentRow.isEmpty()) {
            rows.add(ComponentRowDefinition.builder().buttonDefinitions(currentRow).build());
        }

        return rows;
    }

    public static List<ComponentRowDefinition> extendButtonLayout(List<ComponentRowDefinition> current, List<ButtonDefinition> additionalButtonDefinitions, boolean newLine) {
        final List<List<ButtonDefinition>> rows = current.stream()
                .map(r -> (List<ButtonDefinition>) new ArrayList<>(r.getButtonDefinitions()))
                .collect(Collectors.toList());
        List<ButtonDefinition> currentRow;
        if (rows.size() == 5 || newLine) {
            currentRow = new ArrayList<>();
            rows.add(currentRow);
        } else {
            currentRow = rows.getLast();
        }
        for (ButtonDefinition button : additionalButtonDefinitions) {
            if (currentRow.size() == 5) {
                currentRow = new ArrayList<>();
                rows.add(currentRow);
            }
            currentRow.add(button);
        }

        return rows.stream()
                .map(r -> ComponentRowDefinition.builder().buttonDefinitions(r).build())
                .toList();
    }
}
