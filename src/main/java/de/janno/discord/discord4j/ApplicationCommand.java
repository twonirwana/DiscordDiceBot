package de.janno.discord.discord4j;

import com.google.common.collect.ImmutableList;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandOptionChoiceData;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.Objects;

@Builder
@Value
public class ApplicationCommand {

    String name;
    String description;
    @Singular
    List<ApplicationCommandOptionData> options;

    public boolean equalToApplicationCommandData(ApplicationCommandData applicationCommandData) {
        return Objects.equals(name, applicationCommandData.name()) &&
                Objects.equals(description, applicationCommandData.description()) &&
                equalApplicationCommandOptionDataList(options, applicationCommandData.options().toOptional().orElse(ImmutableList.of()));
    }

    private boolean equalApplicationCommandOptionDataList(List<ApplicationCommandOptionData> applicationCommandOptionDataList1,
                                                          List<ApplicationCommandOptionData> applicationCommandOptionDataList2) {
        if (applicationCommandOptionDataList1 == null && applicationCommandOptionDataList2 == null) {
            return true;
        } else if (applicationCommandOptionDataList1 != null && applicationCommandOptionDataList2 == null) {
            return false;
        } else if (applicationCommandOptionDataList1 == null) {
            return false;
        }
        if (applicationCommandOptionDataList1.size() != applicationCommandOptionDataList2.size()) {
            return false;
        }
        for (int i = 0; i < applicationCommandOptionDataList1.size(); i++) {
            if (!equalApplicationCommandOptionData(applicationCommandOptionDataList1.get(i), applicationCommandOptionDataList2.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean equalApplicationCommandOptionData(ApplicationCommandOptionData applicationCommandOptionData1,
                                                      ApplicationCommandOptionData applicationCommandOptionData2) {

        return Objects.equals(applicationCommandOptionData1.name(), applicationCommandOptionData2.name()) &&
                Objects.equals(applicationCommandOptionData1.description(), applicationCommandOptionData2.description()) &&
                Objects.equals(applicationCommandOptionData1.type(), applicationCommandOptionData2.type()) &&
                applicationCommandOptionData1.required().toOptional().orElse(false).equals(applicationCommandOptionData2.required().toOptional().orElse(false)) &&
                applicationCommandOptionData1.autocomplete().equals(applicationCommandOptionData2.autocomplete()) &&
                applicationCommandOptionData1.minValue().equals(applicationCommandOptionData2.minValue()) &&
                applicationCommandOptionData1.maxValue().equals(applicationCommandOptionData2.maxValue()) &&
                applicationCommandOptionData1.channelTypes().equals(applicationCommandOptionData2.channelTypes()) &&
                equalApplicationCommandOptionDataList(applicationCommandOptionData1.options().toOptional().orElse(ImmutableList.of()),
                        applicationCommandOptionData2.options().toOptional().orElse(ImmutableList.of())) &&
                equalApplicationCommandOptionChoiceDataList(applicationCommandOptionData1.choices().toOptional().orElse(ImmutableList.of()),
                        applicationCommandOptionData2.choices().toOptional().orElse(ImmutableList.of()));

    }

    private boolean equalApplicationCommandOptionChoiceDataList(List<ApplicationCommandOptionChoiceData> applicationCommandOptionChoiceData1,
                                                                List<ApplicationCommandOptionChoiceData> applicationCommandOptionChoiceData2) {
        if (applicationCommandOptionChoiceData1 == null && applicationCommandOptionChoiceData2 == null) {
            return true;
        } else if (applicationCommandOptionChoiceData1 != null && applicationCommandOptionChoiceData2 == null) {
            return false;
        } else if (applicationCommandOptionChoiceData1 == null) {
            return false;
        }
        if (applicationCommandOptionChoiceData1.size() != applicationCommandOptionChoiceData2.size()) {
            return false;
        }
        for (int i = 0; i < applicationCommandOptionChoiceData1.size(); i++) {
            if (!Objects.equals(applicationCommandOptionChoiceData1.get(i).name(), applicationCommandOptionChoiceData2.get(i).name())
                    || !Objects.equals(applicationCommandOptionChoiceData1.get(i).value(), applicationCommandOptionChoiceData2.get(i).value())) {
                return false;
            }
        }

        return true;

    }


    public ApplicationCommandRequest buildRequest() {
        return ApplicationCommandRequest.builder()
                .name(name)
                .description(description)
                .addAllOptions(options)
                .build();
    }
}
