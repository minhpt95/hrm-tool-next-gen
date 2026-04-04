package com.minhpt.hrmtoolnextgen.dto.request;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;

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
@Schema(description = "Payload for approving or rejecting a day off request")
public class ApprovalDayOffRequest {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "Start date and time of the day off request (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T09:00", required = true)
    private LocalDateTime startTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "End date and time of the day off request (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T17:00", required = true)
    private LocalDateTime endTime;

    @NotNull
    @Schema(description = "Approval status: APPROVED or REJECTED", required = true)
    private EDayOffStatus status; // APPROVED or REJECTED
}

