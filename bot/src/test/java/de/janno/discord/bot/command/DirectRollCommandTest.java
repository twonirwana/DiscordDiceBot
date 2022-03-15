package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.command.DirectRollCommand;
import de.janno.discord.connector.api.Answer;
import de.janno.discord.connector.api.ISlashEventAdaptor;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.discord.bot.dice.DiceParserHelper;
import de.janno.discord.bot.dice.IDice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class DirectRollCommandTest {
    DirectRollCommand underTest;
    IDice diceMock;

    @BeforeEach
    void setup() {
        diceMock = mock(IDice.class);
        underTest = new DirectRollCommand(new DiceParserHelper(diceMock));
    }


    @Test
    void handleComponentInteractEvent() {
        ISlashEventAdaptor slashEventAdaptor = mock(ISlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@Test Label")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(slashEventAdaptor.getChannelId()).thenReturn(1L);
        when(slashEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.reply(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:1d6");
        when(slashEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).reply("/r expression:1d6");
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(2)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any(), any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor, never()).replyEphemeral(any());
        verify(slashEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new Answer("Test Label: 1d6 = 3", "[3]", ImmutableList.of())));

        verify(slashEventAdaptor, never()).getChannelId();
    }

    @Test
    void handleComponentInteractEvent_validationFailed() {
        ISlashEventAdaptor slashEventAdaptor = mock(ISlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("asdfasdf")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(diceMock.roll("asdfasdf")).thenThrow(new IllegalArgumentException("not a valid expression"));
        when(slashEventAdaptor.reply(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:asdfasdf");

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);

        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any(), any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor, never()).replyEphemeral(any());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor).reply("/r expression:asdfasdf\n" +
                "The following dice expression are invalid: 'asdfasdf'. Use `/r help` to get more information on how to use the command.");

        verify(slashEventAdaptor, never()).getChannelId();
    }

    @Test
    void handleComponentInteractEvent_help() {
        ISlashEventAdaptor slashEventAdaptor = mock(ISlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("help")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(slashEventAdaptor.getChannelId()).thenReturn(1L);
        when(slashEventAdaptor.replyEphemeral(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:help");
        when(slashEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);


        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any(), any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor).replyEphemeral(EmbedDefinition.builder()
                .description("Type /r and a dice expression e.g. `/r 1d6` \n" + DiceParserHelper.HELP)
                .build());

        verify(slashEventAdaptor, never()).getChannelId();
    }

}