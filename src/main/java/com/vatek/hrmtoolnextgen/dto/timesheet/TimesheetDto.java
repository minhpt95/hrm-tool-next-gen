package com.vatek.hrmtoolnextgen.dto.timesheet;


import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZonedDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetDto {
    private String title;
    private String description;
    private Integer workingHours;
    private ETimesheetType timesheetType;
    private ZonedDateTime workingDay;
    private ETimesheetStatus status;
    private ProjectDto projectEntity;
    private UserDto userEntity;
}
