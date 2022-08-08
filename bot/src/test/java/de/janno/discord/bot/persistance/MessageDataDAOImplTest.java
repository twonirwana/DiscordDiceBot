package de.janno.discord.bot.persistance;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterStateData;
import de.janno.discord.bot.command.fate.FateConfig;
import de.janno.discord.bot.command.holdReroll.HoldRerollConfig;
import de.janno.discord.bot.command.holdReroll.HoldRerollStateData;
import de.janno.discord.bot.command.poolTarget.PoolTargetConfig;
import de.janno.discord.bot.command.poolTarget.PoolTargetStateData;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetStateData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MessageDataDAOImplTest {

    MessageDataDAO underTest = new MessageDataDAOImpl("jdbc:h2:file:./persistence/dice_config", null, null);


    private static Stream<Arguments> generateData() {
        return Stream.of(
                Arguments.of(new CountSuccessesConfig(123L, 6, 5, "no_glitch", 12), null),
                Arguments.of(new CustomParameterConfig(123L, "{n}d{s}"),
                        new CustomParameterStateData(ImmutableList.of("5"), "userName")),
                Arguments.of(new FateConfig(123L, "with_modifier"), null),
                Arguments.of(new HoldRerollConfig(123L, 10, ImmutableSet.of(9, 10), ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1)),
                        new HoldRerollStateData(ImmutableList.of(1, 2, 3), 2)),
                Arguments.of(new PoolTargetConfig(123L, 10, 12, ImmutableSet.of(7, 8, 9, 10), ImmutableSet.of(1), "ask"),
                        new PoolTargetStateData(5, 7, true)),
                Arguments.of(new SumCustomSetConfig(123L, ImmutableList.of(
                        new LabelAndDiceExpression("Label", "+1d6"),
                        new LabelAndDiceExpression("+2d4", "+2d4")
                )), new SumCustomSetStateData("+1d6", "2d4")),
                Arguments.of(new Config(123L), new SumCustomSetStateData("1d6", "testName"))
        );
    }

    @ParameterizedTest(name = "{index} - {0},{1}")
    @MethodSource("generateData")
    void serializeDeserialize(Config config, StateData stateDate) {
        long messageId = System.currentTimeMillis();
        MessageData toSave = new MessageData(UUID.randomUUID(), 1L, messageId, "sum_custom_set", config, stateDate);

        underTest.saveMessageData(toSave);
        MessageData loaded = underTest.getDataForMessage(1L, messageId).orElseThrow();


        assertThat(toSave).isEqualTo(loaded);
    }


}