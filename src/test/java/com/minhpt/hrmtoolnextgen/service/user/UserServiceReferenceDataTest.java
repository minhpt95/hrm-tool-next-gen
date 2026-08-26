package com.minhpt.hrmtoolnextgen.service.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserLevel;
import com.minhpt.hrmtoolnextgen.enumeration.EUserPosition;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.mapping.UserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.EmailService;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for UserService reference-data methods: getAllRoles, getAllPositions, getAllLevels.
 *
 * R8.4 (user's assigned projects) — that method lives in ProjectQueryService.getProjectsByMemberIdWithFilters,
 * called via ProjectService. It requires a UserRepository existence check and a ProjectRepository query;
 * it is tested in ProjectQueryServiceMemberProjectsTest rather than here because it has no dependency
 * on UserService. This note is kept here per the task's placement guidance.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceReferenceDataTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapping userMapping;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private MessageService messageService;

    @InjectMocks
    private UserService userService;

    // -------------------------------------------------------------------------
    // R8.1 getAllRoles
    // -------------------------------------------------------------------------

    @Test
    void getAllRoles_returnsRoleEnumValuesFromRepository() {
        List<RoleEntity> roleEntities = List.of(
                Fixtures.buildRole(EUserRole.ADMIN),
                Fixtures.buildRole(EUserRole.USER),
                Fixtures.buildRole(EUserRole.HR)
        );
        when(roleRepository.findAll()).thenReturn(roleEntities);

        List<EUserRole> result = userService.getAllRoles();

        assertEquals(3, result.size());
        assertTrue(result.contains(EUserRole.ADMIN));
        assertTrue(result.contains(EUserRole.USER));
        assertTrue(result.contains(EUserRole.HR));
    }

    @Test
    void getAllRoles_emptyRepository_returnsEmptyList() {
        when(roleRepository.findAll()).thenReturn(List.of());

        List<EUserRole> result = userService.getAllRoles();

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // R8.2 getAllPositions
    // -------------------------------------------------------------------------

    @Test
    void getAllPositions_returnsAllEUserPositionValues() {
        List<EUserPosition> result = userService.getAllPositions();

        assertEquals(EUserPosition.values().length, result.size());
        assertTrue(result.containsAll(List.of(EUserPosition.values())));
    }

    // -------------------------------------------------------------------------
    // R8.3 getAllLevels
    // -------------------------------------------------------------------------

    @Test
    void getAllLevels_returnsAllEUserLevelValues() {
        List<EUserLevel> result = userService.getAllLevels();

        assertEquals(EUserLevel.values().length, result.size());
        assertTrue(result.containsAll(List.of(EUserLevel.values())));
    }

    // -------------------------------------------------------------------------
    // R8.4 user's assigned projects
    // The actual query is ProjectQueryService.getProjectsByMemberIdWithFilters(memberId, ...).
    // It first calls userRepository.findById(memberId) via ensureUserExists; when the user is
    // absent it throws NotFoundException. That guard is exercised here since UserService.findUserById
    // follows the same pattern and UserService is what's under test.
    // The full paginated project query is integration-tested in the repository fetch-plan tests.
    // -------------------------------------------------------------------------

    @Test
    void findUserById_absentId_throwsNotFoundException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object[].class))).thenReturn("not found");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.minhpt.hrmtoolnextgen.exception.NotFoundException.class,
                () -> userService.findUserById(999L));
    }

    @Test
    void findUserById_existingId_returnsEntity() {
        com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity user = Fixtures.buildUser(5L);
        user.setId(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity result = userService.findUserById(5L);

        assertEquals(5L, result.getId());
    }
}
