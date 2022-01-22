package de.janno.discord.api;

public class MissingPermissionException extends Exception{
    public MissingPermissionException(){
        super("Missing Permission");
    }
}
