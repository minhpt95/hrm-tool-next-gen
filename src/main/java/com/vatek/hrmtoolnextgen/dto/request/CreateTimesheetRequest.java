package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTimesheetRequest {
    private String title;
    private String description;
    @NotEmpty
    @NotNull
    private Long projectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @NotNull
    private LocalTime workingHours;
    private ETimesheetType timesheetType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull
    private LocalDate workingDay;
}
