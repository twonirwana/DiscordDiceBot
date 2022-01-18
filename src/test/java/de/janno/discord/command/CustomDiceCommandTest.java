package de.janno.discord.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.api.Answer;
import de.janno.discord.api.IButtonEventAdaptor;
import de.janno.discord.api.Requester;
import de.janno.discord.cache.ButtonMessageCache;
import de.janno.discord.dice.DiceParserHelper;
import de.janno.discord.dice.IDice;
import dev.diceroll.parser.Dice;
import dev.diceroll.parser.NDice;
import dev.diceroll.parser.ResultTree;
import discord4j.core.object.component.LayoutComponent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomDiceCommandTest {

    CustomDiceCommand underTest;
    IDice diceMock;

    private static Stream<Arguments> generateValidateData() {
        return Stream.of(
                Arguments.of(ImmutableList.of(), "You must configure at least one button with a dice expression. Use '/custom_dice help' to get more information on how to use the command."),
                Arguments.of(ImmutableList.of("1d6"), null),
                Arguments.of(ImmutableList.of("1d6 "), null),
                Arguments.of(ImmutableList.of(" 1d6 "), null),
                Arguments.of(ImmutableList.of("2x[1d6]"), null),
                Arguments.of(ImmutableList.of("1d6@Attack"), null),
                Arguments.of(ImmutableList.of("1d6@Attack", "1d6@Parry"), "The dice expression '1d6' is not unique. Each dice expression must only once."),
                Arguments.of(ImmutableList.of("1d6@a,b"), "The button definition '1d6@a,b' is not allowed to contain ','"),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), null),
                Arguments.of(ImmutableList.of("a"), "The following dice expression are invalid: 'a'. Use custom_dice help to get more information on how to use the command."),
                Arguments.of(ImmutableList.of("@"), "The button definition '@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("a@Attack"), "The following dice expression are invalid: 'a'. Use custom_dice help to get more information on how to use the command."),
                Arguments.of(ImmutableList.of("a@"), "The button definition 'a@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("@Attack"), "Dice expression for '@Attack' is empty"),
                Arguments.of(ImmutableList.of("1d6@1d6"), null),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), "The button definition '1d6@1d6@1d6' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("1d6@@1d6"), "The button definition '1d6@@1d6' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("1d6@@"), "The button definition '1d6@@' should have the diceExpression@Label"),
                Arguments.of(ImmutableList.of("@1d6"), "Dice expression for '@1d6' is empty")

        );
    }

    private static Stream<Arguments> generateConfigOptionStringList() {
        return Stream.of(
                Arguments.of(ImmutableList.of(), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6"), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6", "1d6"), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6", "2d6"), new CustomDiceCommand.Config(ImmutableList.of(
                        new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6"),
                        new CustomDiceCommand.LabelAndDiceExpression("2d6", "2d6")
                ))),
                Arguments.of(ImmutableList.of("1d6 "), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of(" 1d6 "), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("2x[1d6]"), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("2x[1d6]", "2x[1d6]")))),
                Arguments.of(ImmutableList.of("1d6@Attack"), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("Attack", "1d6")))),
                Arguments.of(ImmutableList.of("1d6@a,b"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of(" 1d6 @ Attack "), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("Attack", "1d6")))),
                Arguments.of(ImmutableList.of("a"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("@"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("a@Attack"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("a@"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("@Attack"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@1d6"), new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6")))),
                Arguments.of(ImmutableList.of("1d6@1d6@1d6"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@@1d6"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("1d6@@"), new CustomDiceCommand.Config(ImmutableList.of())),
                Arguments.of(ImmutableList.of("@1d6"), new CustomDiceCommand.Config(ImmutableList.of()))
        );
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateConfigOptionStringList")
    void getConfigOptionStringList(List<String> optionValue, CustomDiceCommand.Config expected) {
        when(diceMock.roll(any())).thenAnswer(a -> {
            String expression = a.getArgument(0);
            return Dice.roll(expression);
        });
        assertThat(underTest.getConfigOptionStringList(optionValue)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index} config={0} -> {1}")
    @MethodSource("generateValidateData")
    void validate(List<String> optionValue, String expected) {
        when(diceMock.roll(any())).thenAnswer(a -> {
            String expression = a.getArgument(0);
            return Dice.roll(expression);
        });
        assertThat(underTest.validate(optionValue)).isEqualTo(expected);
    }


    @BeforeEach
    void setup() {
        diceMock = Mockito.mock(IDice.class);
        underTest = new CustomDiceCommand(new DiceParserHelper(diceMock));
    }

    @Test
    void getButtonMessageWithState() {
        String res = underTest.getButtonMessageWithState(new CustomDiceCommand.State("1d6"), new CustomDiceCommand.Config(ImmutableList.of()));

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getButtonMessage() {
        String res = underTest.getButtonMessage(new CustomDiceCommand.Config(ImmutableList.of()));

        assertThat(res).isEqualTo("Click on a button to roll the dice");
    }

    @Test
    void getConfigFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getAllButtonIds()).thenReturn(ImmutableList.of(new IButtonEventAdaptor.LabelAndCustomId("1d6", "custom_dice,1d6")));
        assertThat(underTest.getConfigFromEvent(event))
                .isEqualTo(new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6"))));
    }

    @Test
    void matchingComponentCustomId_match() {
        assertThat(underTest.matchingComponentCustomId("custom_dice,1;2")).isTrue();
    }

    @Test
    void matchingComponentCustomId_noMatch() {
        assertThat(underTest.matchingComponentCustomId("custom_dice")).isFalse();
    }

    @Test
    void getDiceResult_1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        Answer res = underTest.getAnswer(new CustomDiceCommand.State("1d6"),
                new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("1d6", "1d6"))));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("1d6 = 3");
        assertThat(res.getContent()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        Answer res = underTest.getAnswer(new CustomDiceCommand.State("3x[1d6]"),
                new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("3x[1d6]", "3x[1d6]"))));

        assertThat(res).isEqualTo(new Answer("Multiple Results", null, ImmutableList.of(
                new Answer.Field("1d6 = 6", "[6]", false),
                new Answer.Field("1d6 = 6", "[6]", false),
                new Answer.Field("1d6 = 6", "[6]", false)
        )));
    }


    @Test
    void getDiceResult_1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        Answer res = underTest.getAnswer(new CustomDiceCommand.State("1d6"),
                new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("Label", "1d6"))));

        assertThat(res.getFields()).hasSize(0);
        assertThat(res.getTitle()).isEqualTo("Label: 1d6 = 3");
        assertThat(res.getContent()).isEqualTo("[3]");
    }

    @Test
    void getDiceResult_3x1d6Label() {
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 6, ImmutableList.of()));
        Answer res = underTest.getAnswer(new CustomDiceCommand.State("3x[1d6]"),
                new CustomDiceCommand.Config(ImmutableList.of(new CustomDiceCommand.LabelAndDiceExpression("Label", "3x[1d6]"))));

        assertThat(res).isEqualTo(new Answer("Label", null, ImmutableList.of(
                new Answer.Field("1d6 = 6", "[6]", false),
                new Answer.Field("1d6 = 6", "[6]", false),
                new Answer.Field("1d6 = 6", "[6]", false)
        )));
    }

    @Test
    void getName() {
        String res = underTest.getName();

        assertThat(res).isEqualTo("custom_dice");
    }

    @Test
    void getStartOptions() {
        List<ApplicationCommandOptionData> res = underTest.getStartOptions();

        assertThat(res.stream().map(ApplicationCommandOptionData::name)).containsExactly("1_button",
                "2_button",
                "3_button",
                "4_button",
                "5_button",
                "6_button",
                "7_button",
                "8_button",
                "9_button",
                "10_button",
                "11_button",
                "12_button",
                "13_button",
                "14_button",
                "15_button",
                "16_button",
                "17_button",
                "18_button",
                "19_button",
                "20_button",
                "21_button",
                "22_button",
                "23_button",
                "24_button",
                "25_button");
    }

    @Test
    void getStateFromEvent() {
        IButtonEventAdaptor event = mock(IButtonEventAdaptor.class);
        when(event.getCustomId()).thenReturn("custom_dice,2d6");

        CustomDiceCommand.State res = underTest.getStateFromEvent(event);

        assertThat(res).isEqualTo(new CustomDiceCommand.State("2d6"));
    }

    @Test
    void createButtonCustomId() {
        String res = underTest.createButtonCustomId("2d6");

        assertThat(res).isEqualTo("custom_dice,2d6");
    }

    @Test
    void handleComponentInteractEvent() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice,1d6");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(false);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);


        StepVerifier.create(res)
                .verifyComplete();

        verify(buttonEventAdaptor).editMessage("processing ...");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on a button to roll the dice"),
                any()
        );
        verify(buttonEventAdaptor).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3",
                "[3]", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 60)));

        verify(buttonEventAdaptor,times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();
    }

    @Test
    void handleComponentInteractEvent_pinned() {
        IButtonEventAdaptor buttonEventAdaptor = mock(IButtonEventAdaptor.class);
        when(buttonEventAdaptor.getCustomId()).thenReturn("custom_dice,1d6");
        when(diceMock.detailedRoll("1d6")).thenReturn(new ResultTree(new NDice(6, 1), 3, ImmutableList.of()));
        when(buttonEventAdaptor.getChannelId()).thenReturn(1L);
        when(buttonEventAdaptor.getMessageId()).thenReturn(1L);
        when(buttonEventAdaptor.isPinned()).thenReturn(true);
        when(buttonEventAdaptor.editMessage(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.createButtonMessage(any(), any())).thenReturn(Mono.just(2L));
        when(buttonEventAdaptor.createResultMessageWithEventReference(any())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.deleteMessage(anyLong())).thenReturn(Mono.just(mock(Void.class)));
        when(buttonEventAdaptor.getRequester()).thenReturn(Mono.just(new Requester("user", "channel", "guild")));


        Mono<Void> res = underTest.handleComponentInteractEvent(buttonEventAdaptor);
        StepVerifier.create(res)
                .verifyComplete();


        verify(buttonEventAdaptor).editMessage("Click on a button to roll the dice");
        verify(buttonEventAdaptor).createButtonMessage(
                eq("Click on a button to roll the dice"),
                any()
        );
        verify(buttonEventAdaptor, never()).deleteMessage(anyLong());
        verify(buttonEventAdaptor).createResultMessageWithEventReference(eq(new Answer("1d6 = 3",
                "[3]", ImmutableList.of())));
        assertThat(underTest.getButtonMessageCache())
                .hasSize(1)
                .containsEntry(1L, ImmutableSet.of(new ButtonMessageCache.ButtonWithConfigHash(2L, 60)));

        verify(buttonEventAdaptor, times(2)).getCustomId();
        verify(buttonEventAdaptor).getMessageId();
        verify(buttonEventAdaptor).getChannelId();
        verify(buttonEventAdaptor).isPinned();
        verify(buttonEventAdaptor).getAllButtonIds();
        verify(buttonEventAdaptor, never()).getMessageContent();

    }

    @Test
    void getButtonLayoutWithState() {
        List<LayoutComponent> res = underTest.getButtonLayoutWithState(
                new CustomDiceCommand.State("2d6"),
                new CustomDiceCommand.Config(ImmutableList.of(
                        new CustomDiceCommand.LabelAndDiceExpression("2d6", "2d6"),
                        new CustomDiceCommand.LabelAndDiceExpression("Attack", "1d20")
                ))
        );

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get())).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get())).containsExactly("custom_dice,2d6", "custom_dice,1d20");
    }

    @Test
    void getButtonLayout() {
        List<LayoutComponent> res = underTest.getButtonLayout(
                new CustomDiceCommand.Config(ImmutableList.of(
                        new CustomDiceCommand.LabelAndDiceExpression("2d6", "2d6"),
                        new CustomDiceCommand.LabelAndDiceExpression("Attack", "1d20")
                ))
        );

        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().label().get())).containsExactly("2d6", "Attack");
        assertThat(res.stream().flatMap(l -> l.getChildren().stream()).map(l -> l.getData().customId().get())).containsExactly("custom_dice,2d6", "custom_dice,1d20");
    }

    @Test
    void editButtonMessage() {
        assertThat(underTest.getEditButtonMessage(new CustomDiceCommand.State("2d6"),
                new CustomDiceCommand.Config(ImmutableList.of(
                        new CustomDiceCommand.LabelAndDiceExpression("2d6", "2d6"),
                        new CustomDiceCommand.LabelAndDiceExpression("Attack", "1d20")
                )))).isNull();
    }
}