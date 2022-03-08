package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.api.Answer;
import de.janno.discord.api.ISlashEventAdaptor;
import de.janno.discord.api.Requester;
import de.janno.discord.command.slash.CommandInteractionOption;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DirectRollCommandTest {
    DirectRollCommand underTest;
    IDice diceMock;

    @BeforeEach
    void setup() {
        diceMock = Mockito.mock(IDice.class);
        underTest = new DirectRollCommand(new DiceParserHelper(diceMock));
    }


    @Test
    void handleComponentInteractEvent() {
        ISlashEventAdaptor slashEventAdaptor = mock(ISlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6")
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
        verify(slashEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3", "[3]", ImmutableList.of())));

        verify(slashEventAdaptor, never()).getChannelId();
    }

}