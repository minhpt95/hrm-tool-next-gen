package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minhpt.hrmtoolnextgen.dto.user.UserInfoDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link UserInfoMapping} — a straight same-name field mapper.
 */
class UserInfoMappingTest {

    private UserInfoMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new UserInfoMappingImpl();
    }

    @Test
    void toDto_copiesEveryField() {
        UserInfoEntity entity = Fixtures.buildUserInfo(5L);
        entity.setAvatarUrl("https://example.com/x.png");
        entity.setBirthDate(LocalDate.of(1990, 4, 3));

        UserInfoDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertEquals("First5", dto.getFirstName());
        assertEquals("Last5", dto.getLastName());
        assertEquals("ID-5", dto.getIdentityCard());
        assertEquals(entity.getPhoneNumber1(), dto.getPhoneNumber1());
        assertEquals("Hanoi", dto.getCurrentAddress());
        assertEquals("Hanoi", dto.getPermanentAddress());
        assertEquals("https://example.com/x.png", dto.getAvatarUrl());
        assertEquals(LocalDate.of(2026, 1, 1), dto.getOnboardDate());
        assertEquals(LocalDate.of(1990, 4, 3), dto.getBirthDate());
    }

    @Test
    void toEntity_copiesEveryField() {
        UserInfoDto dto = UserInfoDto.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .identityCard("ID-99")
                .phoneNumber1("0900000099")
                .currentAddress("London")
                .permanentAddress("London")
                .onboardDate(LocalDate.of(2026, 2, 2))
                .birthDate(LocalDate.of(1815, 12, 10))
                .build();

        UserInfoEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals("Ada", entity.getFirstName());
        assertEquals("Lovelace", entity.getLastName());
        assertEquals("ID-99", entity.getIdentityCard());
        assertEquals("London", entity.getCurrentAddress());
        assertEquals(LocalDate.of(1815, 12, 10), entity.getBirthDate());
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapping.toDto((UserInfoEntity) null));
        assertNull(mapping.toEntity((UserInfoDto) null));
        assertNull(mapping.toDto((List<UserInfoEntity>) null));
        assertNull(mapping.toEntity((List<UserInfoDto>) null));
    }

    // -------------------------------------------------------------------------
    // @MappingTarget update overloads — merge onto an existing instance
    // -------------------------------------------------------------------------

    @Test
    void toEntity_ontoExistingTarget_overwritesOnlyNonNullSourceFields() {
        UserInfoEntity target = Fixtures.buildUserInfo(1L);
        String originalIdentityCard = target.getIdentityCard();
        String originalPhone = target.getPhoneNumber1();

        UserInfoDto patch = UserInfoDto.builder().firstName("Patched").build();

        UserInfoEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        assertEquals("Patched", target.getFirstName());
        assertEquals("Last1", target.getLastName(), "null source fields are ignored");
        assertEquals(originalIdentityCard, target.getIdentityCard());
        assertEquals(originalPhone, target.getPhoneNumber1());
    }

    @Test
    void toEntity_ontoExistingTarget_nullSource_returnsTargetUnchanged() {
        UserInfoEntity target = Fixtures.buildUserInfo(2L);

        UserInfoEntity result = mapping.toEntity(null, target);

        assertSame(target, result);
        assertEquals("First2", target.getFirstName());
    }

    @Test
    void toDto_ontoExistingTarget_mergesFields() {
        UserInfoDto target = UserInfoDto.builder().firstName("Old").lastName("Name").build();
        UserInfoEntity source = Fixtures.buildUserInfo(3L);

        UserInfoDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals("First3", target.getFirstName());
        assertEquals("Last3", target.getLastName());
    }

    @Test
    void toDto_ontoExistingTarget_nullSource_returnsTargetUnchanged() {
        UserInfoDto target = UserInfoDto.builder().firstName("Kept").build();

        assertSame(target, mapping.toDto(null, target));
        assertEquals("Kept", target.getFirstName());
    }

    // -------------------------------------------------------------------------
    // Set overloads
    // -------------------------------------------------------------------------

    @Test
    void setOverloads_mapEveryElement() {
        Set<UserInfoDto> dtos = mapping.toDto(Set.of(Fixtures.buildUserInfo(4L)));
        assertEquals(1, dtos.size());
        assertEquals("First4", dtos.iterator().next().getFirstName());

        Set<UserInfoEntity> entities = mapping.toEntity(
                Set.of(UserInfoDto.builder().firstName("SetName").build()));
        assertEquals(1, entities.size());
        assertEquals("SetName", entities.iterator().next().getFirstName());

        assertNull(mapping.toDto((Set<UserInfoEntity>) null));
        assertNull(mapping.toEntity((Set<UserInfoDto>) null));
    }

    @Test
    void toDto_list_mapsEveryElement() {
        List<UserInfoDto> dtos = mapping.toDto(List.of(
                Fixtures.buildUserInfo(1L), Fixtures.buildUserInfo(2L)));

        assertEquals(2, dtos.size());
        assertEquals("First1", dtos.get(0).getFirstName());
    }
}
