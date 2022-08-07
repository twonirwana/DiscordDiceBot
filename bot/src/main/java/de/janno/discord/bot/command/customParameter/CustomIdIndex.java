package de.janno.discord.bot.command.customParameter;

public enum CustomIdIndex {
    BASE_EXPRESSION(1),
    ANSWER_TARGET_CHANNEL(2),
    SELECTED_PARAMETER(3),
    BUTTON_VALUE(4);


    public final int index;

    CustomIdIndex(int index) {
        this.index = index;
    }
}
