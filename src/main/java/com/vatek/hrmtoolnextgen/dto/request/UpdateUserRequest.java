package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for updating an existing employee account")
public class UpdateUserRequest {
    @Schema(description = "Updated email address")
    private String email;

    @Schema(description = "Updated profile information")
    private UserInfoDto userInfo;

    @Schema(description = "Replacement avatar image file")
    private MultipartFile avatarImage;

    @NotEmpty
    @ArraySchema(
            arraySchema = @Schema(description = "Roles that define permissions for the employee"),
            schema = @Schema(implementation = EUserRole.class)
    )
    private Collection<EUserRole> roles;

    @Schema(description = "Whether the employee account is active")
    private Boolean active;
}

