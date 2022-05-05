package de.janno.discord.connector.jda;

import de.janno.discord.connector.api.message.ComponentRowDefinition;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.entities.DataMessage;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;

import java.util.List;
import java.util.stream.Collectors;

public class MessageComponentConverter {

    public static Message messageComponent2MessageLayout(String content, List<ComponentRowDefinition> rows) {
        LayoutComponent[] layoutComponents = rows.stream()
                .map(c -> ActionRow.of(c.getButtonDefinitions().stream()
                        .map(b -> new ButtonImpl(b.getId(), b.getLabel(), ButtonStyle.fromKey(b.getStyle().getValue()), false, null))
                        .collect(Collectors.toList()))
                ).toArray(LayoutComponent[]::new);
        return new DataMessage(false, content, null, null, null, new String[0], new String[0],
                layoutComponents
        );

    }
}
