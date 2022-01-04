package de.janno.discord.command;

public interface IConfig {

    default int getHashForCache(){
        return this.hashCode();
    }

    String toMetricString();

}
