package com.minhpt.hrmtoolnextgen.service.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.DeviceMapping;
import com.minhpt.hrmtoolnextgen.mapping.DeviceUserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for DeviceQueryService.
 *
 * R14.4 getDeviceById — existing → dto; absent → NotFoundException.
 * R14.5 getAllDevices  — paginated envelope; repo queried with spec + pageable.
 * R15.2 getDeviceUsers — existing device → list; absent → NotFoundException.
 *
 * Not-found exception type: NotFoundException (from deviceRepository.findById returning empty).
 * getDeviceUsers not-found exception type: NotFoundException (from findByIdWithUsers returning empty).
 */
@ExtendWith(MockitoExtension.class)
class DeviceQueryServiceTest {

    @Mock private DeviceRepository  deviceRepository;
    @Mock private DeviceMapping     deviceMapping;
    @Mock private DeviceUserMapping deviceUserMapping;
    @Mock private MessageService    messageService;

    @InjectMocks
    private DeviceQueryService queryService;

    private static final long DEVICE_ID = 77L;

    // -------------------------------------------------------------------------
    // R14.4 — getDeviceById
    // -------------------------------------------------------------------------

    @Test
    void getDeviceById_existingDevice_returnsDto() {
        DeviceEntity entity = Fixtures.buildDevice(1L);
        entity.setId(DEVICE_ID);
        when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(entity));

        DeviceDto expectedDto = new DeviceDto(DEVICE_ID, "Device-1", null, "SN-1",
                EDeviceType.LAPTOP, EDeviceStatus.ACTIVE, false);
        when(deviceMapping.toDto(entity)).thenReturn(expectedDto);

        DeviceDto result = queryService.getDeviceById(DEVICE_ID);

        assertNotNull(result);
        assertEquals(DEVICE_ID, result.getId());
        verify(deviceRepository).findById(DEVICE_ID);
        verify(deviceMapping).toDto(entity);
    }

    @Test
    void getDeviceById_nonExistentId_throwsNotFoundException() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("device not found");

        assertThrows(NotFoundException.class, () -> queryService.getDeviceById(999L));
    }

    // -------------------------------------------------------------------------
    // R14.5 — getAllDevices
    // -------------------------------------------------------------------------

    @Test
    void getAllDevices_noFilters_delegatesToRepoWithSpecAndPageable() {
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(0)
                .size(10)
                .build();

        DeviceEntity entity = Fixtures.buildDevice(2L);
        Page<DeviceEntity> entityPage = new PageImpl<>(List.of(entity));

        when(deviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);

        DeviceDto dto = new DeviceDto(2L, "Device-2", null, "SN-2",
                EDeviceType.LAPTOP, EDeviceStatus.ACTIVE, false);
        Page<DeviceDto> dtoPage = new PageImpl<>(List.of(dto));
        when(deviceMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationResponse<DeviceDto> result = queryService.getAllDevices(
                paginationRequest, null, null, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getItems().size());
        verify(deviceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAllDevices_withTypeAndStatusFilters_stillDelegatesToRepoWithSpec() {
        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(0)
                .size(5)
                .build();

        Page<DeviceEntity> emptyPage = new PageImpl<>(List.of());
        when(deviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<DeviceDto> emptyDtoPage = new PageImpl<>(List.of());
        when(deviceMapping.toDtoPageable(emptyPage)).thenReturn(emptyDtoPage);

        PaginationResponse<DeviceDto> result = queryService.getAllDevices(
                paginationRequest, null, null, EDeviceType.LAPTOP, EDeviceStatus.ACTIVE);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(deviceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // R15.2 — getDeviceUsers (service level)
    // -------------------------------------------------------------------------

    @Test
    void getDeviceUsers_existingDeviceWithAssignedUsers_returnsUserDtoList() {
        DeviceEntity entity = Fixtures.buildDevice(3L);
        entity.setId(DEVICE_ID);

        UserEntity user1 = Fixtures.buildUser(10L);
        user1.setId(10L);
        UserEntity user2 = Fixtures.buildUser(11L);
        user2.setId(11L);
        Set<UserEntity> users = new HashSet<>();
        users.add(user1);
        users.add(user2);
        entity.setUsers(users);

        when(deviceRepository.findByIdWithUsers(DEVICE_ID)).thenReturn(Optional.of(entity));

        DeviceUserDto dto1 = DeviceUserDto.builder().id(10L).email("user-10@example.com").build();
        DeviceUserDto dto2 = DeviceUserDto.builder().id(11L).email("user-11@example.com").build();
        when(deviceUserMapping.toDtoList(any())).thenReturn(List.of(dto1, dto2));

        List<DeviceUserDto> result = queryService.getDeviceUsers(DEVICE_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(deviceRepository).findByIdWithUsers(DEVICE_ID);
    }

    @Test
    void getDeviceUsers_existingDeviceWithNoUsers_returnsEmptyList() {
        DeviceEntity entity = Fixtures.buildDevice(4L);
        entity.setId(DEVICE_ID);
        entity.setUsers(new HashSet<>());

        when(deviceRepository.findByIdWithUsers(DEVICE_ID)).thenReturn(Optional.of(entity));
        when(deviceUserMapping.toDtoList(any())).thenReturn(List.of());

        List<DeviceUserDto> result = queryService.getDeviceUsers(DEVICE_ID);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void getDeviceUsers_nonExistentDevice_throwsNotFoundException() {
        when(deviceRepository.findByIdWithUsers(999L)).thenReturn(Optional.empty());
        when(messageService.getMessage(any(), any(Object[].class))).thenReturn("device not found");

        assertThrows(NotFoundException.class, () -> queryService.getDeviceUsers(999L));
    }
}
