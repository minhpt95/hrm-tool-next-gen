package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.component.jwt.JwtProvider;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.LoginRequest;
import com.vatek.hrmtoolnextgen.dto.request.RegisterRequest;
import com.vatek.hrmtoolnextgen.dto.response.LoginResponse;
import com.vatek.hrmtoolnextgen.dto.response.RegisterResponse;
import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.mapping.UserMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;
    private final UserMapping userMapping;
    private final RoleRepository roleRepository;

    public LoginResponse login(LoginRequest loginRequest) {
        // Authentication manager will handle password validation
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipalDto principal = (UserPrincipalDto) authentication.getPrincipal();

        String jwt = jwtProvider.generateJwtToken(authentication);

        return LoginResponse
                .builder()
                .id(principal.getId())
                .accessToken(jwt)
                .email(principal.getEmail())
                .firstName(principal.getFirstName())
                .lastName(principal.getLastName())
                .roles(principal.getRoles())
                .build();
    }

    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest, UserPrincipalDto userPrincipal) {
        UserEntity userEntity = userRepository.findByEmail(registerRequest.getEmail()).orElse(null);

        if (userEntity != null) {
            throw new BadRequestException("Email already in use");
        }

        userEntity = userMapping.createUser(registerRequest);

        userEntity.setActive(true);
        if (userPrincipal == null) {
            userEntity.getUserInfo().setCreatedBy("SYSTEM");
        } else {
            userEntity.getUserInfo().setCreatedBy(String.valueOf(userPrincipal.getId()));
        }

        List<RoleEntity> roles = roleRepository.findByUserRoleIn(registerRequest.getRoles().stream().map(RoleDto::getUserRole).toList());

        if (!roles.isEmpty()) {
            userEntity.setRoles(roles);
        }

        String password = CommonUtils.randomPassword(12);

        userEntity.setPassword(passwordEncoder.encode(password));
        userEntity.setCreatedBy("SYSTEM");
        userEntity.setCreatedDate(ZonedDateTime.now());

        userEntity = userRepository.save(userEntity);

        return RegisterResponse
                .builder()
                .id(userEntity.getId())
                .email(userEntity.getEmail())
                .password(password)
                .build();
    }
}
