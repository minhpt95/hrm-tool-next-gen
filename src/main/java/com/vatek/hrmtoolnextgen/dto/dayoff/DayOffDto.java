package com.vatek.hrmtoolnextgen.dto.dayoff;

import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EDayOffStatus status;
}

