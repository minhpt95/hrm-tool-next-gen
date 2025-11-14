package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.mapping.UserMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {
    private final UserRepository userRepository;
    private final UserMapping userMapping;

    public UserEntity findUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));
    }

    public UserEntity findUserById(Long id){
        return userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
    }

    @Transactional
    public Page<UserDto> getPageUsers(Pageable pageable){
        Page<UserEntity> entityPage = userRepository.findAll(pageable);
        return userMapping.toDtoPageable(entityPage);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest createUserRequest) {
        return null;
    }
}
