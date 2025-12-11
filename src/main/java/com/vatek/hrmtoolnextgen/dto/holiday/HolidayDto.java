package com.vatek.hrmtoolnextgen.dto.holiday;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Holiday information")
public class HolidayDto {
    @Schema(description = "Holiday name")
    private String name;

    @Schema(description = "Holiday description")
    private String description;

    @Schema(description = "Holiday date")
    private LocalDate date;

    @Schema(description = "Holiday type (national, religious, etc.)")
    private String type;

    @Schema(description = "Whether it's a public holiday")
    private Boolean isPublic;

    @Schema(description = "Country code")
    private String country;

    @Schema(description = "Holiday locations")
    private List<String> locations;
}

