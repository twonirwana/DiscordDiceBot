package de.janno.discord.bot.persistance;

import lombok.Value;

import java.time.LocalDateTime;

@Value
public class IdAndCreationDate {
        long id;
        LocalDateTime creationDate;
}
