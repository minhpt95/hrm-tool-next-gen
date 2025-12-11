package com.vatek.hrmtoolnextgen.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to refresh access token")
public class RefreshTokenRequest {
    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token to exchange for new access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;
}

