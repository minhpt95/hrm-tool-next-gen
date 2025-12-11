package com.vatek.hrmtoolnextgen.dto.timesheet;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetDto {
    private String title;
    private String description;
    private Integer workingHours;
    private ETimesheetType timesheetType;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate workingDay;
    private ETimesheetStatus status;
    private ProjectDto projectEntity;
    private UserDto userEntity;
}
