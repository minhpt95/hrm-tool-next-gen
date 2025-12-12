package com.vatek.hrmtoolnextgen.dto.request;

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
    private String startTime; // Format: dd/MM/yyyy HH:mm
    
    @NotEmpty
    @NotNull
    private String endTime; // Format: dd/MM/yyyy HH:mm
}

