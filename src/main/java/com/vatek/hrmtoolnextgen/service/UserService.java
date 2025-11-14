package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.mapping.UserMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {
    private final UserRepository userRepository;
    private final UserMapping userMapping;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserEntity findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));
    }

    public UserEntity findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getPageUsers(Pageable pageable){
        Page<UserEntity> entityPage = userRepository.findAll(pageable);
        return userMapping.toDtoPageable(entityPage);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        UserEntity userEntity = findUserById(id);
        return userMapping.toDto(userEntity);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("User with email '" + request.getEmail() + "' already exists");
        }

        // Check if identity card already exists
        if (request.getUserInfo() != null && request.getUserInfo().getIdentityCard() != null) {
            userRepository.findByEmailOrUserInfo_IdentityCard("", request.getUserInfo().getIdentityCard())
                    .ifPresent(u -> {
                        throw new BadRequestException("User with identity card '" + request.getUserInfo().getIdentityCard() + "' already exists");
                    });
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(request.getEmail());
        userEntity.setActive(true);
        
        // Generate random password
        String randomPassword = CommonUtils.randomPassword(12);
        userEntity.setPassword(passwordEncoder.encode(randomPassword));

        // Create user info
        if (request.getUserInfo() != null) {
            UserInfoEntity userInfoEntity = new UserInfoEntity();
            userInfoEntity.setFirstName(request.getUserInfo().getFirstName());
            userInfoEntity.setLastName(request.getUserInfo().getLastName());
            userInfoEntity.setIdentityCard(request.getUserInfo().getIdentityCard());
            userInfoEntity.setPhoneNumber1(request.getUserInfo().getPhoneNumber1());
            userInfoEntity.setCurrentAddress(request.getUserInfo().getCurrentAddress());
            userInfoEntity.setPermanentAddress(request.getUserInfo().getPermanentAddress());
            userInfoEntity.setAvatarUrl(request.getUserInfo().getAvatarUrl());
            userEntity.setUserInfo(userInfoEntity);
        }

        // Set roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            List<RoleEntity> roles = request.getRoles().stream()
                    .map(roleDto -> roleRepository.findByUserRole(roleDto.getUserRole()))
                    .filter(role -> role != null)
                    .collect(Collectors.toList());
            userEntity.setRoles(roles);
        }

        UserEntity savedEntity = userRepository.save(userEntity);
        log.info("Created user with id: {} and email: {}", savedEntity.getId(), savedEntity.getEmail());
        return userMapping.toDto(savedEntity);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        UserEntity userEntity = findUserById(id);

        // Check if email already exists (excluding current user)
        if (request.getEmail() != null && !request.getEmail().equals(userEntity.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(u -> {
                        if (!u.getId().equals(id)) {
                            throw new BadRequestException("User with email '" + request.getEmail() + "' already exists");
                        }
                    });
            userEntity.setEmail(request.getEmail());
        }

        // Update active status
        if (request.getActive() != null) {
            userEntity.setActive(request.getActive());
        }

        // Update user info
        if (request.getUserInfo() != null && userEntity.getUserInfo() != null) {
            UserInfoEntity userInfo = userEntity.getUserInfo();
            if (request.getUserInfo().getFirstName() != null) {
                userInfo.setFirstName(request.getUserInfo().getFirstName());
            }
            if (request.getUserInfo().getLastName() != null) {
                userInfo.setLastName(request.getUserInfo().getLastName());
            }
            if (request.getUserInfo().getIdentityCard() != null) {
                // Check if identity card already exists
                userRepository.findByEmailOrUserInfo_IdentityCard("", request.getUserInfo().getIdentityCard())
                        .ifPresent(u -> {
                            if (!u.getId().equals(id)) {
                                throw new BadRequestException("User with identity card '" + request.getUserInfo().getIdentityCard() + "' already exists");
                            }
                        });
                userInfo.setIdentityCard(request.getUserInfo().getIdentityCard());
            }
            if (request.getUserInfo().getPhoneNumber1() != null) {
                userInfo.setPhoneNumber1(request.getUserInfo().getPhoneNumber1());
            }
            if (request.getUserInfo().getCurrentAddress() != null) {
                userInfo.setCurrentAddress(request.getUserInfo().getCurrentAddress());
            }
            if (request.getUserInfo().getPermanentAddress() != null) {
                userInfo.setPermanentAddress(request.getUserInfo().getPermanentAddress());
            }
            if (request.getUserInfo().getAvatarUrl() != null) {
                userInfo.setAvatarUrl(request.getUserInfo().getAvatarUrl());
            }
        }

        // Update roles
        if (request.getRoles() != null) {
            List<RoleEntity> roles = request.getRoles().stream()
                    .map(roleDto -> roleRepository.findByUserRole(roleDto.getUserRole()))
                    .filter(role -> role != null)
                    .collect(Collectors.toList());
            userEntity.setRoles(roles);
        }

        UserEntity updatedEntity = userRepository.save(userEntity);
        log.info("Updated user with id: {}", updatedEntity.getId());
        return userMapping.toDto(updatedEntity);
    }

    @Transactional
    public void deleteUser(Long id) {
        UserEntity userEntity = findUserById(id);
        userEntity.setActive(false);
        userRepository.save(userEntity);
        log.info("Deactivated user with id: {}", id);
    }
}
