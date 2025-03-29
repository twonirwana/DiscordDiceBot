package de.janno.discord.connector.jda;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import de.janno.discord.connector.api.message.DropdownDefinition;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

@ExtendWith(SnapshotExtension.class)
class MessageComponentConverterTest {

    Expect expect;

    @Test
    void messageComponent2MessageLayout() {
        MessageCreateData messageCreateData = MessageComponentConverter.messageComponent2MessageLayout("welcome", List.of(
                ComponentRowDefinition.builder().componentDefinition(ButtonDefinition.builder()
                        .label("label")
                        .id("id")
                        .emoji("\uD83D\uDE01")
                        .disabled(true)
                        .style(ButtonDefinition.Style.SECONDARY)
                        .build()).build(),
                ComponentRowDefinition.builder().componentDefinition(DropdownDefinition.builder()
                        .id("id")
                        .disabled(true)
                        .maxValues(1)
                        .minValues(1)
                        .placeholder("placeholder")
                        .options(List.of(DropdownDefinition.DropdownOption.builder()
                                .description("description")
                                .emoji("\uD83D\uDE01")
                                .isDefault(true)
                                .label("label")
                                .value("value")
                                .build()))
                        .build()).build()
        ));
        expect.toMatchSnapshot(messageCreateData);
    }
}