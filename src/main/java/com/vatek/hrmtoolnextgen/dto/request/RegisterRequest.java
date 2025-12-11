package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class RegisterRequest {

    @NotEmpty
    private String email;
    @NotNull
    private UserInfoDto userInfo;
    @NotEmpty
    @Schema(
            description = "Roles that define permissions for the employee",
            type = "array",
            implementation = EUserRole.class,
            example = "[\"USER\",\"HR\"]"
    )
    private Collection<EUserRole> roles;
}
