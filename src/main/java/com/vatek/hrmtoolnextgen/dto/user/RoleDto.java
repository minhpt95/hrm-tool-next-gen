package com.vatek.hrmtoolnextgen.dto.user;

import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleDto {
    private EUserRole userRole;
}
