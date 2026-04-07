package com.minhpt.hrmtoolnextgen.dto.request;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for user registration")
public class RegisterRequest {

    @NotEmpty
    @Email(message = "Email must be a valid email address")
    @SafeString
    @Schema(description = "Unique email address used for login", example = "user@example.com", required = true)
    private String email;

    @NotNull
    @Valid
    @Schema(description = "Detailed profile information for the user", required = true)
    private UserInfoDto userInfo;

    @NotEmpty
    @ArraySchema(
            schema = @Schema(implementation = EUserRole.class),
            arraySchema = @Schema(
                    description = "Roles that define permissions for the user",
                    example = "[\"USER\",\"HR\"]",
                    required = true
            )
    )
    private Collection<EUserRole> roles;
}
