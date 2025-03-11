package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import net.fellbaum.jemoji.EmojiManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Value
@Builder
public class ButtonDefinition implements ComponentDefinition {
    private static final Pattern IS_EMOJI_PATTERN = Pattern.compile("^<a?:([a-zA-Z0-9_]+):([0-9]+)>$");
    @NonNull
    String label;
    @NonNull
    String id;
    @NonNull
    ButtonDefinition.Style style;
    boolean disabled;
    @Nullable
    String emoji;

    ButtonDefinition(@NonNull String label, @NonNull String id, Style style, boolean disabled, @Nullable String emoji) {
        //https://discord.com/developers/docs/interactions/message-components#button-object
        Preconditions.checkArgument(id.length() <= 100, String.format("ID '%s' is to long", id));
        Preconditions.checkArgument(StringUtils.isNoneBlank(label) || emoji != null, "label and emoji are empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id is empty");
        Preconditions.checkArgument(emoji == null || isEmoji(emoji), "invalid emoji: " + emoji);
        this.label = Optional.of(label).map(s -> StringUtils.abbreviate(s.replace("\n", " "), 80)).orElse(null);
        this.id = id;
        this.style = Objects.requireNonNullElse(style, Style.PRIMARY);
        this.disabled = disabled;
        this.emoji = emoji;
    }

    private static boolean isEmoji(String in) {
        return EmojiManager.isEmoji(in) || IS_EMOJI_PATTERN.matcher(in).find();
    }

    @Override
    public String getLabelOrPlaceholder() {
        return label;
    }

    public enum Style {
        PRIMARY(1),
        SECONDARY(2),
        SUCCESS(3),
        DANGER(4),
        LINK(5);

        private final int data;

        Style(int i) {
            this.data = i;
        }

        public int getValue() {
            return this.data;
        }
    }
}
