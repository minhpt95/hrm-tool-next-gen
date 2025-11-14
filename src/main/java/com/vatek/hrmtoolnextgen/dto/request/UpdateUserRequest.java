package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "New role assignments")
    private Collection<RoleDto> roles;

    @Schema(description = "Whether the employee account is active")
    private Boolean active;
}

