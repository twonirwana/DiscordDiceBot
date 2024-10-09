package de.janno.discord.bot.command;

import com.google.common.base.Stopwatch;
import de.janno.discord.bot.BotMetrics;
import lombok.Data;

import java.time.Duration;

@Data
public class Timer {
    private Stopwatch stopwatch = Stopwatch.createStarted();
    private Duration replyOrAcknowledge;
    private Duration answer;
    private Duration newButton;
    private Duration furtherAction;
    private final String commandId;

    public String toLog() {
        Duration total = stopwatch.elapsed();

        return ""; //todo
    }

    public void stopReplyOrAcknowledge() {
        //todo metric
        replyOrAcknowledge = stopwatch.elapsed();
    }

    public void stopAnswer() {
        answer = stopwatch.elapsed();
        BotMetrics.timerAnswerMetricCounter(commandId, answer);
    }

    public void stopFurtherAction() {
        //todo metric
        furtherAction = stopwatch.elapsed();
    }

    public void stopNewButton() {
        newButton = stopwatch.elapsed();
        BotMetrics.incrementButtonMetricCounter(commandId);
        BotMetrics.timerNewButtonMessageMetricCounter(commandId, newButton);
    }
}
