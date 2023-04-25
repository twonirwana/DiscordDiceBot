package de.janno.discord.bot.command.directRoll;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.channelConfig.ChannelConfigCommand;
import de.janno.discord.bot.command.channelConfig.DirectRollConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class DirectRollCommandTest {
    DirectRollCommand underTest;

    @BeforeEach
    void setup() {
        underTest = new DirectRollCommand(mock(PersistenceManager.class), new CachingDiceEvaluator((minExcl, maxIncl) -> 1, 1000, 0));
    }


    @Test
    void handleComponentInteractEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@Test Label")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(slashEventAdaptor.getChannelId()).thenReturn(1L);
        when(slashEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.deleteMessageById(anyLong())).thenReturn(Mono.empty());
        when(slashEventAdaptor.acknowledgeAndRemoveSlash()).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:1d6");
        when(slashEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));


        StepVerifier.create(res)
                .verifyComplete();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor, never()).reply(any(), anyBoolean());
        verify(slashEventAdaptor).acknowledgeAndRemoveSlash();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor, never()).replyEmbed(any(), anyBoolean());
        verify(slashEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new EmbedOrMessageDefinition("Test Label ⇒ 1", "1d6", ImmutableList.of(), new File("imageCache//polyhedral_3d_red_and_white//cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png"), EmbedOrMessageDefinition.Type.EMBED)));

        verify(slashEventAdaptor, times(2)).getChannelId();
    }

    @Test
    void handleComponentInteractEvent_validationFailed() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("asdfasdf")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:asdfasdf");
        when(slashEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));

        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor, never()).replyEmbed(any(), anyBoolean());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor).reply("/r expression:asdfasdf\n" +
                "The following expression is invalid: 'asdfasdf'. The error is: No matching operator for 'asdfasdf', non-functional text and value names must to be surrounded by '' or []. Use `/r expression:help` to get more information on how to use the command.", true);

        verify(slashEventAdaptor, times(1)).getChannelId();
    }

    @Test
    void handleComponentInteractEvent_help() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(slashEventAdaptor.getChannelId()).thenReturn(1L);
        when(slashEventAdaptor.replyEmbed(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:help");
        when(slashEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]"));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"));


        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor).replyEmbed(EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Type /r and a dice expression, configuration with /channel_config\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/r expression:1d6`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for Help and News", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);

        verify(slashEventAdaptor, never()).getChannelId();
    }

    @Test
    void getCommandId() {
        String res = underTest.getCommandId();
        assertThat(res).isEqualTo("r");
    }

    @Test
    void getCommandDefinition() {
        CommandDefinition res = underTest.getCommandDefinition();

        assertThat(res).isEqualTo(CommandDefinition.builder()
                .name("r")
                .description("direct roll of dice expression, configuration with /channel_config")
                .option(CommandDefinitionOption.builder()
                        .name("expression")
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build());
    }

    @Test
    void getConfigCommandDefinition() {
        CommandDefinition res = new ChannelConfigCommand(null).getCommandDefinition();

        assertThat(res.getOptions().stream().map(CommandDefinitionOption::getName)).containsExactlyInAnyOrder("save_direct_roll_config", "delete_direct_roll_config", "channel_alias", "user_channel_alias");
    }

    @Test
    void deserialization_config() {
        String configString = """
                ---
                answerTargetChannelId: null
                alwaysSumResult: false
                answerFormatType: "without_expression"
                resultImage: "polyhedral_3d_red_and_white"
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "r", "DirectRollConfig", configString);


        DirectRollConfig res = underTest.deserializeConfig(savedData);
        assertThat(res).isEqualTo(new DirectRollConfig(null, false, AnswerFormatType.without_expression, ResultImage.polyhedral_3d_red_and_white));

    }


}