package com.minhpt.hrmtoolnextgen.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.constant.CommonConstant;
import com.minhpt.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserLevel;
import com.minhpt.hrmtoolnextgen.enumeration.EUserPosition;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserService {
    private final UserRepository userRepository;
    private final UserMapping userMapping;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final MessageService messageService;

    public UserEntity findUserByEmail(String email) {
        log.debug("Finding user by email: {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("user.not.found.email", email)));
    }

    public UserEntity findUserById(Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("user.not.found", id)));
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserDto> getPageUsers(PaginationRequest paginationRequest) {
        log.debug("Getting page users - page: {}, size: {}", paginationRequest.getPage(), paginationRequest.getSize());
        Page<UserEntity> entityPage = userRepository.findAll(CommonUtils.buildPageable(paginationRequest));
        Page<UserDto> dtoPage = userMapping.toDtoPageable(entityPage);
        return CommonUtils.buildPaginationResponse(dtoPage, paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<UserDto> getAllUsersForAdmin(
            PaginationRequest paginationRequest,
            String name,
            String email) {
        log.debug("Getting all users for admin - name: {}, email: {}", name, email);

        // Build specification for filtering
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by email if provided
            if (StringUtils.hasText(email)) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                ));
            }

            // Filter by name (firstName or lastName) if provided
            if (StringUtils.hasText(name)) {
                String namePattern = "%" + name.toLowerCase() + "%";
                var userInfoPath = root.get("userInfo");
                Predicate firstNamePredicate = cb.and(
                        cb.isNotNull(userInfoPath),
                        cb.like(
                                cb.lower(userInfoPath.get("firstName")),
                                namePattern
                        )
                );
                Predicate lastNamePredicate = cb.and(
                        cb.isNotNull(userInfoPath),
                        cb.like(
                                cb.lower(userInfoPath.get("lastName")),
                                namePattern
                        )
                );
                predicates.add(cb.or(firstNamePredicate, lastNamePredicate));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        // Build pageable with default sort by id asc
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, "id", "ASC");

        Page<UserEntity> entityPage = userRepository.findAll(spec, Objects.requireNonNull(pageable));
        Page<UserDto> dtoPage = userMapping.toDtoPageable(entityPage);

        // Build pagination request for response
        String actualSortBy = paginationRequest.getSortBy() != null && !paginationRequest.getSortBy().isBlank()
                ? paginationRequest.getSortBy() : "id";
        String actualDirection = paginationRequest.getDirection() != null && !paginationRequest.getDirection().isBlank()
                ? paginationRequest.getDirection() : "ASC";
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest, actualSortBy, actualDirection);

        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        log.debug("Getting user DTO by id: {}", id);
        UserEntity userEntity = findUserById(id);
        return userMapping.toDto(userEntity);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException(messageService.getMessage("user.email.exists", request.getEmail()));
        }

        // Check if identity card already exists
        if (request.getUserInfo() != null && request.getUserInfo().getIdentityCard() != null) {
            userRepository.findByEmailOrUserInfo_IdentityCard("", request.getUserInfo().getIdentityCard())
                    .ifPresent(u -> {
                        throw new BadRequestException(messageService.getMessage("user.identity.card.exists", request.getUserInfo().getIdentityCard()));
                    });
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(request.getEmail());
        userEntity.setActive(true);

        // Generate random password
        String randomPassword = CommonUtils.randomPassword(CommonConstant.DEFAULT_PASSWORD_LENGTH);
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
            userInfoEntity.setOnboardDate(request.getUserInfo().getOnboardDate());
            userInfoEntity.setBirthDate(request.getUserInfo().getBirthDate());
            userEntity.setUserInfo(userInfoEntity);
        }

        // Set roles
        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            var roles = request.getRoles().stream()
                    .map(roleRepository::findByUserRole)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            userEntity.setRoles(roles);
        }

        UserEntity savedEntity = userRepository.save(userEntity);
        log.info("Created user with id: {} and email: {}", savedEntity.getId(), savedEntity.getEmail());

        // Send welcome email with credentials
        String userName = savedEntity.getUserInfo() != null
                ? (savedEntity.getUserInfo().getFirstName() + " " + savedEntity.getUserInfo().getLastName()).trim()
                : savedEntity.getEmail();
        emailService.sendWelcomeEmail(savedEntity.getEmail(), userName, randomPassword);

        return userMapping.toDto(savedEntity);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        log.info("Updating user with id: {}", id);
        UserEntity userEntity = findUserById(id);

        // Check if email already exists (excluding current user)
        if (request.getEmail() != null && !request.getEmail().equals(userEntity.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(u -> {
                        if (!u.getId().equals(id)) {
                            throw new BadRequestException(messageService.getMessage("user.email.exists", request.getEmail()));
                        }
                    });
            userEntity.setEmail(request.getEmail());
        }

        // Update active status
        if (request.getActive() != null) {
            userEntity.setActive(Boolean.TRUE.equals(request.getActive()));
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
                                throw new BadRequestException(messageService.getMessage("user.identity.card.exists", request.getUserInfo().getIdentityCard()));
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
            if (request.getUserInfo().getOnboardDate() != null) {
                userInfo.setOnboardDate(request.getUserInfo().getOnboardDate());
            }
            if (request.getUserInfo().getBirthDate() != null) {
                userInfo.setBirthDate(request.getUserInfo().getBirthDate());
            }
        }

        // Update roles
        if (request.getRoles() != null) {
            var roles = request.getRoles().stream()
                    .map(roleRepository::findByUserRole)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            userEntity.setRoles(roles);
        }

        UserEntity updatedEntity = userRepository.save(Objects.requireNonNull(userEntity));
        log.info("Updated user with id: {}", updatedEntity.getId());
        return userMapping.toDto(updatedEntity);
    }

    @Transactional
    public void deleteUser(Long id) {
        log.info("Deactivating user with id: {}", id);
        UserEntity userEntity = findUserById(id);
        userEntity.setActive(false);
        userRepository.save(userEntity);
        log.info("Deactivated user with id: {}", id);
    }

    @Transactional
    public void setUserPassword(Long id, String newPassword) {
        log.info("Setting password for user with id: {}", id);
        UserEntity userEntity = findUserById(id);
        userEntity.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(userEntity);
        log.info("Password updated for user with id: {}", id);
    }

    @Transactional(readOnly = true)
    public List<EUserRole> getAllRoles() {
        log.debug("Getting all roles");
        return roleRepository.findAll()
                .stream()
                .map(role -> role.getUserRole())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EUserPosition> getAllPositions() {
        return List.of(EUserPosition.values());
    }

    @Transactional(readOnly = true)
    public List<EUserLevel> getAllLevels() {
        return List.of(EUserLevel.values());
    }
}
