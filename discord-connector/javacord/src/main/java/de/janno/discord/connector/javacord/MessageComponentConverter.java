package de.janno.discord.connector.javacord;

import de.janno.discord.connector.api.ComponentRowDefinition;
import org.javacord.api.entity.message.component.ActionRowBuilder;
import org.javacord.api.entity.message.component.ButtonBuilder;
import org.javacord.api.entity.message.component.HighLevelComponent;

import java.util.List;
import java.util.stream.Collectors;

public class MessageComponentConverter {
    public static HighLevelComponent[] messageComponent2MessageLayout(List<ComponentRowDefinition> rows) {
        return rows.stream().map(c -> new ActionRowBuilder()
                        .addComponents(c.getButtonDefinitions().stream()
                                .map(b -> new ButtonBuilder()
                                        .setCustomId(b.getId())
                                        .setLabel(b.getLabel())
                                        .setStyle(b.getStyle().getValue())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .toArray(HighLevelComponent[]::new);
    }
}
