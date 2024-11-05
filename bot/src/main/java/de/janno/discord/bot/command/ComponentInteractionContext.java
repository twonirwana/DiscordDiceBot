package de.janno.discord.bot.command;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import lombok.Data;

import java.time.Duration;

@Data
public class ComponentInteractionContext {
    private final String commandId;
    private Stopwatch stopwatch = Stopwatch.createStarted();
    private Duration startAcknowledge;
    private Duration replyFinished;
    private Duration acknowledgeFinished;
    private Duration answerFinished;
    private Duration newButtonFinished;
    private String answerText;

    public String toLog() {
        String out = null;
        if(answerText  != null){
            out = answerText;
        }
        if(startAcknowledge != null) {
            out = Joiner.on(" ").skipNulls().join(out, "start=%dms".formatted(startAcknowledge.toMillis()));
        }
        if (replyFinished != null) {
            out = Joiner.on(" ").skipNulls().join(out, "reply=%dms".formatted(replyFinished.toMillis()));
        }
        if (acknowledgeFinished != null) {
            out = Joiner.on(" ").skipNulls().join(out, "ack=%dms".formatted(acknowledgeFinished.toMillis()));
        }
        if (answerFinished != null) {
            out = Joiner.on(" ").skipNulls().join(out, "answer=%dms".formatted(answerFinished.toMillis()));
        }
        if (newButtonFinished != null) {
            out = Joiner.on(" ").skipNulls().join(out, "newButton=%dms".formatted(newButtonFinished.toMillis()));
        }
        return Joiner.on(" ").skipNulls().join(out, "total=%dms".formatted(stopwatch.elapsed().toMillis()));
    }

    public void stopStartAcknowledge() {
        startAcknowledge = stopwatch.elapsed();
        BotMetrics.timerAcknowledgeStartMetricCounter(commandId, startAcknowledge);
    }

    public void stopReplyFinished() {
        replyFinished = stopwatch.elapsed();
        //reply also acknowledges
        BotMetrics.timerAcknowledgeFinishedMetricCounter(commandId, replyFinished);
    }

    public void stopAcknowledgeFinished() {
        acknowledgeFinished = stopwatch.elapsed();
        BotMetrics.timerAcknowledgeFinishedMetricCounter(commandId, acknowledgeFinished);
    }

    public void stopAnswer() {
        answerFinished = stopwatch.elapsed();
        BotMetrics.timerAnswerMetricCounter(commandId, answerFinished);
    }

    public void stopNewButton() {
        newButtonFinished = stopwatch.elapsed();
        BotMetrics.timerNewButtonMessageMetricCounter(commandId, newButtonFinished);
    }
}
