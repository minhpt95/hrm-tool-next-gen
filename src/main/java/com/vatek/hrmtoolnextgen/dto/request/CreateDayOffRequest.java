package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
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
public class CreateDayOffRequest {
    @NotEmpty
    @NotNull
    private String requestTitle;
    
    @NotEmpty
    @NotNull
    private String requestReason;
    
    @NotEmpty
    @NotNull
    private String dateOff; // Format: dd/MM/yyyy
    
    @NotNull
    private EDayOffType type; // FULL, PARTIAL
    
    private Integer numberOfHours; // Required for PARTIAL type, must be > 0 and < 8
}

