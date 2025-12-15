package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for creating a new day off request")
public class CreateDayOffRequest {
    @NotEmpty
    @NotNull
    @Schema(description = "Title of the day off request", example = "Annual Leave", required = true)
    private String requestTitle;
    
    @NotEmpty
    @NotNull
    @Schema(description = "Reason for the day off request", example = "Family vacation", required = true)
    private String requestReason;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "Start date and time of the day off (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T09:00", required = true)
    private LocalDateTime startTime;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm")
    @NotNull
    @Schema(description = "End date and time of the day off (format: yyyy-MM-dd'T'HH:mm)", example = "2024-01-15T17:00", required = true)
    private LocalDateTime endTime;
}

