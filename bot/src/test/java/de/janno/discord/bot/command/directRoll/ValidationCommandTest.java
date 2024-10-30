package de.janno.discord.bot.command.directRoll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.channelConfig.DirectRollConfig;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.DiceEvaluatorAdapter;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.ChannelConfigDTO;
import de.janno.discord.bot.persistance.PersistenceManager;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.Requester;
import de.janno.discord.connector.api.SlashEventAdaptor;
import de.janno.discord.connector.api.message.EmbedOrMessageDefinition;
import de.janno.discord.connector.api.slash.CommandInteractionOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SnapshotExtension.class)
class ValidationCommandTest {
    ValidationCommand underTest;
    Expect expect;

    static Stream<Arguments> generateAllLocaleData() {
        return I18n.allSupportedLanguage().stream()
                .map(Arguments::of);
    }

    @BeforeEach
    void setup() {
        underTest = new ValidationCommand(mock(PersistenceManager.class), new CachingDiceEvaluator((minExcl, maxIncl) -> 1));
    }

    @ParameterizedTest(name = "{index} locale={0}")
    @MethodSource("generateAllLocaleData")
    void testHelp(Locale userLocale) {
        expect.scenario(userLocale.toString()).toMatchSnapshot(underTest.getHelpMessage(userLocale));
    }

    @Test
    void autoCompleteValid() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("expression", "1d6", List.of()), Locale.ENGLISH, 1L, 1L, 0L);

        assertThat(res).containsExactly(new AutoCompleteAnswer("1d6", "1d6"));
    }

    @Test
    void autoComplete_wrongAction() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("help", "1d6", List.of()), Locale.ENGLISH, 1L, 1L, 0L);

        assertThat(res).isEmpty();
    }

    @Test
    void autoComplete_Invalid() {
        List<AutoCompleteAnswer> res = underTest.getAutoCompleteAnswer(new AutoCompleteRequest("expression", "1d", List.of()), Locale.ENGLISH, 1L, 1L, 0L);

        assertThat(res).containsExactly(new AutoCompleteAnswer("Operator d has right associativity but the right value was: empty", "1d"));
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
        when(slashEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]", Locale.ENGLISH, null));

        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);

        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions(Locale.ENGLISH);
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).sendMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor, never()).replyWithEmbedOrMessageDefinition(any(), anyBoolean());
        verify(slashEventAdaptor, never()).sendMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor).reply("/r expression:asdfasdf\n" +
                "The following expression is invalid: __as__`dfasdf`. The error is: No matching operator for 'as', non-functional text and value names must to be surrounded by '' or []. Use `/r expression:help` to get more information on how to use the command.", true);

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
        when(slashEventAdaptor.replyWithEmbedOrMessageDefinition(any(), anyBoolean())).thenReturn(Mono.just(mock(Void.class)));
        when(slashEventAdaptor.getCommandString()).thenReturn("/r expression:help");
        when(slashEventAdaptor.getRequester()).thenReturn(new Requester("user", "channel", "guild", "[0 / 1]", Locale.ENGLISH, null));


        Mono<Void> res = underTest.handleSlashCommandEvent(slashEventAdaptor, () -> UUID.fromString("00000000-0000-0000-0000-000000000000"), Locale.ENGLISH);


        assertThat(res).isNotNull();

        verify(slashEventAdaptor).checkPermissions(Locale.ENGLISH);
        verify(slashEventAdaptor).getOption("expression");
        verify(slashEventAdaptor, times(1)).getCommandString();
        verify(slashEventAdaptor, never()).sendMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor, never()).sendMessage(any());
        verify(slashEventAdaptor, never()).deleteMessageById(anyLong());
        verify(slashEventAdaptor).replyWithEmbedOrMessageDefinition(EmbedOrMessageDefinition.builder()
                .descriptionOrContent("Type `/validation` and a dice expression, the autocomplete will show if there are errors in the expression.\n" + DiceEvaluatorAdapter.getHelp())
                .field(new EmbedOrMessageDefinition.Field("Example", "`/validation expression: 1d6`", false))
                .field(new EmbedOrMessageDefinition.Field("Full documentation", "https://github.com/twonirwana/DiscordDiceBot", false))
                .field(new EmbedOrMessageDefinition.Field("Discord Server for News, Help and Feature Requests", "https://discord.gg/e43BsqKpFr", false))
                .build(), true);

        verify(slashEventAdaptor, never()).getChannelId();
    }

    @Test
    public void getCommandDefinition() {
        expect.toMatchSnapshot(underTest.getCommandDefinition());
    }

    @Test
    public void getId() {
        expect.toMatchSnapshot(underTest.getCommandId());
    }


    @Test
    void deserialization_config_legacy() {
        String configString = """
                ---
                answerTargetChannelId: null
                alwaysSumResult: false
                answerFormatType: "without_expression"
                resultImage: "polyhedral_3d_red_and_white"
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "r", "DirectRollConfig", configString, null);


        DirectRollConfig res = underTest.deserializeConfig(savedData);
        assertThat(res).isEqualTo(new DirectRollConfig(null, false, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_3d, "red_and_white"), Locale.ENGLISH, null));

    }

    @Test
    void deserialization_config() {
        String configString = """
                ---
                answerTargetChannelId: null
                alwaysSumResult: false
                answerFormatType: "without_expression"
                configLocale: "de"
                diceStyleAndColor:
                  diceImageStyle: "polyhedral_alies_v2"
                  configuredDefaultColor: "blue_and_silver"
                """;

        ChannelConfigDTO savedData = new ChannelConfigDTO(UUID.randomUUID(), 1L, 2L, null, "r", "DirectRollConfig", configString, null);


        DirectRollConfig res = underTest.deserializeConfig(savedData);
        assertThat(res).isEqualTo(new DirectRollConfig(null, false, AnswerFormatType.without_expression, AnswerInteractionType.none, null, new DiceStyleAndColor(DiceImageStyle.polyhedral_alies_v2, "blue_and_silver"), Locale.GERMAN, null));
    }

}