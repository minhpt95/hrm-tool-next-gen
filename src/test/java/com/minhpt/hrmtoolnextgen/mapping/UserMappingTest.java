package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.minhpt.hrmtoolnextgen.dto.request.RegisterRequest;
import com.minhpt.hrmtoolnextgen.dto.user.UserDto;
import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link UserMapping}, including the hand-written default methods
 * that translate between {@link RoleEntity} and {@link EUserRole} in both directions.
 */
class UserMappingTest {

    private UserMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new UserMappingImpl();
    }

    // -------------------------------------------------------------------------
    // Role converters — the default methods and their null branches
    // -------------------------------------------------------------------------

    @Test
    void mapRoleToEnum_returnsUnderlyingRole() {
        assertEquals(EUserRole.ADMIN, mapping.mapRoleToEnum(Fixtures.buildRole(EUserRole.ADMIN)));
    }

    @Test
    void mapRoleToEnum_null_returnsNull() {
        assertNull(mapping.mapRoleToEnum(null));
    }

    @Test
    void mapEnumToRole_wrapsInRoleEntity() {
        RoleEntity role = mapping.mapEnumToRole(EUserRole.HR);

        assertNotNull(role);
        assertEquals(EUserRole.HR, role.getUserRole());
    }

    @Test
    void mapEnumToRole_null_returnsNull() {
        assertNull(mapping.mapEnumToRole(null));
    }

    @Test
    void mapRolesToEnum_mapsEveryElement() {
        var result = mapping.mapRolesToEnum(List.of(
                Fixtures.buildRole(EUserRole.ADMIN), Fixtures.buildRole(EUserRole.USER)));

        assertEquals(List.of(EUserRole.ADMIN, EUserRole.USER), List.copyOf(result));
    }

    @Test
    void mapRolesToEnum_null_returnsNull() {
        assertNull(mapping.mapRolesToEnum(null));
    }

    @Test
    void mapEnumsToRoles_mapsEveryElement() {
        var result = mapping.mapEnumsToRoles(List.of(EUserRole.IT_ADMIN));

        assertEquals(1, result.size());
        assertEquals(EUserRole.IT_ADMIN, result.iterator().next().getUserRole());
    }

    @Test
    void mapEnumsToRoles_null_returnsNull() {
        assertNull(mapping.mapEnumsToRoles(null));
    }

    // -------------------------------------------------------------------------
    // toCustomDto — active maps onto enabled
    // -------------------------------------------------------------------------

    @Test
    void toCustomDto_mapsIdRolesAndEnabledFlag() {
        UserEntity user = Fixtures.buildUser(3L, Fixtures.buildRole(EUserRole.PROJECT_MANAGER));
        user.setId(3L);
        user.setActive(true);

        UserDto dto = mapping.toCustomDto(user);

        assertNotNull(dto);
        assertEquals(3L, dto.getId());
        assertEquals(user.getEmail(), dto.getEmail());
        assertTrue(dto.isEnabled());
        assertEquals(List.of(EUserRole.PROJECT_MANAGER), List.copyOf(dto.getRoles()));
    }

    @Test
    void toCustomDto_inactiveUser_mapsEnabledFalse() {
        UserEntity user = Fixtures.buildUser(4L);
        user.setId(4L);
        user.setActive(false);

        assertFalse(mapping.toCustomDto(user).isEnabled());
    }

    @Test
    void toCustomDto_null_returnsNull() {
        assertNull(mapping.toCustomDto(null));
    }

    // -------------------------------------------------------------------------
    // toDto / toEntity round trip
    // -------------------------------------------------------------------------

    @Test
    void toDto_mapsNestedUserInfo() {
        UserEntity user = Fixtures.buildUser(6L);
        user.setId(6L);

        UserDto dto = mapping.toDto(user);

        assertNotNull(dto);
        assertEquals(6L, dto.getId());
        assertNotNull(dto.getUserInfo());
        assertEquals("First6", dto.getUserInfo().getFirstName());
        assertEquals("Last6", dto.getUserInfo().getLastName());
    }

    @Test
    void toEntity_mapsNestedUserInfo() {
        UserDto dto = new UserDto();
        dto.setId(7L);
        dto.setEmail("round@example.com");
        dto.setUserInfo(UserInfoDto.builder().firstName("Grace").lastName("Hopper").build());

        UserEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals(7L, entity.getId());
        assertEquals("round@example.com", entity.getEmail());
        assertNotNull(entity.getUserInfo());
        assertEquals("Grace", entity.getUserInfo().getFirstName());
    }

    // -------------------------------------------------------------------------
    // createUser — from the registration request
    // -------------------------------------------------------------------------

    @Test
    void createUser_mapsEmailAndUserInfo() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setUserInfo(UserInfoDto.builder().firstName("Alan").lastName("Turing").build());
        request.setRoles(List.of(EUserRole.USER));

        UserEntity entity = mapping.createUser(request);

        assertNotNull(entity);
        assertEquals("new@example.com", entity.getEmail());
        assertNotNull(entity.getUserInfo());
        assertEquals("Alan", entity.getUserInfo().getFirstName());
    }

    /**
     * Pins a sharp edge of the generated mapper: {@code createUser} does NOT route roles
     * through the {@code @Named("enumToRole")} default method, so MapStruct emits a
     * RoleEntity with a null userRole. AuthAccountService overwrites roles from the
     * repository immediately afterwards, so this never reaches the database on the normal
     * path — but the mapper output alone is not a usable role.
     */
    @Test
    void createUser_rolesAreNotResolvedByTheMapper() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("roles@example.com");
        request.setRoles(List.of(EUserRole.USER));

        UserEntity entity = mapping.createUser(request);

        assertEquals(1, entity.getRoles().size());
        assertNull(entity.getRoles().get(0).getUserRole(),
                "generated mapper bypasses the enumToRole qualifier");
    }

    @Test
    void createUser_null_returnsNull() {
        assertNull(mapping.createUser(null));
    }

    // -------------------------------------------------------------------------
    // Collections and paging
    // -------------------------------------------------------------------------

    @Test
    void toDto_list_mapsEveryElement() {
        UserEntity a = Fixtures.buildUser(1L);
        a.setId(1L);
        UserEntity b = Fixtures.buildUser(2L);
        b.setId(2L);

        List<UserDto> dtos = mapping.toDto(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
    }

    @Test
    void toDtoPageable_mapsContent() {
        UserEntity user = Fixtures.buildUser(8L);
        user.setId(8L);
        Page<UserEntity> page = new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1);

        Page<UserDto> mapped = mapping.toDtoPageable(page);

        assertEquals(1, mapped.getTotalElements());
        assertEquals(8L, mapped.getContent().get(0).getId());
    }

    // -------------------------------------------------------------------------
    // @MappingTarget update overloads and Set overloads
    // -------------------------------------------------------------------------

    @Test
    void toEntity_ontoExistingTarget_mergesNonNullFields() {
        UserEntity target = Fixtures.buildUser(20L);
        target.setId(20L);
        String originalEmail = target.getEmail();

        UserDto patch = new UserDto();
        patch.setUserInfo(UserInfoDto.builder().firstName("Patched").build());

        UserEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        assertEquals("Patched", target.getUserInfo().getFirstName());
        assertEquals(originalEmail, target.getEmail(), "null source fields are ignored");
    }

    @Test
    void toDto_ontoExistingTarget_mergesFields() {
        UserEntity source = Fixtures.buildUser(21L);
        source.setId(21L);
        UserDto target = new UserDto();

        UserDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals(21L, target.getId());
    }

    @Test
    void updateOverloads_nullSource_returnTargetUnchanged() {
        UserEntity entityTarget = Fixtures.buildUser(22L);
        UserDto dtoTarget = new UserDto();

        assertSame(entityTarget, mapping.toEntity(null, entityTarget));
        assertSame(dtoTarget, mapping.toDto(null, dtoTarget));
    }

    @Test
    void setOverloads_mapEveryElement() {
        UserEntity user = Fixtures.buildUser(23L);
        user.setId(23L);

        Set<UserDto> dtos = mapping.toDto(Set.of(user));
        assertEquals(1, dtos.size());
        assertEquals(23L, dtos.iterator().next().getId());

        UserDto dto = new UserDto();
        dto.setId(24L);
        Set<UserEntity> entities = mapping.toEntity(Set.of(dto));
        assertEquals(1, entities.size());
        assertEquals(24L, entities.iterator().next().getId());

        assertNull(mapping.toDto((Set<UserEntity>) null));
        assertNull(mapping.toEntity((Set<UserDto>) null));
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapping.toDto((UserEntity) null));
        assertNull(mapping.toEntity((UserDto) null));
        assertNull(mapping.toDto((List<UserEntity>) null));
        assertNull(mapping.toEntity((List<UserDto>) null));
    }
}
