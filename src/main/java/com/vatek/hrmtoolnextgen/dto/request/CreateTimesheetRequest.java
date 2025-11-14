package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateTimesheetRequest {
    private String title;
    private String description;
    @NotEmpty
    @NotNull
    private String projectId;

    @Min(1)
    @Max(8)
    private Integer workingHours;
    private ETimesheetType timesheetType;
//    @DateFormatConstraint
    private String workingDay;
}
