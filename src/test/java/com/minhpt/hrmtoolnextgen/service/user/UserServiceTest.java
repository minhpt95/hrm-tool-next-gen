package com.minhpt.hrmtoolnextgen.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateUserRequest;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for UserService — plain Mockito, no Spring context.
 *
 * messageService stubs are only added when the code path under test actually
 * reaches a getMessage call (i.e. error/not-found paths). Happy-path tests
 * do NOT stub messageService because it is never invoked there.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapping userMapping;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private MessageService messageService;

    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // R7.1 createUser — success path (no userInfo)
    // messageService is NOT called on the success path → no stub needed.
    // -------------------------------------------------------------------------

    @Test
    void createUser_success_savesEntityAndReturnsDto() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("new@example.com");
        req.setRoles(List.of(EUserRole.USER));

        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findByUserRole(EUserRole.USER)).thenReturn(Fixtures.buildRole(EUserRole.USER));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userMapping.toDto(any(UserEntity.class))).thenReturn(new UserDto());

        userService.createUser(req);

        verify(userRepository).save(any(UserEntity.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // R7.1 createUser — success path with userInfo
    // -------------------------------------------------------------------------

    @Test
    void createUser_withUserInfo_setsProfileFieldsAndSaves() {
        UserInfoDto infoDto = UserInfoDto.builder()
                .firstName("First")
                .lastName("Last")
                .identityCard("ID-999")
                .phoneNumber1("0909090909")
                .build();

        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("info@example.com");
        req.setUserInfo(infoDto);
        req.setRoles(List.of(EUserRole.USER));

        when(userRepository.findByEmail("info@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailOrUserInfo_IdentityCard("", "ID-999")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findByUserRole(EUserRole.USER)).thenReturn(Fixtures.buildRole(EUserRole.USER));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(userMapping.toDto(any(UserEntity.class))).thenReturn(new UserDto());

        userService.createUser(req);

        verify(userRepository).save(any(UserEntity.class));
        verify(emailService).sendWelcomeEmail(anyString(), anyString(), anyString());
    }

    // -------------------------------------------------------------------------
    // R7.7 createUser — uniqueness conflict: email already exists
    // -------------------------------------------------------------------------

    @Test
    void createUser_emailAlreadyExists_throwsBadRequestException() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("dup@example.com");
        req.setRoles(List.of(EUserRole.USER));

        when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(Fixtures.buildUser(1L)));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("email exists");

        assertThrows(BadRequestException.class, () -> userService.createUser(req));
    }

    // -------------------------------------------------------------------------
    // R7.7 createUser — uniqueness conflict: identity card already exists
    // -------------------------------------------------------------------------

    @Test
    void createUser_identityCardAlreadyExists_throwsBadRequestException() {
        UserInfoDto infoDto = UserInfoDto.builder().identityCard("ID-DUP").build();
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("unique@example.com");
        req.setUserInfo(infoDto);
        req.setRoles(List.of(EUserRole.USER));

        when(userRepository.findByEmail("unique@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailOrUserInfo_IdentityCard("", "ID-DUP"))
                .thenReturn(Optional.of(Fixtures.buildUser(2L)));
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("identity card exists");

        assertThrows(BadRequestException.class, () -> userService.createUser(req));
    }

    // -------------------------------------------------------------------------
    // R7.3 updateUser — profile fields updated
    // findUserById is called first; user IS found → messageService is NOT called.
    // -------------------------------------------------------------------------

    @Test
    void updateUser_updatesProfileFieldsAndSaves() {
        UserEntity existing = Fixtures.buildUser(10L);
        existing.setId(10L);

        UpdateUserRequest req = new UpdateUserRequest();
        req.setUserInfo(UserInfoDto.builder().firstName("NewFirst").lastName("NewLast").build());
        req.setRoles(List.of(EUserRole.HR));

        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(roleRepository.findByUserRole(EUserRole.HR)).thenReturn(Fixtures.buildRole(EUserRole.HR));
        when(userRepository.save(any(UserEntity.class))).thenReturn(existing);
        when(userMapping.toDto(any(UserEntity.class))).thenReturn(new UserDto());

        userService.updateUser(10L, req);

        verify(userRepository).save(existing);
    }

    // -------------------------------------------------------------------------
    // R7.4, R17.1 deleteUser — soft-deactivation (sets active=false, calls save)
    // User IS found → messageService is NOT called on this path.
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_setsActiveFalseAndSaves() {
        UserEntity existing = Fixtures.buildUser(20L);
        existing.setId(20L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(UserEntity.class))).thenReturn(existing);

        userService.deleteUser(20L);

        verify(userRepository).save(existing);
        assertFalse(existing.isActive());
    }

    // -------------------------------------------------------------------------
    // R17.3 deleteUser — not-found guard
    // -------------------------------------------------------------------------

    @Test
    void deleteUser_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("not found");

        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
    }

    // -------------------------------------------------------------------------
    // R7.5 setUserPassword — encodes and saves
    // User IS found → messageService is NOT called on this path.
    // -------------------------------------------------------------------------

    @Test
    void setUserPassword_encodesAndSaves() {
        UserEntity existing = Fixtures.buildUser(30L);
        existing.setId(30L);

        when(userRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newSecret")).thenReturn("encoded-new");
        when(userRepository.save(any(UserEntity.class))).thenReturn(existing);

        userService.setUserPassword(30L, "newSecret");

        verify(passwordEncoder).encode("newSecret");
        verify(userRepository).save(existing);
        assertEquals("encoded-new", existing.getPassword());
    }

    // -------------------------------------------------------------------------
    // R17.3 setUserPassword — not-found guard
    // -------------------------------------------------------------------------

    @Test
    void setUserPassword_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(998L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("not found");

        assertThrows(NotFoundException.class, () -> userService.setUserPassword(998L, "any"));
    }

    // -------------------------------------------------------------------------
    // R17.3 getUserById — not-found guard
    // -------------------------------------------------------------------------

    @Test
    void getUserById_userNotFound_throwsNotFoundException() {
        when(userRepository.findById(997L)).thenReturn(Optional.empty());
        when(messageService.getMessage(anyString(), any(Object[].class))).thenReturn("not found");

        assertThrows(NotFoundException.class, () -> userService.getUserById(997L));
    }
}
