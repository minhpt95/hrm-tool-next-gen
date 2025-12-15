package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for approving or rejecting a timesheet entry")
public class ApprovalTimesheetRequest {
    @NotNull
    @Min(1)
    @Schema(description = "ID of the timesheet entry to approve or reject", required = true)
    private Long id;

    @NotEmpty
    @NotNull
    @Schema(description = "Approval status (e.g., APPROVED, REJECTED)", required = true)
    private ETimesheetStatus timesheetStatus;
}
