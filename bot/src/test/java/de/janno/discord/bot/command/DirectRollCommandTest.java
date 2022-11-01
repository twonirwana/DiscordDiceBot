package de.janno.discord.bot.command;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.dice.Dice;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedDefinition;
import de.janno.discord.connector.api.slash.CommandDefinition;
import de.janno.discord.connector.api.slash.CommandDefinitionOption;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import de.janno.evaluator.dice.DiceEvaluator;
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
    Dice diceMock;

    @BeforeEach
    void setup() {
        diceMock = mock(Dice.class);
        underTest = new DirectRollCommand((minExcl, maxIncl) -> 0, diceMock);
    }


    @Test
    void handleComponentInteractEvent() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("1d6@Test Label")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(slashEventAdaptor.getChannelId()).thenReturn(1L);
        when(slashEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.deleteMessage(anyLong(), anyBoolean())).thenReturn(Mono.just(2L));
        when(slashEventAdaptor.acknowledgeAndRemoveSlash()).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:1d6");
        when(slashEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor, never()).reply(any(), anyBoolean());
        verify(slashEventAdaptor).acknowledgeAndRemoveSlash();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(2)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(slashEventAdaptor, never()).replyEmbed(any(), anyBoolean());
        verify(slashEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new EmbedDefinition("Test Label ⇒ 0", "1d6 throws 0", ImmutableList.of())));

        verify(slashEventAdaptor, never()).getChannelId();
    }

    @Test
    void handleComponentInteractEvent_validationFailed() {
        SlashEventAdaptor slashEventAdaptor = mock(SlashEventAdaptor.class);

        CommandInteractionOption interactionOption = CommandInteractionOption.builder()
                .name("expression")
                .stringValue("asdfasdf")
                .build();
        when(slashEventAdaptor.getOption(any())).thenReturn(Optional.of(interactionOption));
        when(diceMock.detailedRoll("asdfasdf")).thenThrow(new IllegalArgumentException("not a valid expression"));
        when(slashEventAdaptor.reply(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:asdfasdf");

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);

        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(slashEventAdaptor, never()).replyEmbed(any(), anyBoolean());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(slashEventAdaptor).reply("/r expression:asdfasdf\n" +
                "The following expression is invalid: 'asdfasdf'. The error is: '[d, D]' requires as left input a single integer but was '[as]'. Use `/r expression:help` to get more information on how to use the command.", true);

        verify(slashEventAdaptor, never()).getChannelId();
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
        when(slashEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor);


        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions();
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).createButtonMessage(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(slashEventAdaptor, never()).createResultMessageWithEventReference(any());
        verify(slashEventAdaptor, never()).deleteMessage(anyLong(), anyBoolean());
        verify(slashEventAdaptor).replyEmbed(EmbedDefinition.builder()
                .description("Type /r and a dice expression e.g. `/r 1d6` \n" + "```\n" + DiceEvaluator.getHelpText() + "\n```\nSee here: https://github.com/twonirwana/DiceEvaluator")
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
                .description("direct roll of dice expression")
                .option(CommandDefinitionOption.builder()
                        .name("expression")
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build());
    }
}