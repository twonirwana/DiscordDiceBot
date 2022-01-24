package de.janno.discord.api;

public class MissingPermissionException extends Exception{
    public MissingPermissionException(String message){
        super(message);
    }
}
