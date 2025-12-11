package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalTimesheetRequest {
    private Long id;
    private ETimesheetStatus timesheetStatus;
}
