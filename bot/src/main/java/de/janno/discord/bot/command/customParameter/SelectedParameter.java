package de.janno.discord.bot.command.customParameter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import lombok.Value;


import java.util.Optional;

@Value
//a parameter with and with its value
public class SelectedParameter {
    @NonNull
    //parameter with {} and range
    String parameterExpression;
    @NonNull
    String name;
    
    String selectedValue;
    
    String labelOfSelectedValue;
    boolean finished;
    @NonNull
    String pathId;
    /**
     * if the selection is finished then nextPathId contains the pathId of the selected parameterOption.
     * It is null if finished=false
     */
    
    String nextPathId;

    @JsonCreator
    public SelectedParameter(@JsonProperty("parameterExpression") @NonNull String parameterExpression,
                             @JsonProperty("name") @NonNull String name,
                             @JsonProperty("selectedValue") String selectedValue,
                             @JsonProperty("labelOfSelectedValue") String labelOfSelectedValue,
                             @JsonProperty("finished") Boolean finished,
                             @JsonProperty("pathId") String pathId,
                             @JsonProperty("nextPathId") String nextPathId) {
        this.parameterExpression = parameterExpression;
        this.name = name;
        this.selectedValue = selectedValue;
        this.labelOfSelectedValue = labelOfSelectedValue;
        this.finished = Optional.ofNullable(finished).orElse(selectedValue != null);
        this.pathId = Optional.ofNullable(pathId).orElse(Parameter.NO_PATH);
        //legacy handling, if a parameter is finished then the next path is always set
        if (this.finished && nextPathId == null) {
            this.nextPathId = Parameter.NO_PATH;
        } else {
            this.nextPathId = nextPathId;
        }
    }


    @JsonIgnore
    public String getShortString() {
        return "%s=%s".formatted(parameterExpression,
                (Optional.ofNullable(labelOfSelectedValue).orElse(""))).replace("\n", " ");
    }

    @JsonIgnore
    public SelectedParameter copy() {
        return new SelectedParameter(parameterExpression, name, selectedValue, labelOfSelectedValue, finished, pathId, nextPathId);
    }
}
