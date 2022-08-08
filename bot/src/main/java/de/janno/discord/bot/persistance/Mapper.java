package de.janno.discord.bot.persistance;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import de.janno.discord.bot.command.Config;
import de.janno.discord.bot.command.LabelAndDiceExpression;
import de.janno.discord.bot.command.StateData;
import de.janno.discord.bot.command.countSuccesses.CountSuccessesConfig;
import de.janno.discord.bot.command.customDice.CustomDiceConfig;
import de.janno.discord.bot.command.customParameter.CustomIdIndex;
import de.janno.discord.bot.command.customParameter.CustomIdIndexWithValue;
import de.janno.discord.bot.command.customParameter.CustomParameterConfig;
import de.janno.discord.bot.command.customParameter.CustomParameterStateData;
import de.janno.discord.bot.command.fate.FateConfig;
import de.janno.discord.bot.command.holdReroll.HoldRerollConfig;
import de.janno.discord.bot.command.holdReroll.HoldRerollStateData;
import de.janno.discord.bot.command.poolTarget.PoolTargetConfig;
import de.janno.discord.bot.command.poolTarget.PoolTargetStateData;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetConfig;
import de.janno.discord.bot.command.sumCustomSet.SumCustomSetStateData;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public class Mapper {


    public static final String NO_PERSISTED_STATE = "None";
    private static final ImmutableBiMap<String, Class<? extends Config>> CONFIGS = ImmutableBiMap.<String, Class<? extends Config>>builder()
            .put("Config", Config.class)
            .put("CountSuccessesConfig", CountSuccessesConfig.class)
            .put("CustomDiceConfig", CustomDiceConfig.class)
            .put("CustomParameterConfig", CustomParameterConfig.class)
            .put("FateConfig", FateConfig.class)
            .put("HoldRerollConfig", HoldRerollConfig.class)
            .put("PoolTargetConfig", PoolTargetConfig.class)
            .put("SumCustomSetConfig", SumCustomSetConfig.class)
            .build();
    private static final ImmutableBiMap<String, Class<? extends StateData>> STATES = ImmutableBiMap.<String, Class<? extends StateData>>builder()
            .put("CustomParameterStateData", CustomParameterStateData.class)
            .put("HoldRerollStateData", HoldRerollStateData.class)
            .put("PoolTargetStateData", PoolTargetStateData.class)
            .put("SumCustomSetStateData", SumCustomSetStateData.class)
            .build();
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
            .addMixIn(Config.class, Mapper.ConfigMixin.class)
            .addMixIn(CountSuccessesConfig.class, Mapper.CountSuccessesConfigMixin.class)
            .addMixIn(CustomDiceConfig.class, Mapper.CustomDiceConfigMixin.class)
            .addMixIn(CustomParameterConfig.class, Mapper.CustomParameterConfigMixin.class)
            .addMixIn(FateConfig.class, Mapper.FateConfigMixin.class)
            .addMixIn(HoldRerollConfig.class, Mapper.HoldRerollConfigMixin.class)
            .addMixIn(PoolTargetConfig.class, Mapper.PoolTargetConfigMixin.class)
            .addMixIn(SumCustomSetConfig.class, Mapper.SumCustomSetConfigMixin.class)
            .addMixIn(CustomParameterStateData.class, Mapper.CustomParameterStateDataMixin.class)
            .addMixIn(HoldRerollStateData.class, Mapper.HoldRerollStateDataMixin.class)
            .addMixIn(PoolTargetStateData.class, Mapper.PoolTargetStateDataMixin.class)
            .addMixIn(SumCustomSetStateData.class, Mapper.SumCustomSetStateMixin.class)
            .addMixIn(LabelAndDiceExpression.class, Mapper.LabelAndDiceExpressionMixin.class)
            .addMixIn(CustomIdIndexWithValue.class, Mapper.CustomIdIndexWithValueMixin.class);


    public static String serializedConfig(@NonNull Config config) throws JsonProcessingException {
        return mapper.writeValueAsString(config);
    }

    public static String getConfigId(@NonNull Config config) {
        Preconditions.checkArgument(CONFIGS.inverse().containsKey(config.getClass()), "Missing config: %s", config);
        return CONFIGS.inverse().get(config.getClass());
    }

    public static String serializedStateData(StateData stateData) throws JsonProcessingException {
        if (stateData == null) {
            return null;
        }
        return mapper.writeValueAsString(stateData);
    }

    public static String getStateDataId(StateData stateData) {
        if (stateData == null) {
            return NO_PERSISTED_STATE;
        }
        Preconditions.checkArgument(STATES.inverse().containsKey(stateData.getClass()), "Missing stateData: %s", stateData);
        return STATES.inverse().get(stateData.getClass());
    }

    public static Config deserializeConfig(String configString, String configId) throws JsonProcessingException {
        Preconditions.checkArgument(CONFIGS.containsKey(configId), "Missing configId: %s", configId);
        return mapper.readValue(configString, CONFIGS.get(configId));
    }

    public static StateData deserializeState(String stateString, String stateId) throws JsonProcessingException {
        if (NO_PERSISTED_STATE.equals(stateId)) {
            return null;
        }
        Preconditions.checkArgument(STATES.containsKey(stateId), "Missing stateId: %s", stateId);
        return mapper.readValue(stateString, STATES.get(stateId));
    }

    private abstract static class ConfigMixin {
        @JsonCreator
        public ConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId) {
        }
    }

    private abstract static class CountSuccessesConfigMixin {
        @JsonCreator
        public CountSuccessesConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                         @JsonProperty("diceSides") int diceSides,
                                         @JsonProperty("target") int target,
                                         @JsonProperty("glitchOption") @NonNull String glitchOption,
                                         @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons) {
        }
    }

    private abstract static class CustomDiceConfigMixin {
        @JsonCreator
        public CustomDiceConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                     @JsonProperty("labelAndExpression") @NonNull List<LabelAndDiceExpression> labelAndExpression) {
        }
    }

    private abstract static class SumCustomSetConfigMixin {
        @JsonCreator
        public SumCustomSetConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                       @JsonProperty("labelAndExpression") List<LabelAndDiceExpression> labelAndExpression) {
        }
    }

    private abstract static class CustomParameterConfigMixin {
        @JsonCreator
        public CustomParameterConfigMixin(
                @JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                @JsonProperty("baseExpression") String baseExpression) {
        }

        @JsonIgnore
        public Collection<CustomIdIndexWithValue> getIdComponents() {
            return null;
        }

    }

    private abstract static class LabelAndDiceExpressionMixin {
        @JsonCreator
        public LabelAndDiceExpressionMixin(@JsonProperty("label") String label,
                                           @JsonProperty("diceExpression") String diceExpression) {
        }
    }


    private abstract static class FateConfigMixin {
        @JsonCreator
        public FateConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                               @JsonProperty("type") @NonNull String type) {

        }
    }

    private abstract static class HoldRerollConfigMixin {
        @JsonCreator
        public HoldRerollConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                     @JsonProperty("sidesOfDie") int sidesOfDie,
                                     @JsonProperty("rerollSet") @NonNull Set<Integer> rerollSet,
                                     @JsonProperty("successSet") @NonNull Set<Integer> successSet,
                                     @JsonProperty("failureSet") @NonNull Set<Integer> failureSet) {
        }
    }

    private abstract static class PoolTargetConfigMixin {
        @JsonCreator
        public PoolTargetConfigMixin(@JsonProperty("answerTargetChannelId") Long answerTargetChannelId,
                                     @JsonProperty("diceSides") int diceSides,
                                     @JsonProperty("maxNumberOfButtons") int maxNumberOfButtons,
                                     @JsonProperty("rerollSet") @NonNull Set<Integer> rerollSet,
                                     @JsonProperty("botchSet") @NonNull Set<Integer> botchSet,
                                     @JsonProperty("rerollVariant") String rerollVariant) {
        }
    }

    private abstract static class CustomParameterStateDataMixin {
        @JsonCreator
        public CustomParameterStateDataMixin(
                @JsonProperty("selectedParameterValues") @NonNull List<String> selectedParameterValues,
                @JsonProperty("lockedForUserName") @Nullable String lockedForUserName) {
        }

        @JsonIgnore
        public String getShortStringValues() {
            return "";
        }

        @JsonIgnore
        public Collection<CustomIdIndexWithValue> getIdComponents() {
            return null;
        }

    }

    private abstract static class HoldRerollStateDataMixin {
        int rerollCounter;

        @JsonCreator
        public HoldRerollStateDataMixin(
                @JsonProperty("currentResults") @NonNull List<Integer> currentResults,
                @JsonProperty("rerollCounter") int rerollCounter) {
        }

        @JsonIgnore
        public String getShortStringValues() {
            return "";
        }
    }

    private abstract static class PoolTargetStateDataMixin {

        @JsonCreator
        public PoolTargetStateDataMixin(
                @JsonProperty("dicePool") Integer dicePool,
                @JsonProperty("targetNumber") Integer targetNumber,
                @JsonProperty("doReroll") Boolean doReroll) {
        }

        @JsonIgnore
        public String getShortStringValues() {
            return "";
        }
    }

    private abstract static class SumCustomSetStateMixin {
        @JsonCreator
        public SumCustomSetStateMixin(@JsonProperty("diceExpression") String diceExpression,
                                      @JsonProperty("lockedForUserName") String lockedForUserName) {
        }

        @JsonIgnore
        public String getShortStringValues() {
            return "";
        }
    }

    private abstract static class CustomIdIndexWithValueMixin {
        @JsonCreator
        public CustomIdIndexWithValueMixin(@JsonProperty("customIdIndex") CustomIdIndex customIdIndex,
                                           @NonNull @JsonProperty("value") String value) {
        }
    }
}
