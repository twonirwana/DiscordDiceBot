package de.janno.discord.connector.api;

public class MissingPermissionException extends Exception{
    public MissingPermissionException(String message){
        super(message);
    }
}
