package com.vatek.hrmtoolnextgen.dto.dayoff;

import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayOffDto {
    private Long userId;
    private String userName;
    private String userEmail;
    private String requestTitle;
    private String requestReason;
    private Instant startTime;
    private Instant endTime;
    private Instant dateOff; // Derived from startTime for backward compatibility
    private EDayOffStatus status;
}

