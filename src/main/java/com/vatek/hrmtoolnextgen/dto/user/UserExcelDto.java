package com.vatek.hrmtoolnextgen.dto.user;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetExcelDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserExcelDto {
    private String name;
    private List<TimesheetExcelDto> normalHours = new ArrayList<>();
    private List<TimesheetExcelDto> overtimeHours = new ArrayList<>();
}
