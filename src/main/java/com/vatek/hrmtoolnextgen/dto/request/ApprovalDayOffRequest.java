package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalDayOffRequest {
    @NotNull
    @Min(1)
    private Long userId;
    
    @NotNull
    private Instant dateOff;
    
    @NotNull
    private com.vatek.hrmtoolnextgen.enumeration.EDayOffType type;
    
    @NotEmpty
    @NotNull
    private EDayOffStatus status; // APPROVED or REJECTED
}

