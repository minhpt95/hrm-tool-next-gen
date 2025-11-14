package com.vatek.hrmtoolnextgen.dto.user;
import com.vatek.hrmtoolnextgen.entity.jpa.user.RoleEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private UserInfoDto userInfo;
    private boolean isEnabled;
    private Collection<RoleDto> roles;
}
