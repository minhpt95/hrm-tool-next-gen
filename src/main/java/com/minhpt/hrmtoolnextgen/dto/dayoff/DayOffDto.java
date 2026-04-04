package com.minhpt.hrmtoolnextgen.dto.dayoff;

import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
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
    private Long requestId;
    private String requesterName;
    private String requesterEmail;
    private String requestTitle;
    private String requestReason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private EDayOffStatus status;
}

