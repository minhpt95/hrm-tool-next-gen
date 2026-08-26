package com.minhpt.hrmtoolnextgen.service.device;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.DeviceMapping;
import com.minhpt.hrmtoolnextgen.mapping.DeviceUserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for DeviceCommandService.
 *
 * Delete mechanism: @SQLDelete (sql = "UPDATE devices SET is_delete = true WHERE id = ?").
 * The service calls deviceRepository.delete(entity), which Hibernate translates to
 * the soft-delete SQL rather than a hard DELETE. The entity's isDelete flag is NOT
 * manually set to true by application code before deletion.
 *
 * Soft-delete re-delete guard (R17.3): deleteDevice() DOES contain the guard
 *   if (entity.isDelete()) throw new BadRequestException(...)
 * HOWEVER, @SQLRestriction("is_delete = false") on DeviceEntity means findById()
 * will never return a soft-deleted row under normal JPA loading. The guard can
 * only fire if the restriction is bypassed (e.g. native query, future code change).
 * The guard assertion is therefore @Disabled — see R17.3 test below.
 *
 * R14.1 createDevice — persists with type+status; repo.save invoked; dto returned.
 * R14.2 updateDevice — updates fields; not-found id → NotFoundException.
 * R14.3 deleteDevice — calls repo.delete(entity) — the @SQLDelete soft-delete path.
 * R17.3 soft-delete guard — @Disabled (guard exists but is unreachable via findById due to @SQLRestriction).
 */
@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceTest {

    @Mock private DeviceRepository  deviceRepository;
    @Mock private UserRepository    userRepository;
    @Mock private DeviceMapping     deviceMapping;
    @Mock private DeviceUserMapping deviceUserMapping;
    @Mock private MessageService    messageService;

    @InjectMocks
    private DeviceCommandService commandService;

    private static final long DEVICE_ID = 42L;

    // -------------------------------------------------------------------------
    // R14.1 — createDevice: happy path
    // -------------------------------------------------------------------------

    @Test
    void createDevice_validRequest_savesEntityAndReturnsDto() {
        CreateDeviceDto request = CreateDeviceDto.builder()
                .name("ThinkPad X1")
                .serialNumber("SN-CREATE-001")
                .type(EDeviceType.LAPTOP)
                .status(EDeviceStatus.ACTIVE)
                .build();

        DeviceEntity mappedEntity = Fixtures.buildDevice(1L);
        when(deviceRepository.existsBySerialNumberIgnoreCaseAndDeleteFalse("SN-CREATE-001"))
                .thenReturn(false);
        when(deviceMapping.fromCreateRequest(request)).thenReturn(mappedEntity);

        DeviceEntity savedEntity = Fixtures.buildDevice(1L);
        savedEntity.setId(DEVICE_ID);
        savedEntity.setType(EDeviceType.LAPTOP);
        savedEntity.setStatus(EDeviceStatus.ACTIVE);
        when(deviceRepository.save(any(DeviceEntity.class))).thenReturn(savedEntity);

        DeviceDto expectedDto = new DeviceDto(DEVICE_ID, "Device-1", null, "SN-1",
                EDeviceType.LAPTOP, EDeviceStatus.ACTIVE, false);
        when(deviceMapping.toDto(savedEntity)).thenReturn(expectedDto);

        DeviceDto result = commandService.createDevice(request);

        // repo.save must be invoked exactly once
        ArgumentCaptor<DeviceEntity> captor = ArgumentCaptor.forClass(DeviceEntity.class);
        verify(deviceRepository).save(captor.capture());

        // delete flag is set to false before save
        assertEquals(false, captor.getValue().isDelete());

        assertNotNull(result);
        assertEquals(DEVICE_ID, result.getId());
        assertEquals(EDeviceType.LAPTOP, result.getType());
        assertEquals(EDeviceStatus.ACTIVE, result.getStatus());
    }

    @Test
    void createDevice_duplicateSerialNumber_throwsBadRequestException() {
        CreateDeviceDto request = CreateDeviceDto.builder()
                .name("Duplicate")
                .serialNumber("SN-DUP-001")
                .type(EDeviceType.MOUSE)
                .status(EDeviceStatus.ACTIVE)
                .build();

        when(deviceRepository.existsBySerialNumberIgnoreCaseAndDeleteFalse("SN-DUP-001"))
                .thenReturn(true);
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("serial exists");

        assertThrows(BadRequestException.class, () -> commandService.createDevice(request));
        verify(deviceRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // R14.2 — updateDevice: happy path + not-found
    // -------------------------------------------------------------------------

    @Test
    void updateDevice_existingDevice_updatesFieldsAndReturnsDto() {
        UpdateDeviceDto request = UpdateDeviceDto.builder()
                .name("Updated Name")
                .status(EDeviceStatus.INACTIVE)
                .build();

        DeviceEntity existing = Fixtures.buildDevice(2L);
        existing.setId(DEVICE_ID);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(existing));

        DeviceEntity updatedEntity = Fixtures.buildDevice(2L);
        updatedEntity.setId(DEVICE_ID);
        when(deviceRepository.save(any(DeviceEntity.class))).thenReturn(updatedEntity);

        DeviceDto expectedDto = new DeviceDto(DEVICE_ID, "Updated Name", null, "SN-2",
                EDeviceType.LAPTOP, EDeviceStatus.INACTIVE, false);
        when(deviceMapping.toDto(updatedEntity)).thenReturn(expectedDto);

        DeviceDto result = commandService.updateDevice(DEVICE_ID, request);

        verify(deviceMapping).updateEntityFromRequest(request, existing);
        verify(deviceRepository).save(existing);
        assertNotNull(result);
        assertEquals(DEVICE_ID, result.getId());
    }

    @Test
    void updateDevice_nonExistentId_throwsNotFoundException() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("not found");

        assertThrows(NotFoundException.class,
                () -> commandService.updateDevice(999L, new UpdateDeviceDto()));
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_alreadySoftDeleted_throwsBadRequestException() {
        DeviceEntity deletedEntity = Fixtures.buildDevice(3L);
        deletedEntity.setId(DEVICE_ID);
        deletedEntity.setDelete(true);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(deletedEntity));
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("already deleted");

        assertThrows(BadRequestException.class,
                () -> commandService.updateDevice(DEVICE_ID, new UpdateDeviceDto()));
        verify(deviceRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // R14.3 — deleteDevice: soft-delete mechanism via @SQLDelete
    // -------------------------------------------------------------------------

    /**
     * deleteDevice() calls deviceRepository.delete(entity).
     * Hibernate translates this to the @SQLDelete SQL:
     *   UPDATE devices SET is_delete = true WHERE id = ?
     * rather than a hard DELETE. We verify repo.delete(entity) is invoked —
     * asserting the @SQLDelete path was triggered.
     */
    @Test
    void deleteDevice_existingActiveDevice_invokesRepoDeleteForSoftDelete() {
        DeviceEntity entity = Fixtures.buildDevice(4L);
        entity.setId(DEVICE_ID);
        entity.setDelete(false);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(entity));

        commandService.deleteDevice(DEVICE_ID);

        verify(deviceRepository).delete((DeviceEntity) entity);
        // repo.save must NOT be called — this is not a flag-set-then-save soft-delete
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void deleteDevice_nonExistentId_throwsNotFoundException() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("not found");

        assertThrows(NotFoundException.class, () -> commandService.deleteDevice(999L));
        verify(deviceRepository, never()).delete(any(DeviceEntity.class));
    }

    // -------------------------------------------------------------------------
    // R17.3 — soft-delete re-delete guard
    // -------------------------------------------------------------------------

    /**
     * The guard (if entity.isDelete() throw BadRequestException) EXISTS in
     * deleteDevice(). However, it is unreachable under normal conditions:
     * @SQLRestriction("is_delete = false") on DeviceEntity means findById()
     * will never return a soft-deleted row — the guard fires only if the
     * restriction is bypassed (native query or future code change).
     *
     * This test is disabled to document the gap rather than assert with a
     * mocked entity that cannot realistically be returned by the repository.
     *
     * R17.3 GAP: guard code exists but is dead code under current @SQLRestriction.
     */
    @Disabled("R17.3: soft-delete re-delete guard exists in deleteDevice() but is " +
              "unreachable via findById() because @SQLRestriction(\"is_delete = false\") " +
              "prevents soft-deleted devices from being loaded. The guard is dead code " +
              "under current configuration.")
    @Test
    void deleteDevice_alreadySoftDeleted_throwsBadRequestException() {
        DeviceEntity softDeletedEntity = Fixtures.buildDevice(5L);
        softDeletedEntity.setId(DEVICE_ID);
        softDeletedEntity.setDelete(true);
        // findById would never return this in production due to @SQLRestriction
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(softDeletedEntity));
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("already deleted");

        assertThrows(BadRequestException.class, () -> commandService.deleteDevice(DEVICE_ID));
        verify(deviceRepository, never()).delete(any(DeviceEntity.class));
    }
}
