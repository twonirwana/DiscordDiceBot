package de.janno.discord.bot;

import lombok.NonNull;
import net.fellbaum.jemoji.EmojiManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotEmojiUtil {

    private static final Pattern START_WITH_EMOJI_PATTERN = Pattern.compile("^<a?:([a-zA-Z0-9_]+):([0-9]+)>");

    private static String getLeadingUnicodeEmoji(String in) {
        if (EmojiManager.containsEmoji(in)
                && in.startsWith(EmojiManager.extractEmojisInOrder(in).getFirst().getEmoji())) {
            return EmojiManager.extractEmojisInOrder(in).getFirst().getEmoji();
        }
        return null;
    }

    private static String getLeadingDiscordEmoji(String in) {
        Matcher emojiMatcher = START_WITH_EMOJI_PATTERN.matcher(in);
        boolean found = emojiMatcher.find();
        if (found) {
            return emojiMatcher.group();
        }
        return null;
    }

    public static LabelAndEmoji splitLabel(final @NonNull String label) {
        final String emoji;
        final String labelWithoutLeadingEmoji;
        final String cleanLabel = label.trim();
        final String unicodeEmoji = BotEmojiUtil.getLeadingUnicodeEmoji(cleanLabel);
        if (unicodeEmoji != null) {
            emoji = unicodeEmoji;
            labelWithoutLeadingEmoji = label.substring(unicodeEmoji.length());
            return new LabelAndEmoji(labelWithoutLeadingEmoji, emoji);
        }
        final String discordEmoji = BotEmojiUtil.getLeadingDiscordEmoji(cleanLabel);
        if (discordEmoji != null) {
            emoji = discordEmoji;
            //can produce an empty String
            labelWithoutLeadingEmoji = label.substring(discordEmoji.length());
            return new LabelAndEmoji(labelWithoutLeadingEmoji, emoji);

        }
        return new LabelAndEmoji(label, null);
    }

    public record LabelAndEmoji(String labelWithoutLeadingEmoji, String emoji) {
    }
}
