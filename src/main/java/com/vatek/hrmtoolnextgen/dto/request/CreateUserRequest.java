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
@Schema(description = "Payload for creating a new employee account")
public class CreateUserRequest {

    @NotEmpty
    @Schema(description = "Unique email address used for login")
    private String email;

    @Schema(description = "Detailed profile information for the employee")
    private UserInfoDto userInfo;

    @Schema(description = "Optional avatar image file")
    private MultipartFile avatarImage;

    @NotEmpty
    @ArraySchema(
            schema = @Schema(implementation = EUserRole.class),
            arraySchema = @Schema(
                    description = "Roles that define permissions for the employee",
                    example = "[\"USER\",\"HR\"]"
            )
    )
    private Collection<EUserRole> roles;
}
