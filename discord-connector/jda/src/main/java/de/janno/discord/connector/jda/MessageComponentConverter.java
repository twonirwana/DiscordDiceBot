package de.janno.discord.connector.jda;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.DropdownDefinition;
import net.dv8tion.jda.api.components.MessageTopLevelComponent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.components.buttons.ButtonImpl;
import net.dv8tion.jda.internal.components.selections.StringSelectMenuImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MessageComponentConverter {

    public static MessageCreateData messageComponent2MessageLayout(String content, List<ComponentRowDefinition> rows) {
        return new MessageCreateBuilder()
                .useComponentsV2(false)
                .addContent(content)
                .addComponents(componentRowDefinition2LayoutComponent(rows))
                .setSuppressedNotifications(true)
                .build();
    }

    public static List<? extends MessageTopLevelComponent> componentRowDefinition2LayoutComponent(List<ComponentRowDefinition> rows) {
        final AtomicInteger selectMenuUniqueId = new AtomicInteger(0); //todo could be null or 0?
        Set<String> invalidIds = rows.stream()
                .flatMap(r -> r.getComponentDefinitions().stream())
                .map(ComponentDefinition::getId)
                .filter(id -> id.length() > 100 || Strings.isNullOrEmpty(id))
                .collect(Collectors.toSet());
        Preconditions.checkArgument(invalidIds.isEmpty(), String.format("The following ids are invalid: %s", invalidIds));
        return rows.stream()
                .map(c -> ActionRow.of(
                        c.getComponentDefinitions().stream()
                                .map(cd -> mapComponentDefinition2ItemComponent(cd, selectMenuUniqueId))
                                .toList())
                ).toList();
    }

    private static ActionRowChildComponent mapComponentDefinition2ItemComponent(ComponentDefinition componentDefinition, AtomicInteger selectMenuUniqueId) {
        if (componentDefinition instanceof DropdownDefinition d) {
            return new StringSelectMenuImpl(d.getId(), selectMenuUniqueId.incrementAndGet(), d.getPlaceholder(), d.getMinValues(), d.getMaxValues(), d.isDisabled(), dropdownOptions2SelectOptions(d.getOptions()), true);
        } else if (componentDefinition instanceof ButtonDefinition b) {
            return new ButtonImpl(b.getId(), b.getLabel(), ButtonStyle.fromKey(b.getStyle().getValue()), b.isDisabled(),
                    Optional.ofNullable(b.getEmoji())
                            .map(String::trim)
                            .map(Strings::emptyToNull)
                            .map(Emoji::fromFormatted).orElse(null));
        }
        throw new IllegalStateException("Unhandled componentDefinition: " + componentDefinition);
    }

    private static List<SelectOption> dropdownOptions2SelectOptions(List<DropdownDefinition.DropdownOption> options) {
        return options.stream()
                .map(d -> SelectOption.of(d.getLabel(), d.getValue())
                        .withDescription(d.getDescription())
                        .withDefault(d.isDefault())
                        .withEmoji(Optional.ofNullable(d.getEmoji())
                                .map(String::trim)
                                .map(Strings::emptyToNull)
                                .map(Emoji::fromFormatted).orElse(null)))
                .toList();
    }
}
