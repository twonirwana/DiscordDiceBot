package de.janno.discord.bot.command;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import de.janno.discord.bot.ButtonEventAdaptorMock;
import io.avaje.config.Config;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SnapshotExtension.class)
public class LegacyIdHandlerMockTest {
    private Expect expect;

    @BeforeEach
    void setup() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
    }

    @AfterEach
    void cleanUp() throws IOException {
        File cacheDirectory = new File("imageCache/");
        if (cacheDirectory.exists()) {
            FileUtils.cleanDirectory(cacheDirectory);
        }
        Config.setProperty("diceEvaluator.cacheSize", "0");
    }

    @ParameterizedTest
    @CsvSource({
            "count_successes, true",
            "fate, true",
            "hold_reroll, true",
            "pool_target, true",
            "sum_dice_set, true",
            "custom_dice, false",
    })
    void handleId(String commandId, boolean expectMatchingId) {
        LegacyIdHandler underTest = new LegacyIdHandler();
        ButtonEventAdaptorMock buttonEvent = new ButtonEventAdaptorMock(commandId, "button_1", new AtomicLong(0));

        boolean matchingId = underTest.matchingComponentCustomId(buttonEvent.getCustomId());
        assertThat(matchingId).isEqualTo(expectMatchingId);

        if (matchingId) {
            underTest.handleComponentInteractEvent(buttonEvent).block();
            expect.scenario(commandId).toMatchSnapshot(buttonEvent.getSortedActions());
        }
    }
}
