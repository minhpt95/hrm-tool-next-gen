package com.vatek.hrmtoolnextgen.dto.request;

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
@Schema(description = "Payload for setting a new password for a user (admin operation)")
public class SetUserPasswordRequest {
    @NotNull
    @NotEmpty
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @Schema(description = "New password to set for the user (minimum 6 characters)", example = "newPassword123", required = true)
    private String newPassword;
}






























