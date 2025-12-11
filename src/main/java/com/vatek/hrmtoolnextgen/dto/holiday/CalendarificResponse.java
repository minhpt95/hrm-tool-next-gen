package com.vatek.hrmtoolnextgen.dto.holiday;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalendarificResponse {
    @JsonProperty("meta")
    private Meta meta;

    @JsonProperty("response")
    private Response response;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        @JsonProperty("code")
        private Integer code;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        @JsonProperty("holidays")
        private List<CalendarificHoliday> holidays;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalendarificHoliday {
        @JsonProperty("name")
        private String name;

        @JsonProperty("description")
        private String description;

        @JsonProperty("country")
        private Country country;

        @JsonProperty("date")
        private HolidayDate date;

        @JsonProperty("type")
        private List<String> types;

        @JsonProperty("locations")
        private String locations;

        @JsonProperty("states")
        private String states;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Country {
        @JsonProperty("id")
        private String id;

        @JsonProperty("name")
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HolidayDate {
        @JsonProperty("iso")
        private String iso;

        @JsonProperty("datetime")
        private DateTime datetime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateTime {
        @JsonProperty("year")
        private Integer year;

        @JsonProperty("month")
        private Integer month;

        @JsonProperty("day")
        private Integer day;
    }
}

