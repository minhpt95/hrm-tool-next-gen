package com.vatek.hrmtoolnextgen.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateTimesheetRequest extends CreateTimesheetRequest {
    @NotNull
    private Long id;
}
