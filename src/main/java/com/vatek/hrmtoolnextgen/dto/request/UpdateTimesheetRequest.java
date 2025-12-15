package com.vatek.hrmtoolnextgen.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for updating an existing timesheet entry")
public class UpdateTimesheetRequest extends CreateTimesheetRequest {
    @NotNull
    @Schema(description = "ID of the timesheet entry to update", required = true)
    private Long id;
}
