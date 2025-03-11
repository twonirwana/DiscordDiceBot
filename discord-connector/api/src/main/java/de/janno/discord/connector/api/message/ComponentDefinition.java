package de.janno.discord.connector.api.message;

public interface ComponentDefinition {

    String getId();

    String getLabelOrPlaceholder();

    boolean isDisabled();
}
