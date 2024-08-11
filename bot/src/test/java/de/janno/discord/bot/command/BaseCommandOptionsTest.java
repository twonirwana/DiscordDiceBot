package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.I18n;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.connector.api.AutoCompleteAnswer;
import de.janno.discord.connector.api.AutoCompleteRequest;
import de.janno.discord.connector.api.OptionValue;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@ExtendWith(SnapshotExtension.class)
class BaseCommandOptionsTest {

    Expect expect;

    static Stream<Arguments> generateColorLocaleData() {
        return Arrays.stream(DiceImageStyle.values())
                .flatMap(d -> I18n.allSupportedLanguage().stream()
                        .map(l -> Arguments.of(d, l)));
    }

    static Stream<Arguments> generateLocaleData() {
        return I18n.allSupportedLanguage().stream()
                .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("generateColorLocaleData")
    void autoCompleteColorOption(DiceImageStyle style, Locale userLocal) {
        List<AutoCompleteAnswer> res = BaseCommandOptions.autoCompleteColorOption(new AutoCompleteRequest("dice_image_color", "", List.of(new OptionValue("dice_image_style", style.name()))), userLocal);
        expect.scenario(style + "_" + userLocal).toMatchSnapshot(res);
    }

    @ParameterizedTest
    @MethodSource("generateLocaleData")
    void autoCompleteColorOption_invalidStyle(Locale userLocal) {
        List<AutoCompleteAnswer> res = BaseCommandOptions.autoCompleteColorOption(new AutoCompleteRequest("dice_image_color", "", List.of(new OptionValue("dice_image_style", "test"))), userLocal);
        expect.scenario(userLocal.toString()).toMatchSnapshot(res);
    }
}