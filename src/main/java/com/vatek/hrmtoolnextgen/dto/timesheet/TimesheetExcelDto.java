package com.vatek.hrmtoolnextgen.dto.timesheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetExcelDto {
    private int no;
    private String date;
    private String taskDescription;
    private int workingHours;
}
