package de.janno.discord.bot.command.sumDiceSet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.janno.discord.bot.command.EmptyData;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(using = SumDiceSetStateData.MapDeserializer.class)
public class SumDiceSetStateData extends EmptyData {

    @NonNull
    Map<String, Integer> diceSetMap;

    @JsonCreator
    public SumDiceSetStateData(@NonNull @JsonProperty("diceSetMap") Map<String, Integer> diceSetMap) {
        this.diceSetMap = diceSetMap;
    }

    @Override
    @JsonIgnore
    public String getShortStringValues() {
        return String.format("%s", diceSetMap);
    }

    //todo find better deserilizer
    public static class MapDeserializer extends StdDeserializer<SumDiceSetStateData> {
        public MapDeserializer() {
            this(null);
        }

        public MapDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public SumDiceSetStateData deserialize(JsonParser jp, DeserializationContext dc)
                throws IOException {
            Map<String, Integer> diceMap = new HashMap<>();
            JsonNode node = jp.getCodec().readTree(jp);
            JsonNode parms = node.get("diceSetMap");

            for (Iterator<Map.Entry<String, JsonNode>> it = parms.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                String key = entry.getKey();
                Integer value = entry.getValue().asInt();
                diceMap.put(key, value);
            }
            return new SumDiceSetStateData(diceMap);
        }
    }

}
