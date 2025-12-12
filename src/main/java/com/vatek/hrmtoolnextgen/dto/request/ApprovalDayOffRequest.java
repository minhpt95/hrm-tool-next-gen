package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalDayOffRequest {
    @NotNull
    @Min(1)
    private Long userId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    private LocalDateTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    private LocalDateTime endTime;
    
    @NotEmpty
    @NotNull
    private EDayOffStatus status; // APPROVED or REJECTED
}

