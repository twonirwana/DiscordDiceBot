package de.janno.discord.connector.jda;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MessageComponentConverter {

    public static MessageCreateData messageComponent2MessageLayout(String content, List<ComponentRowDefinition> rows) {
        LayoutComponent[] layoutComponents = componentRowDefinition2LayoutComponent(rows);
        return new MessageCreateBuilder()
                .addContent(content)
                .addComponents(layoutComponents)
                .setSuppressedNotifications(true)
                .build();
    }

    public static LayoutComponent[] componentRowDefinition2LayoutComponent(List<ComponentRowDefinition> rows) {
        Set<String> invalidIds = rows.stream()
                .flatMap(r -> r.getButtonDefinitions().stream())
                .map(ButtonDefinition::getId)
                .filter(id -> id.length() > 100 || Strings.isNullOrEmpty(id))
                .collect(Collectors.toSet());
        Preconditions.checkArgument(invalidIds.isEmpty(), String.format("The following ids are invalid: %s", invalidIds));
        return rows.stream()
                .map(c -> ActionRow.of(c.getButtonDefinitions().stream()
                        .map(b -> new ButtonImpl(b.getId(), b.getLabel(), ButtonStyle.fromKey(b.getStyle().getValue()), b.isDisabled(),
                                Optional.ofNullable(b.getEmoji())
                                        .map(String::trim)
                                        .map(Strings::emptyToNull)
                                        .map(Emoji::fromFormatted).orElse(null)))
                        .toList())
                ).toArray(LayoutComponent[]::new);
    }
}
