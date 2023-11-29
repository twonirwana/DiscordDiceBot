package de.janno.discord.connector.api.message;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
public class EmbedOrMessageDefinition {

    //https://discord.com/developers/docs/resources/channel#create-message
    //https://discord.com/developers/docs/resources/channel#embed-limits
    String title;
    String descriptionOrContent;
    @Singular
    @NonNull
    List<Field> fields;
    Supplier<? extends InputStream> image;
    @Singular
    @NonNull
    List<ComponentRowDefinition> componentRowDefinitions;
    Type type;

    public EmbedOrMessageDefinition(String title, String descriptionOrContent, @NonNull List<Field> fields, Supplier<? extends InputStream> image, @NonNull List<ComponentRowDefinition> componentRowDefinitions, Type type) {
        this.title = title;
        this.descriptionOrContent = descriptionOrContent;
        this.fields = fields;
        this.image = image;
        this.componentRowDefinitions = componentRowDefinitions;
        this.type = Optional.ofNullable(type).orElse(Type.EMBED);
        if (this.type == Type.EMBED) {
            Preconditions.checkArgument(title == null || title.length() <= 256, "Title %s is to long", title);
            Preconditions.checkArgument(descriptionOrContent == null || descriptionOrContent.length() <= 4096, "Description %s is to long", title);
            Preconditions.checkArgument(fields.size() <= 25, "Too many fields in {}, max is 25", fields);
            Preconditions.checkArgument(componentRowDefinitions.size() <= 5, "Too many component rows in %s, max is 5", componentRowDefinitions);
            List<String> duplicatedComponentKeys = componentRowDefinitions.stream().flatMap(r -> r.getButtonDefinitions().stream())
                    .collect(Collectors.groupingBy(ButtonDefinition::getId, Collectors.counting()))
                    .entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .map(Map.Entry::getKey)
                    .toList();
            Preconditions.checkArgument(duplicatedComponentKeys.isEmpty(), "The following componentKeys are not unique: {}", duplicatedComponentKeys);
        } else {
            Preconditions.checkArgument(title == null, "Message have no title");
            Preconditions.checkArgument(descriptionOrContent == null || descriptionOrContent.length() <= 2000, "Content %s is to long", title);
            Preconditions.checkArgument(fields.isEmpty(), "Message have no Fields");
            Preconditions.checkArgument(image == null, "Message have no image");
            Preconditions.checkArgument(componentRowDefinitions.size() <= 5, "Too many component rows in %s, max is 5", componentRowDefinitions);
        }
    }

    @Override
    public String toString() {
        return "EmbedOrMessageDefinition(" +
                "title=" + title +
                ", descriptionOrContent=" + descriptionOrContent +
                ", fields=" + fields +
                ", componentRowDefinitions=" + componentRowDefinitions +
                ", hasImage=" + (image != null) +
                ", type=" + type +
                ')';
    }

    public enum Type {
        MESSAGE,
        EMBED
    }

    public static class EmbedOrMessageDefinitionBuilder {
        String title;
        String descriptionOrContent;

        public EmbedOrMessageDefinitionBuilder shortedTitle(String title) {
            this.title = StringUtils.abbreviate(title, 256);
            return this;
        }

        public EmbedOrMessageDefinitionBuilder shortedDescription(String description) {
            this.descriptionOrContent = StringUtils.abbreviate(description, 4096);
            return this;
        }

        public EmbedOrMessageDefinitionBuilder shortedContent(String content) {
            this.descriptionOrContent = StringUtils.abbreviate(content, 2000);
            return this;
        }

    }

    @Value
    public static class Field {
        @NonNull
        String name;
        @NonNull
        String value;
        boolean inline;

        public Field(@NonNull String name, @NonNull String value, boolean inline) {
            this.name = StringUtils.abbreviate(name, 256);
            this.value = StringUtils.abbreviate(value, 1024);
            this.inline = inline;
        }
    }
}
