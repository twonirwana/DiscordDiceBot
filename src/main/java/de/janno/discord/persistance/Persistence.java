package de.janno.discord.persistance;

import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import reactor.core.publisher.Flux;

import java.io.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class Persistence {
    private final static int BUFFER_TIMEFRAME_IN_SEC = 10;
    private final File configSaveFile = new File("configSave.csv");
    private final List<IPersistable> persistables;
    private final FluxSinkConsumer<Trigger> saveFluxConsumer;

    public Persistence(List<IPersistable> persistables) {
        this.persistables = persistables;
        this.saveFluxConsumer = new FluxSinkConsumer<>();

        Flux.create(saveFluxConsumer::registerToSink)
                .filter(t -> t == Trigger.SAVE)
                .doOnEach(s -> log.info("Add save to buffer"))
                .buffer(Duration.ofSeconds(BUFFER_TIMEFRAME_IN_SEC))
                .doOnEach(s -> log.info("Collected {} trigger for save in {}s",
                        Optional.ofNullable(s.get()).map(List::size).orElse(0),
                        BUFFER_TIMEFRAME_IN_SEC))
                .subscribe(t -> saveAll());
    }

    public void acceptSaveTrigger(Trigger trigger) {
        saveFluxConsumer.accept(trigger);
    }


    private synchronized void saveAll() {
        log.info("Save configs");
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(configSaveFile, false), CSVFormat.DEFAULT)) {
            List<SerializedChannelConfig> currentConfig = persistables.stream()
                    .flatMap(p -> p.getChannelConfig().stream())
                    .collect(Collectors.toList());
            for (SerializedChannelConfig config : currentConfig) {
                printer.printRecord(config.getType(), config.getChannelId(), config.getConfig());
            }
        } catch (IOException e) {
            log.error("Saving error", e);
        }
    }

    public void loadAll() {
        if (configSaveFile.exists()) {
            log.info("load: " + configSaveFile);
            try {
                Reader in = new FileReader(configSaveFile);
                Map<String, List<SerializedChannelConfig>> typeConfigMap = CSVFormat.DEFAULT
                        .parse(in).stream()
                        .map(csv -> new SerializedChannelConfig(csv.get(0), csv.get(1), csv.get(2)))
                        .collect(Collectors.groupingBy(SerializedChannelConfig::getType));

                persistables.forEach(persistable -> {
                    log.info("restore: {}", persistable);
                    persistable.setChannelConfig(typeConfigMap.getOrDefault(persistable.getName(), ImmutableList.of()));
                });
            } catch (IOException e) {
                log.error("Loading error", e);
            }
        }
    }

}
