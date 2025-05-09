package de.janno.discord.bot.command;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import de.janno.discord.bot.BotEmojiUtil;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.connector.api.BottomCustomIdUtils;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;


import java.util.*;
import java.util.stream.Collectors;

public class ButtonHelper {

    private static final String LABEL_DELIMITER = "@";
    private static final String BUTTON_DELIMITER = ";";


    public static List<ButtonIdLabelAndDiceExpression> parseString(String buttons) {
        buttons = buttons.replace("\\n", "\n");
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
                        final String label = split[1].trim().replace("\n", " ");
                        final String expression = split[0].trim();
                        if (!Strings.isNullOrEmpty(expression) && !Strings.isNullOrEmpty(label)) {
                            final boolean directRoll;
                            String cleanLabel;
                            if (label.startsWith("!") && label.length() > 1) {
                                directRoll = true;
                                cleanLabel = label.substring(1);
                            } else {
                                cleanLabel = label;
                                directRoll = false;
                            }
                            BotEmojiUtil.LabelAndEmoji labelAndEmoji = BotEmojiUtil.splitLabel(cleanLabel);
                            builder.add(new ButtonIdLabelAndDiceExpression(idCounter++ + "_button", labelAndEmoji.labelWithoutLeadingEmoji(), expression, newLine, directRoll, labelAndEmoji.emoji()));
                            newLine = false;
                        }
                    }
                } else {
                    final String label = button.trim().replace("\n", " ");
                    final String expression = button.trim();
                    if (!Strings.isNullOrEmpty(expression) && !Strings.isNullOrEmpty(label)) {
                        builder.add(new ButtonIdLabelAndDiceExpression(idCounter++ + "_button", label, expression, newLine, false, null));
                        newLine = false;
                    }
                }

            }
        }
        return builder.build();
    }

    public static List<ComponentRowDefinition> createButtonLayoutDetail(String commandId, UUID configUUID, List<ButtonIdLabelAndDiceExpressionExtension> buttons) {
        final List<ComponentRowDefinition> rows = new ArrayList<>();
        List<ButtonDefinition> currentRow = new ArrayList<>();
        for (ButtonIdLabelAndDiceExpressionExtension button : buttons) {
            if (currentRow.size() == 5 || (button.buttonIdLabelAndDiceExpression.isNewLine() && !currentRow.isEmpty())) {
                rows.add(ComponentRowDefinition.builder().componentDefinitions(currentRow).build());
                currentRow = new ArrayList<>();
            }
            final ButtonDefinition.Style style;
            if (button.style != null) {
                style = button.style;
            } else {
                style = button.buttonIdLabelAndDiceExpression.isDirectRoll() ? ButtonDefinition.Style.SUCCESS : ButtonDefinition.Style.PRIMARY;
            }
            currentRow.add(ButtonDefinition.builder()
                    .id(BottomCustomIdUtils.createButtonCustomId(commandId, button.buttonIdLabelAndDiceExpression.getButtonId(), configUUID))
                    .label(button.buttonIdLabelAndDiceExpression.getLabel())
                    .style(style)
                    .disabled(button.disabled)
                    .emoji(button.buttonIdLabelAndDiceExpression.getEmoji())
                    .build());
        }
        if (!currentRow.isEmpty()) {
            rows.add(ComponentRowDefinition.builder().componentDefinitions(currentRow).build());
        }

        return rows;
    }

    public static List<ComponentRowDefinition> createButtonLayout(String commandId, UUID configUUID, List<ButtonIdLabelAndDiceExpression> buttons) {
        return createButtonLayoutDetail(commandId, configUUID, buttons.stream().map(b -> new ButtonIdLabelAndDiceExpressionExtension(b, false, null)).toList());
    }

    public static List<ComponentRowDefinition> extendButtonLayout(List<ComponentRowDefinition> current, List<ButtonDefinition> additionalButtonDefinitions, boolean newLine) {
        final List<List<ComponentDefinition>> rows = current.stream()
                .map(r -> (List<ComponentDefinition>) new ArrayList<>(r.getComponentDefinitions()))
                .collect(Collectors.toList());
        List<ComponentDefinition> currentRow;
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
                .map(r -> ComponentRowDefinition.builder().componentDefinitions(r).build())
                .toList();
    }

    public static Optional<String> valdiate(String buttons, Locale userLocale, List<String> extraButtonIds, boolean extraLine) {
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
            Optional<String> validateLabel = DiceEvaluatorAdapter.validateLabel(button, userLocale);
            if (validateLabel.isPresent()) {
                return validateLabel;
            }
        }
        if (extraLine && !extraButtonIds.isEmpty()) {
            rows.addAll(Lists.partition(extraButtonIds, 5));
        } else if (!extraButtonIds.isEmpty()) {
            for (String button : extraButtonIds) {
                if (currentRow.size() == 5 || button.isBlank()) {
                    currentRow = new ArrayList<>();
                    rows.add(currentRow);
                }
                if (!button.isBlank()) {
                    currentRow.add(button);
                }
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

    public record ButtonIdLabelAndDiceExpressionExtension(ButtonIdLabelAndDiceExpression buttonIdLabelAndDiceExpression,
                                                          boolean disabled, ButtonDefinition.Style style) {
    }
}
