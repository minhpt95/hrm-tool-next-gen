package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link DeviceMapping} (MapStruct impl {@code DeviceCrudMappingImpl}).
 *
 * <p>Covers the isDelete↔delete rename in both directions, the create-request and
 * partial-update paths, and NullValuePropertyMappingStrategy.IGNORE — which must leave
 * an existing target value untouched when the source property is null.
 */
class DeviceMappingTest {

    private DeviceMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new DeviceCrudMappingImpl();
    }

    // -------------------------------------------------------------------------
    // toDto / toEntity — including the isDelete <-> delete rename
    // -------------------------------------------------------------------------

    @Test
    void toDto_mapsAllScalarsAndDeleteFlag() {
        DeviceEntity entity = Fixtures.buildDevice(4L);
        entity.setId(4L);
        entity.setDelete(true);

        DeviceDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertEquals(4L, dto.getId());
        assertEquals("Device-4", dto.getName());
        assertEquals("Test device 4", dto.getDescription());
        assertEquals("SN-4", dto.getSerialNumber());
        assertEquals(EDeviceType.LAPTOP, dto.getType());
        assertEquals(EDeviceStatus.ACTIVE, dto.getStatus());
        assertEquals(Boolean.TRUE, dto.getIsDelete());
    }

    @Test
    void toEntity_mapsDeleteFlagBack() {
        DeviceDto dto = new DeviceDto();
        dto.setId(5L);
        dto.setName("Monitor");
        dto.setSerialNumber("SN-5");
        dto.setType(EDeviceType.LAPTOP);
        dto.setStatus(EDeviceStatus.ACTIVE);
        dto.setIsDelete(true);

        DeviceEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals(5L, entity.getId());
        assertEquals("Monitor", entity.getName());
        assertEquals("SN-5", entity.getSerialNumber());
        assertEquals(true, entity.isDelete());
        // Explicitly ignored in the mapping: the entity's own empty default survives.
        assertTrue(entity.getUsers().isEmpty());
    }

    @Test
    void toDto_and_toEntity_null_returnNull() {
        assertNull(mapping.toDto((DeviceEntity) null));
        assertNull(mapping.toEntity((DeviceDto) null));
    }

    // -------------------------------------------------------------------------
    // fromCreateRequest — identity/audit fields must stay unset
    // -------------------------------------------------------------------------

    @Test
    void fromCreateRequest_mapsScalarsAndLeavesIdentityUnset() {
        CreateDeviceDto request = CreateDeviceDto.builder()
                .name("Keyboard")
                .description("Mechanical")
                .serialNumber("SN-K1")
                .type(EDeviceType.LAPTOP)
                .status(EDeviceStatus.ACTIVE)
                .build();

        DeviceEntity entity = mapping.fromCreateRequest(request);

        assertNotNull(entity);
        assertEquals("Keyboard", entity.getName());
        assertEquals("Mechanical", entity.getDescription());
        assertEquals("SN-K1", entity.getSerialNumber());
        assertEquals(EDeviceType.LAPTOP, entity.getType());
        assertEquals(EDeviceStatus.ACTIVE, entity.getStatus());
        assertNull(entity.getId());
        assertTrue(entity.getUsers().isEmpty());
    }

    @Test
    void fromCreateRequest_null_returnsNull() {
        assertNull(mapping.fromCreateRequest(null));
    }

    // -------------------------------------------------------------------------
    // updateEntityFromRequest — in-place partial update
    // -------------------------------------------------------------------------

    @Test
    void updateEntityFromRequest_overwritesSuppliedFields() {
        DeviceEntity target = Fixtures.buildDevice(6L);
        target.setId(6L);

        UpdateDeviceDto request = UpdateDeviceDto.builder()
                .name("Renamed")
                .description("Updated description")
                .serialNumber("SN-NEW")
                .type(EDeviceType.LAPTOP)
                .status(EDeviceStatus.INACTIVE)
                .build();

        mapping.updateEntityFromRequest(request, target);

        assertEquals("Renamed", target.getName());
        assertEquals("Updated description", target.getDescription());
        assertEquals("SN-NEW", target.getSerialNumber());
        assertEquals(EDeviceStatus.INACTIVE, target.getStatus());
        assertEquals(6L, target.getId(), "id is ignored by the mapping and must survive");
    }

    @Test
    void updateEntityFromRequest_nullProperties_leaveExistingValues() {
        DeviceEntity target = Fixtures.buildDevice(7L);
        String originalName = target.getName();
        String originalSerial = target.getSerialNumber();

        // NullValuePropertyMappingStrategy.IGNORE: null source properties are skipped.
        UpdateDeviceDto request = UpdateDeviceDto.builder()
                .description("Only the description changes")
                .build();

        mapping.updateEntityFromRequest(request, target);

        assertEquals("Only the description changes", target.getDescription());
        assertEquals(originalName, target.getName());
        assertEquals(originalSerial, target.getSerialNumber());
    }

    @Test
    void updateEntityFromRequest_nullRequest_leavesTargetUntouched() {
        DeviceEntity target = Fixtures.buildDevice(8L);
        String originalName = target.getName();

        mapping.updateEntityFromRequest(null, target);

        assertEquals(originalName, target.getName());
    }

    // -------------------------------------------------------------------------
    // Collection and page overloads
    // -------------------------------------------------------------------------

    @Test
    void toDto_collections_mapEveryElement() {
        DeviceEntity a = Fixtures.buildDevice(1L);
        a.setId(1L);
        DeviceEntity b = Fixtures.buildDevice(2L);
        b.setId(2L);

        List<DeviceDto> list = mapping.toDto(List.of(a, b));
        Set<DeviceDto> set = mapping.toDto(Set.of(a));

        assertEquals(2, list.size());
        assertEquals(1L, list.get(0).getId());
        assertEquals(1, set.size());
    }

    @Test
    void toDtoPageable_mapsContent() {
        DeviceEntity entity = Fixtures.buildDevice(3L);
        entity.setId(3L);
        Page<DeviceEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);

        Page<DeviceDto> mapped = mapping.toDtoPageable(page);

        assertEquals(1, mapped.getTotalElements());
        assertEquals(3L, mapped.getContent().get(0).getId());
    }

    @Test
    void toEntity_ontoExistingTarget_mergesNonNullFields() {
        DeviceEntity target = Fixtures.buildDevice(20L);
        target.setId(20L);
        String originalName = target.getName();

        DeviceDto patch = new DeviceDto();
        patch.setDescription("Patched description");

        DeviceEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        assertEquals("Patched description", target.getDescription());
        assertEquals(originalName, target.getName(), "null source fields are ignored");
    }

    @Test
    void toDto_ontoExistingTarget_mergesFields() {
        DeviceEntity source = Fixtures.buildDevice(21L);
        source.setId(21L);
        DeviceDto target = new DeviceDto();

        DeviceDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals(21L, target.getId());
    }

    @Test
    void updateOverloads_nullSource_returnTargetUnchanged() {
        DeviceEntity entityTarget = Fixtures.buildDevice(22L);
        DeviceDto dtoTarget = new DeviceDto();

        assertSame(entityTarget, mapping.toEntity(null, entityTarget));
        assertSame(dtoTarget, mapping.toDto(null, dtoTarget));
    }

    @Test
    void nullCollections_returnNull() {
        assertNull(mapping.toDto((List<DeviceEntity>) null));
        assertNull(mapping.toEntity((List<DeviceDto>) null));
        assertNull(mapping.toDto((Set<DeviceEntity>) null));
        assertNull(mapping.toEntity((Set<DeviceDto>) null));
    }
}
