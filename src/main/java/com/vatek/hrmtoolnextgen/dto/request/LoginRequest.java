package com.vatek.hrmtoolnextgen.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for user authentication")
public class LoginRequest {
    @Schema(description = "User email address or username", example = "user@example.com", required = true)
    private String username;
    
    @Schema(description = "User password", example = "password123", required = true)
    private String password;
}
