package de.janno.discord.connector.api.message;

import net.fellbaum.jemoji.EmojiManager;

import java.util.regex.Pattern;

public class EmojiHelper {
    private static final Pattern IS_EMOJI_PATTERN = Pattern.compile("^<a?:([a-zA-Z0-9_]+):([0-9]+)>$");

    public static boolean isEmoji(String in) {
        return EmojiManager.isEmoji(in) || IS_EMOJI_PATTERN.matcher(in).find();
    }
}
