package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalTimesheetRequest {
    @NotNull
    @Min(1)
    private Long id;

    @NotEmpty
    @NotNull
    private ETimesheetStatus timesheetStatus;
}
