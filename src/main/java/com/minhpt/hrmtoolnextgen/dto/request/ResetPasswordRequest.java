package com.minhpt.hrmtoolnextgen.dto.request;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for resetting password using a reset token")
public class ResetPasswordRequest {
    @NotNull
    @NotEmpty
    @SafeString
    @Schema(description = "Password reset token received via email", example = "abc123xyz789", required = true)
    private String token;

    @NotNull
    @NotEmpty
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Schema(description = "New password (minimum 8 characters)", example = "newPassword123!", required = true)
    private String newPassword;
}






























