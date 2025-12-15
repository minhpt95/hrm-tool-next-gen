package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for creating a new timesheet entry")
public class CreateTimesheetRequest {
    @Schema(description = "Optional title for the timesheet entry")
    private String title;
    
    @Schema(description = "Optional description or notes about the work performed")
    private String description;
    
    @NotEmpty
    @NotNull
    @Schema(description = "ID of the project this timesheet entry is associated with", required = true)
    private Long projectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    @NotNull
    @Schema(description = "Number of working hours (format: HH:mm)", example = "08:00", required = true)
    private LocalTime workingHours;
    
    @Schema(description = "Type of timesheet entry (e.g., REGULAR, OVERTIME)")
    private ETimesheetType timesheetType;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull
    @Schema(description = "Date of the work performed (format: yyyy-MM-dd)", example = "2024-01-15", required = true)
    private LocalDate workingDay;
}
