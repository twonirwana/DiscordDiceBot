package de.janno.discord.bot.persistance;


import lombok.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.dataformat.yaml.YAMLMapper;


public class Mapper {

    public static final String NO_PERSISTED_STATE = "None";
    private static final YAMLMapper mapper = YAMLMapper.builder()
            .build();

    public static String serializedObject(@NonNull Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T deserializeObject(@NonNull String object, @NonNull Class<T> classOfObject) {
        try {
            return mapper.readValue(object, classOfObject);
        } catch (JacksonException e) {
            throw new IllegalStateException(e);
        }
    }

}
