package com.vatek.hrmtoolnextgen.enumeration;

import com.vatek.hrmtoolnextgen.constant.RoleConstant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

@Getter
@AllArgsConstructor
public enum EUserRole implements GrantedAuthority {
    ADMIN(RoleConstant.ADMIN),
    IT_ADMIN(RoleConstant.IT_ADMIN),
    PROJECT_MANAGER(RoleConstant.PROJECT_MANAGER),
    USER(RoleConstant.USER),;

    private final String authority;

    @Override
    public String getAuthority() {
        return authority;
    }
}
