package de.janno.discord.bot.command.directRoll;

import com.google.common.collect.ImmutableList;
import de.janno.discord.bot.ResultImage;
import de.janno.discord.bot.command.AnswerFormatType;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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

    static Stream<Arguments> generateAliasData() {
        return Stream.of(
                Arguments.of(List.of(new Alias("base", "1d20")), List.of(new Alias("bonus", "20")), "base+bonus", "1d20+20"),
                Arguments.of(List.of(new Alias("Attack", "1d20")), List.of(new Alias("att", "Attack")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "Attack"), new Alias("Attack", "2d20")), List.of(), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("1d20", "1d20+1d20")), List.of(), "1d20@roll", "1d20+1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20"), new Alias("att", "2d20")), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20"), new Alias("att", "2d20")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(new Alias("att", "2d20")), "att@roll", "2d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(), "att@roll", "1d20@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20")), "att@roll", "1d20@roll"),
                Arguments.of(List.of(new Alias("att", "1d20")), List.of(), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(new Alias("att", "1d20")), "1d6@roll", "1d6@roll"),
                Arguments.of(List.of(), List.of(), "1d6@roll", "1d6@roll")
        );
    }

    @ParameterizedTest(name = "{index} channelAlias={0}, userChannelAlias={1}, expression={3} -> {4}")
    @MethodSource("generateAliasData")
    void applyAlias(List<Alias> channelAlias, List<Alias> userChannelAlias, String expression, String expected) {
        String res = underTest.applyAliaseToExpression(channelAlias, userChannelAlias, expression);
        assertThat(res).contains(expected);
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
        verify(slashEventAdaptor).createResultMessageWithEventReference(ArgumentMatchers.eq(new EmbedOrMessageDefinition("Test Label ⇒ 1", "1d6", ImmutableList.of(), new File("imageCache//cea2a67e61a8b605c6702aac213960f86922331b5cac795649502b363dde97aa.png"), EmbedOrMessageDefinition.Type.EMBED)));

        verify(slashEventAdaptor, times(3)).getChannelId();
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

        verify(slashEventAdaptor, times(2)).getChannelId();
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
                .descriptionOrContent("Type /r and a dice expression, configuration with /direct_roll_config\n" + DiceEvaluatorAdapter.getHelp())
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
                .description("direct roll of dice expression, configuration with /direct_roll_config")
                .option(CommandDefinitionOption.builder()
                        .name("expression")
                        .required(true)
                        .description("dice expression, e.g. '2d6'")
                        .type(CommandDefinitionOption.Type.STRING)
                        .build())
                .build());
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


    @Test
    void deserialization_alias() {

        String aliasString = """
                ---
                aliasList:
                - name: "att"
                  value: "d20+5"
                - name: "par"
                  value: "d20+3"
                - name: "dmg"
                  value: "3d6+4"
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "r", "AliasConfig", aliasString);

        AliasConfig res = underTest.deserializeAliasConfig(savedData);
        assertThat(res).isEqualTo(new AliasConfig(List.of(new Alias("att", "d20+5"), new Alias("par", "d20+3"), new Alias("dmg", "3d6+4"))));

    }
}