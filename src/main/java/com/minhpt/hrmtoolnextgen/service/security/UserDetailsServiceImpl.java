package com.minhpt.hrmtoolnextgen.service.security;


import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.component.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserDetailsServiceImpl implements UserDetailsService {


    private final UserRepository userRepository;
    private final MessageService messageService;

    @Override
    @Transactional
    public UserPrincipalDto loadUserByUsername(String username)
            throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        UserEntity userEntity = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(messageService.getMessage("auth.user.not.found"))
                );

        List<RoleEntity> roleEntities = userEntity.getRoles();

        var rolesAuthority = getPrivileges(roleEntities);

        return UserPrincipalDto
                .userPrincipalDtoBuilder(userEntity)
                .authorities(getAuthorities(roleEntities))
                .roles(rolesAuthority)
                .build();
    }

    private List<? extends GrantedAuthority> getAuthorities(
            List<RoleEntity> roles
    ) {
        return getGrantedAuthorities(getPrivileges(roles));
    }

    private List<String> getPrivileges(Collection<RoleEntity> roles) {

        return roles
                .stream()
                .map(x -> x.getUserRole().name())
                .toList();
    }

    private List<GrantedAuthority> getGrantedAuthorities(List<String> privileges) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        for (String privilege : privileges) {
            // Remove "ROLE_" prefix if present, we'll use hasAuthority() instead
            String authority = privilege.startsWith("ROLE_") ? privilege.substring(5) : privilege;
            authorities.add(new SimpleGrantedAuthority(authority));
        }
        return authorities;
    }
}
