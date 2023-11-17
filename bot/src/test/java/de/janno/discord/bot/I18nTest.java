package de.janno.discord.bot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @Test
    void testFallbackToDefault_English() {
        String res = I18n.getMessage("command.help", Locale.ENGLISH);
        assertThat(res).isEqualTo("help");
    }

    @Test
    void testFallbackToDefault_Japan() {
        String res = I18n.getMessage("command.help", Locale.JAPAN);
        assertThat(res).isEqualTo("help");
    }

    @Test
    void testGermany() {
        String res = I18n.getMessage("command.help", Locale.GERMAN);
        assertThat(res).isEqualTo("hilfe");
    }


    @Test
    void debug() throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource("/images");
        Path dirPath = Paths.get(url.toURI());
        Stream<Path> paths = Files.list(dirPath);
        List<String> names  = paths.map(p -> p.getFileName().toString())
                        .toList();
        System.out.println(names);
    }
}