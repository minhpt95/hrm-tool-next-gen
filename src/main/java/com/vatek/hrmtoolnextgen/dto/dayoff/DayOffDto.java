package com.vatek.hrmtoolnextgen.dto.dayoff;

import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
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
    private Integer numberOfHours;
    private Instant dateOff;
    private EDayOffType type;
    private EDayOffStatus status;
}

