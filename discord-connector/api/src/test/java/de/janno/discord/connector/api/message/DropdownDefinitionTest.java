package de.janno.discord.connector.api.message;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DropdownDefinitionTest {

    @Test
    void testIdLength_builder() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DropdownDefinition.builder()
                .id("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .options(List.of())
                .build());

        assertThat(exception.getMessage()).isEqualTo("ID 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' is to long");

    }

    @Test
    void testIdEmpty() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DropdownDefinition.builder()
                .id("")
                .options(List.of())
                .build());

        assertThat(exception.getMessage()).isEqualTo("id is empty");
    }

    @Test
    void testTooManyOptions() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DropdownDefinition.builder()
                .id("id")
                .options(IntStream.range(0, 27).mapToObj(i -> DropdownDefinition.DropdownOption.builder()
                                .value(i + "")
                                .label("label")
                                .build())
                        .toList())
                .build());

        assertThat(exception.getMessage()).isEqualTo("To many options, max are 25 options");
    }

    @Test
    void testLabelOrEmoji_fail() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> DropdownDefinition.builder()
                .options(List.of(DropdownDefinition.DropdownOption.builder()
                        .value("value")
                        .label("")
                        .emoji(null)
                        .build()))
                .id("a").build());

        assertThat(exception.getMessage()).isEqualTo("label and emoji are empty");
    }
}