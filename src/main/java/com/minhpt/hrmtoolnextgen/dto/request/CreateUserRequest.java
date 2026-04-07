package com.minhpt.hrmtoolnextgen.dto.request;

import java.util.Collection;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating a new employee account")
public class CreateUserRequest {

    @NotEmpty
    @Email(message = "Email must be a valid email address")
    @SafeString
    @Schema(description = "Unique email address used for login")
    private String email;

    @Valid
    @Schema(description = "Detailed profile information for the employee")
    private UserInfoDto userInfo;

    @Schema(description = "Optional avatar image file")
    private MultipartFile avatarImage;

    @NotEmpty
    @ArraySchema(
            schema = @Schema(implementation = EUserRole.class),
            arraySchema = @Schema(
                    description = "Roles that define permissions for the employee",
                    example = "[\"USER\",\"HR\"]",
                    implementation = List.class
            )
    )
    private Collection<EUserRole> roles;
}
