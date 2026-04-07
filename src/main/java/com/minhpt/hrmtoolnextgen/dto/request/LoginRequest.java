package com.minhpt.hrmtoolnextgen.dto.request;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for user authentication")
public class LoginRequest {
    @NotBlank(message = "Username is required")
    @SafeString
    @Schema(description = "User email address or username", example = "user@example.com", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "password123", required = true)
    private String password;
}
