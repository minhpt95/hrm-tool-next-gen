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
    @NotNull
    @Schema(description = "ID of the day off request to approve or reject", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @NotNull
    @Schema(description = "Approval status: APPROVED or REJECTED", requiredMode = Schema.RequiredMode.REQUIRED)
    private EDayOffStatus status; // APPROVED or REJECTED
}

