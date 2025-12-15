package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for approving or rejecting a day off request")
public class ApprovalDayOffRequest {
    @NotNull
    @Min(1)
    @Schema(description = "ID of the user who submitted the day off request", required = true)
    private Long userId;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "Start date and time of the day off request (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T09:00", required = true)
    private LocalDateTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "End date and time of the day off request (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T17:00", required = true)
    private LocalDateTime endTime;
    
    @NotEmpty
    @NotNull
    @Schema(description = "Approval status: APPROVED or REJECTED", required = true)
    private EDayOffStatus status; // APPROVED or REJECTED
}

