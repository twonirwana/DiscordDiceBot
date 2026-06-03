package de.janno.discord.connector.jda;

import au.com.origin.snapshots.Snapshot;
import au.com.origin.snapshots.comparators.SnapshotComparator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


public class JsonObjectComparator implements SnapshotComparator {
    private static Object asObject(String snapshotName, String json) {
        try {
            return new ObjectMapper().readValue(json.replaceFirst(snapshotName + "=", ""), Object.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean matches(Snapshot previous, Snapshot current) {
        return asObject(previous.getName(), previous.getBody()).equals(asObject(current.getName(), current.getBody()));
    }
}
