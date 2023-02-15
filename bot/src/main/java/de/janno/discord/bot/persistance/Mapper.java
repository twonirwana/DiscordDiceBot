package de.janno.discord.bot.persistance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.NonNull;


public class Mapper {

    public static final String NO_PERSISTED_STATE = "None";
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    public static String serializedObject(@NonNull Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> T deserializeObject(@NonNull String object, @NonNull Class<T> classOfObject) {
        try {
            return mapper.readValue(object, classOfObject);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

}
