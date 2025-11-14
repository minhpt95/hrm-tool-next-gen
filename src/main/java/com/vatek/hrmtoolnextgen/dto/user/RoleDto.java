package com.vatek.hrmtoolnextgen.dto.user;

import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Role assigned to a user")
public class RoleDto {
    @Schema(description = "Role identifier")
    private EUserRole userRole;
}
