package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.api.Answer;
import de.janno.discord.api.ISlashEventAdaptor;
import de.janno.discord.api.Requester;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.Dice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandInteractionOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
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

        ApplicationCommandInteractionOption option = new ApplicationCommandInteractionOption(mock(GatewayDiscordClient.class), ApplicationCommandInteractionOptionData.builder()
                .name("expression")
                .value("1d6")
                .type(ApplicationCommandOption.Type.STRING.getValue())
                .build(), null);

        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(option));
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
        verify(slashEventAdaptor,times(2)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any(), any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong());
        verify(slashEventAdaptor, never()).replyEphemeral(any());
        verify(slashEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3", "[3]", ImmutableList.of())));

        verify(slashEventAdaptor,never()).getChannelId();
    }

}