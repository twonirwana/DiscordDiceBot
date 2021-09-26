package de.janno.discord.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DiceSideTargetNumberConfig implements Serializable {
    Integer diceSide;
    Integer targetNumber;

    @Override
    public String toString() {
        return String.format("Sides of the die: %d and target number: %d", diceSide, targetNumber);
    }
}
