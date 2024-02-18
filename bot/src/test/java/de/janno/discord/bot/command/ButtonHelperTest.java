package de.janno.discord.bot.command;

import de.janno.discord.connector.api.message.ButtonDefinition;
import de.janno.discord.connector.api.message.ComponentRowDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static shadow.org.assertj.core.api.Assertions.assertThat;

class ButtonHelperTest {

    static Stream<Arguments> generateValidationData() {
        return Stream.of(
                Arguments.of("1d6", Optional.empty()),
                Arguments.of("1d6@roll", Optional.empty()),
                Arguments.of("1d6@roll@", Optional.of("The expression `1d6@roll@` should have the format diceExpression@Label")),
                Arguments.of("@@", Optional.of("The expression `@@` should have the format diceExpression@Label")),
                Arguments.of("@", Optional.of("The expression `@` should have the format diceExpression@Label")),
                Arguments.of(" ", Optional.of("Missing buttons expression like: `+1d6@D6;+5`")),
                Arguments.of("1d6;1d6", Optional.empty()),
                Arguments.of("1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23;24;25", Optional.empty()),
                Arguments.of("1;2;3;4;5;6;7;8;9;10;11;12;13;14;15;16;17;18;19;20;21;22;23;24;25;26", Optional.of("The maximum are 5 rows with each 5 buttons")),
                Arguments.of("1;;1;;1;;1;;1", Optional.empty()),
                Arguments.of("1;;1;;1;;1;;1;;1", Optional.of("The maximum are 5 rows with each 5 buttons")),
                Arguments.of("1;;1;;1;;1;;1;2;3;4;5;6", Optional.of("The maximum are 5 rows with each 5 buttons")),
                Arguments.of("1d6;1d6;", Optional.empty()),
                Arguments.of("1d6;;1d6;", Optional.empty()),
                Arguments.of("1d6;;1d6;;", Optional.empty()),
                Arguments.of("1d6;;1d6;;;;", Optional.empty()),
                Arguments.of("1d6;;;1d6;", Optional.of("Empty rows is not allowed")),
                Arguments.of("", Optional.of("Missing buttons expression like: `+1d6@D6;+5`"))
        );
    }

    @Test
    public void testParseString() {
        List<ButtonIdLabelAndDiceExpression> res = ButtonHelper.parseString("+1d4;+1d6;+1d7;+1d8;+1d10;+1d12;+1d14;+1d16;+1d20;+1d24;+1d16;+1d30;+1d100;+1;+2;+3;+4;-1;-2;-3;-4");

        assertThat(res.stream().map(ButtonIdLabelAndDiceExpression::getDiceExpression)).containsExactly("+1d4",
                "+1d6",
                "+1d7",
                "+1d8",
                "+1d10",
                "+1d12",
                "+1d14",
                "+1d16",
                "+1d20",
                "+1d24",
                "+1d16",
                "+1d30",
                "+1d100",
                "+1",
                "+2",
                "+3",
                "+4",
                "-1",
                "-2",
                "-3",
                "-4");
    }

    @Test
    public void testParseStringLineBreak() {
        List<ButtonIdLabelAndDiceExpression> res = ButtonHelper.parseString("+1d4;;1d6");

        assertThat(res.stream().map(ButtonIdLabelAndDiceExpression::getDiceExpression)).containsExactly("+1d4", "1d6");
    }
    @Test
    public void testParseStringMultiLineLiteral() {
        List<ButtonIdLabelAndDiceExpression> res = ButtonHelper.parseString("d[a\\nb\\nc,\\nd,e\\n];1d20@\\nAttack\\nDown\\n;3d10,3d10,3d10");

        assertThat(res.stream().map(ButtonIdLabelAndDiceExpression::getDiceExpression)).containsExactly("d[a\nb\nc,\nd,e\n]", "1d20", "3d10,3d10,3d10");
        assertThat(res.stream().map(ButtonIdLabelAndDiceExpression::getLabel)).containsExactly("d[a b c, d,e ]", "Attack Down", "3d10,3d10,3d10");
    }

    @Test
    public void createButtonLayout() {
        List<ComponentRowDefinition> res = ButtonHelper.createButtonLayout("id", UUID.fromString("00000000-0000-0000-0000-000000000000"), List.of(new ButtonIdLabelAndDiceExpression("1", "1", "1", false),
                new ButtonIdLabelAndDiceExpression("2", "2", "2", false)));

        assertThat(res).hasSize(1);
        assertThat(res.getFirst().getButtonDefinitions()).containsExactly(ButtonDefinition.builder().id("id\u001E1\u001E00000000-0000-0000-0000-000000000000").label("1").disabled(false).style(ButtonDefinition.Style.PRIMARY).build(),
                ButtonDefinition.builder().id("id\u001E2\u001E00000000-0000-0000-0000-000000000000").label("2").disabled(false).style(ButtonDefinition.Style.PRIMARY).build()
        );
    }

    @Test
    public void createButtonLayoutLinebreak() {
        List<ComponentRowDefinition> res = ButtonHelper.createButtonLayout("id", UUID.fromString("00000000-0000-0000-0000-000000000000"), List.of(new ButtonIdLabelAndDiceExpression("1", "1", "1", false),
                new ButtonIdLabelAndDiceExpression("2", "2", "2", true)));

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getButtonDefinitions()).containsExactly(ButtonDefinition.builder().id("id\u001E1\u001E00000000-0000-0000-0000-000000000000").label("1").disabled(false).style(ButtonDefinition.Style.PRIMARY).build());
        assertThat(res.get(1).getButtonDefinitions()).containsExactly(ButtonDefinition.builder().id("id\u001E2\u001E00000000-0000-0000-0000-000000000000").label("2").disabled(false).style(ButtonDefinition.Style.PRIMARY).build());
    }

    @Test
    public void extendButtonLayout() {
        List<ComponentRowDefinition> res = ButtonHelper.extendButtonLayout(List.of(ComponentRowDefinition.builder()
                        .buttonDefinition(ButtonDefinition.builder().id("id\u001E1\u001E00000000-0000-0000-0000-000000000000").label("1").build())
                        .build()),
                List.of(ButtonDefinition.builder().id("id\u001E2\u001E00000000-0000-0000-0000-000000000000").label("2").build()), false);

        assertThat(res).hasSize(1);
        assertThat(res.getFirst().getButtonDefinitions()).containsExactly(
                ButtonDefinition.builder().id("id\u001E1\u001E00000000-0000-0000-0000-000000000000").label("1").disabled(false).style(ButtonDefinition.Style.PRIMARY).build(),
                ButtonDefinition.builder().id("id\u001E2\u001E00000000-0000-0000-0000-000000000000").label("2").disabled(false).style(ButtonDefinition.Style.PRIMARY).build());
    }

    @Test
    public void extendButtonLayoutFive() {
        List<ComponentRowDefinition> res = ButtonHelper.extendButtonLayout(List.of(
                        ComponentRowDefinition.builder()
                                .buttonDefinition(ButtonDefinition.builder().id("1").label("1").build())
                                .buttonDefinition(ButtonDefinition.builder().id("2").label("2").build())
                                .buttonDefinition(ButtonDefinition.builder().id("3").label("3").build())
                                .buttonDefinition(ButtonDefinition.builder().id("4").label("4").build())
                                .build()
                ),
                List.of(
                        ButtonDefinition.builder().id("5").label("5").build(),
                        ButtonDefinition.builder().id("6").label("6").build()
                ), false);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getButtonDefinitions().stream().map(ButtonDefinition::getId)).containsExactly("1", "2", "3", "4", "5");
        assertThat(res.get(1).getButtonDefinitions().stream().map(ButtonDefinition::getId)).containsExactly("6");
    }

    @Test
    public void extendButtonLayoutLineBreak() {
        List<ComponentRowDefinition> res = ButtonHelper.extendButtonLayout(List.of(
                        ComponentRowDefinition.builder()
                                .buttonDefinition(ButtonDefinition.builder().id("1").label("1").build())
                                .buttonDefinition(ButtonDefinition.builder().id("2").label("2").build())
                                .buttonDefinition(ButtonDefinition.builder().id("3").label("3").build())
                                .buttonDefinition(ButtonDefinition.builder().id("4").label("4").build())
                                .build()
                ),
                List.of(
                        ButtonDefinition.builder().id("5").label("5").build(),
                        ButtonDefinition.builder().id("6").label("6").build()
                ), true);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getButtonDefinitions().stream().map(ButtonDefinition::getId)).containsExactly("1", "2", "3", "4");
        assertThat(res.get(1).getButtonDefinitions().stream().map(ButtonDefinition::getId)).containsExactly("5", "6");
    }


    @ParameterizedTest(name = "{index} buttons={0} -> {1}")
    @MethodSource("generateValidationData")
    void testValidation(String buttons, Optional<String> expected) {
        Optional<String> res = ButtonHelper.valdiate(buttons, Locale.ENGLISH, List.of(), false);
        assertThat(res).isEqualTo(expected);
    }


}