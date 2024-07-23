package de.janno.discord.bot;

import io.avaje.config.Config;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.fellbaum.jemoji.EmojiManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BotEmojiUtil {

    private static final Pattern START_WITH_EMOJI_PATTERN = Pattern.compile("^<a?:([a-zA-Z0-9_]+):([0-9]+)>");

    public static String replaceEmojiKeyWithEmoji(String input) {
        if (input == null) {
            return null;
        }
        for (EmojiKey emojiKey : EmojiKey.values()) {
            String discordEmoji = Config.getOptional(emojiKey.getKey()).orElse("");
            input = input.replace(emojiKey.getKey(), discordEmoji);
        }
        return input;
    }

    public static String toDiscordString(EmojiKey emojiKey) {
        if (emojiKey == null) {
            return null;
        }
        return Config.getOptional(emojiKey.getKey()).orElse(null);
    }

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

    @Getter
    @RequiredArgsConstructor
    public enum EmojiKey {
        BACK_EMOJI_KEY("emoji.back"),
        ROLL_EMOJI_KEY("emoji.roll"),
        CANCEL_EMOJI_KEY("emoji.cancel"),
        FATE_EMOJI_KEY("emoji.fate"),
        D20S20_RDD_EMOJI_KEY("emoji.d20s20_rdd"),
        D6S6_RDD_EMOJI_KEY("emoji.d6s6_rdd"),
        COIN_EMOJI_KEY("emoji.coinflip"),
        D6S6_BLACK_GOLD_EMOJI_KEY("emoji.d6s6_black_gold"),
        D10S10_RDD_EMOJI_KEY("emoji.d10s10_rdd"),
        D10S10_RED_EMOJI_KEY("emoji.d10s10_red"),
        D10S10_BLUE_EMOJI_KEY("emoji.d10s10_blue"),
        CALCULATOR_EMOJI_KEY("emoji.calculator");

        private final String key;

    }

    public record LabelAndEmoji(String labelWithoutLeadingEmoji, String emoji) {
    }
}
