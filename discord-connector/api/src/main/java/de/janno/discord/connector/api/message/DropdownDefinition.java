package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Value
@Builder
public class DropdownDefinition implements ComponentDefinition {
    String id;
    String placeholder;
    int minValues;
    int maxValues;
    boolean disabled;
    @NonNull
    List<DropdownOption> options;

    DropdownDefinition(String id, String placeholder, int minValues, int maxValues, boolean disabled, @NonNull List<DropdownOption> options) {
        //https://discord.com/developers/docs/interactions/message-components#select-menu-object
        Preconditions.checkArgument(id.length() <= 100, String.format("ID '%s' is to long", id));
        Preconditions.checkArgument(options.size() <= 25, "To many options, max are 25 options");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(id), "id is empty");
        Preconditions.checkArgument(placeholder.length() <= 150, String.format("Placeholder '%s' is to long", placeholder));
        Preconditions.checkArgument(maxValues >= 0 && maxValues <= 25, String.format("maxValue must be between 0 and 25 but was: '%s'", maxValues));
        Preconditions.checkArgument(minValues >= 0 && minValues <= 25, String.format("minValues must be between 0 and 25 but was: '%s'", minValues));
        this.id = id;
        this.placeholder = placeholder;
        this.minValues = minValues;
        this.maxValues = maxValues;
        this.disabled = disabled;
        this.options = options;
    }

    @Override
    public String getLabelOrPlaceholder() {
        return placeholder;
    }

    @Data
    @Builder
    public static class DropdownOption {

        String label;
        @NonNull
        String value;
        String description;
        boolean isDefault;
        String emoji;

        DropdownOption(String label, @NonNull String value, String description, boolean isDefault, String emoji) {
            Preconditions.checkArgument(StringUtils.isNoneBlank(label) || emoji != null, "label and emoji are empty");
            Preconditions.checkArgument(emoji == null || EmojiHelper.isEmoji(emoji), "invalid emoji: " + emoji);
            Preconditions.checkArgument(value.length() <= 100, String.format("value '%s' is to long", value));
            Preconditions.checkArgument(!Strings.isNullOrEmpty(value), "value is empty");
            Preconditions.checkArgument(label == null ||label.length() <= 100, String.format("label '%s' is to long", label));
            Preconditions.checkArgument(description == null || description.length() <= 100, String.format("description '%s' is to long", description));

            this.label = label;
            this.value = value;
            this.description = description;
            this.isDefault = isDefault;
            this.emoji = emoji;
        }
    }
}
