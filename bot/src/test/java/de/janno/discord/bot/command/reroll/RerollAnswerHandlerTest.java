package de.janno.discord.bot.command.reroll;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.AnswerInteractionType;
import de.janno.discord.bot.command.AnswerFormatType;
import de.janno.discord.bot.command.ConfigAndState;
import de.janno.discord.bot.command.DieIdDb;
import de.janno.discord.bot.dice.CachingDiceEvaluator;
import de.janno.discord.bot.dice.image.DiceImageStyle;
import de.janno.discord.bot.dice.image.DiceStyleAndColor;
import de.janno.discord.bot.persistance.*;
import de.janno.evaluator.dice.DieId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
class RerollAnswerHandlerTest {

    RerollAnswerHandler underTest;
    private Expect expect;

    @Test
    void checkPersistence() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new RerollAnswerHandler(persistenceManager, new CachingDiceEvaluator((minExcl, maxIncl) -> 0));

        UUID configUUID = UUID.randomUUID();
        long channelId = System.currentTimeMillis();
        long messageId = System.currentTimeMillis();
        DieId dieId1 = DieId.of(1, "d", 0, 0, 0);
        DieId dieId2 = DieId.of(1, "d", 0, 1, 0);
        RerollAnswerConfig config = new RerollAnswerConfig(123L, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, AnswerInteractionType.reroll,
                "2d6", List.of(
                new DieIdTypeAndValue(DieIdDb.fromDieId(dieId1), "2", 2, 6, null),
                new DieIdTypeAndValue(DieIdDb.fromDieId(dieId2), "5", 5, 6, null)
        ), 0, "userName", true, "roll");
        Optional<MessageConfigDTO> toSave = RerollAnswerHandler.createMessageConfig(configUUID, 1L, channelId, config);
        assertThat(toSave).isPresent();

        persistenceManager.saveMessageConfig(toSave.get());
        MessageConfigDTO loaded = persistenceManager.getMessageConfig(configUUID).orElseThrow();

        assertThat(toSave.get()).isEqualTo(loaded);
        RerollAnswerStateData stateData = new RerollAnswerStateData(List.of());
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, channelId, messageId, "reroll_answer", "RerollAnswerStateData", Mapper.serializedObject(stateData));

        ConfigAndState<RerollAnswerConfig, RerollAnswerStateData> configAndState = underTest.getMessageDataAndUpdateWithButtonValue(loaded, messageDataDTO, dieId1.toString(), "userName");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new RerollAnswerStateData(List.of(DieIdDb.fromDieId(dieId1))));
    }

    @Test
    void deserialization() {
        PersistenceManager persistenceManager = new PersistenceManagerImpl("jdbc:h2:mem:" + UUID.randomUUID(), null, null);
        underTest = new RerollAnswerHandler(persistenceManager, new CachingDiceEvaluator((minExcl, maxIncl) -> 0));

        UUID configUUID = UUID.randomUUID();
        MessageConfigDTO messageConfigDTO = new MessageConfigDTO(configUUID, 1L, 1660644934298L, "reroll_answer", "RerollAnswerConfig", """
                ---
                answerTargetChannelId: 123
                answerFormatType: "full"
                configLocale: "en"
                answerInteractionType: "reroll"
                expression: "2d6"
                dieIdTypeAndValues:
                - dieIdDb:
                    expressionPositionStartInc: 1
                    value: "d"
                    reEvaluateCounter: 0
                    dieIndex: 0
                    reroll: 0
                  value: "2"
                  numberSupplierValue: 2
                  diceSides: 6
                  selectedFrom: null
                - dieIdDb:
                    expressionPositionStartInc: 1
                    value: "d"
                    reEvaluateCounter: 0
                    dieIndex: 1
                    reroll: 0
                  value: "5"
                  numberSupplierValue: 5
                  diceSides: 6
                  selectedFrom: null
                rerollCount: 0
                owner: "owner"
                alwaysSumUp: true
                label: "roll"
                diceStyleAndColor:
                  diceImageStyle: "none"
                  configuredDefaultColor: "none"
                """);
        MessageDataDTO messageDataDTO = new MessageDataDTO(configUUID, 1L, 1660644934298L, 1660644934298L, "reroll_answer",
                "RerollAnswerStateData", """
                ---
                rerollDice:
                - expressionPositionStartInc: 1
                  value: "d"
                  reEvaluateCounter: 0
                  dieIndex: 0
                  reroll: 0
                """);

        ConfigAndState<RerollAnswerConfig, RerollAnswerStateData> configAndState = underTest.getMessageDataAndUpdateWithButtonValue(messageConfigDTO, messageDataDTO, "dadf", "userName");
        RerollAnswerConfig config = new RerollAnswerConfig(123L, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, AnswerInteractionType.reroll,
                "2d6", List.of(
                new DieIdTypeAndValue(DieIdDb.fromDieId(DieId.of(1, "d", 0, 0, 0)), "2", 2, 6, null),
                new DieIdTypeAndValue(DieIdDb.fromDieId(DieId.of(1, "d", 0, 1, 0)), "5", 5, 6, null)
        ), 0, "owner", true, "roll");
        assertThat(configAndState.getConfig()).isEqualTo(config);
        assertThat(configAndState.getConfigUUID()).isEqualTo(configUUID);
        assertThat(configAndState.getState().getData()).isEqualTo(new RerollAnswerStateData(List.of(DieIdDb.fromDieId(DieId.of(1, "d", 0, 0, 0)))));
    }

    @Test
    void configSerialization() {
        UUID configUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        RerollAnswerConfig config = new RerollAnswerConfig(123L, AnswerFormatType.full, null, new DiceStyleAndColor(DiceImageStyle.none, "none"), Locale.ENGLISH, AnswerInteractionType.reroll,
                "2d6", List.of(
                new DieIdTypeAndValue(DieIdDb.fromDieId(DieId.of(1, "d", 0, 0, 0)), "2", 2, 6, null),
                new DieIdTypeAndValue(DieIdDb.fromDieId(DieId.of(1, "d", 0, 1, 0)), "5", 5, 6, null)
        ), 0, "owner", true, "roll");
        Optional<MessageConfigDTO> toSave = RerollAnswerHandler.createMessageConfig(configUUID, 1L, 2L, config);
        assertThat(toSave).isPresent();

        expect.toMatchSnapshot(toSave.get());
    }

}