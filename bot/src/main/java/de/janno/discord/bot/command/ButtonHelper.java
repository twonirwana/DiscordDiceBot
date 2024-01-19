package de.janno.discord.bot.command;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.DiceSystemAdapter;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;

import java.util.*;
import java.util.stream.Collectors;

public class ButtonHelper {

    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_DELIMITER = ";";

    public static List<ButtonIdLabelAndDiceExpression> parseString(String buttons) {
        ImmutableList.Builder<ButtonIdLabelAndDiceExpression> builder = ImmutableList.builder();
        int idCounter = 1;
        boolean newLine = false;
        for (String button : buttons.split(BUTTON_DELIMITER)) {
            if (button.isBlank()) {
                newLine = true;
            } else {
                if (button.contains(LABEL_DELIMITER)) {
                    if (button.split(LABEL_DELIMITER).length == 2) {
                        String[] split = button.split(LABEL_DELIMITER);
                        final String label = split[1].trim();
                        final String expression = split[0].trim();
                        if (!Strings.isNullOrEmpty(expression) && !Strings.isNullOrEmpty(label)) {
                            builder.add(new ButtonIdLabelAndDiceExpression(idCounter++ + "_button", label, expression, newLine));
                            newLine = false;
                        }
                    }
                } else {
                    final String label = button.trim();
                    final String expression = button.trim();
                    if (!Strings.isNullOrEmpty(expression) && !Strings.isNullOrEmpty(label)) {
                        builder.add(new ButtonIdLabelAndDiceExpression(idCounter++ + "_button", label, expression, newLine));
                        newLine = false;
                    }
                }

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
        if (rows.isEmpty() || newLine) {
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

    public static Optional<String> valdiate(String buttons, Locale userLocale) {
        List<List<String>> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        rows.add(currentRow);
        String[] buttonsSplit = buttons.split(BUTTON_DELIMITER);
        if (buttonsSplit.length == 1) {
            if (buttonsSplit[0].isBlank()) {
                return Optional.of(I18n.getMessage("buttons.validation.noButtons", userLocale));
            }
        }
        for (String button : buttonsSplit) {
            if (currentRow.size() == 5 || button.isBlank()) {
                currentRow = new ArrayList<>();
                rows.add(currentRow);
            }
            if (!button.isBlank()) {
                currentRow.add(button);
            }
        }

        for (String button : rows.stream().flatMap(Collection::stream).toList()) {
            Optional<String> validateLabel = DiceSystemAdapter.validateLabel(button, userLocale);
            if (validateLabel.isPresent()) {
                return validateLabel;
            }
        }

        if (rows.size() > 5) {
            return Optional.of(I18n.getMessage("buttons.validation.toMany", userLocale));
        }

        for (List<String> row : rows) {
            if (row.size() > 5) {
                return Optional.of(I18n.getMessage("buttons.validation.toMany", userLocale));
            }
            if (row.isEmpty()) {
                return Optional.of(I18n.getMessage("buttons.validation.emptyRow", userLocale));
            }
        }


        return Optional.empty();
    }
}
