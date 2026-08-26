package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link DeviceUserMapping} — flattens UserInfo onto the device-user DTO.
 */
class DeviceUserMappingTest {

    private DeviceUserMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new DeviceUserMappingImpl();
    }

    @Test
    void toDto_flattensUserInfoFields() {
        UserEntity user = Fixtures.buildUser(2L);
        user.setId(2L);
        user.getUserInfo().setAvatarUrl("https://example.com/a.png");

        DeviceUserDto dto = mapping.toDto(user);

        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals("First2", dto.getFirstName());
        assertEquals("Last2", dto.getLastName());
        assertEquals("https://example.com/a.png", dto.getAvatarUrl());
    }

    @Test
    void toDto_userWithoutUserInfo_leavesNameFieldsNull() {
        UserEntity user = new UserEntity();
        user.setId(3L);
        user.setEmail("no-info@example.com");

        DeviceUserDto dto = mapping.toDto(user);

        assertNotNull(dto);
        assertEquals(3L, dto.getId());
        assertEquals("no-info@example.com", dto.getEmail());
        assertNull(dto.getFirstName());
        assertNull(dto.getLastName());
        assertNull(dto.getAvatarUrl());
    }

    @Test
    void toDto_null_returnsNull() {
        assertNull(mapping.toDto(null));
    }

    @Test
    void toDtoList_mapsEveryElement() {
        List<DeviceUserDto> dtos = mapping.toDtoList(List.of(
                Fixtures.buildUser(1L), Fixtures.buildUser(2L)));

        assertEquals(2, dtos.size());
        assertEquals("First1", dtos.get(0).getFirstName());
        assertEquals("First2", dtos.get(1).getFirstName());
    }

    @Test
    void toDtoList_null_returnsNull() {
        assertNull(mapping.toDtoList(null));
    }
}
