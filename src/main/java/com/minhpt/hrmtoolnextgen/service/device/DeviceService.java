package com.minhpt.hrmtoolnextgen.service.device;

import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceQueryService deviceQueryService;
    private final DeviceCommandService deviceCommandService;

    @Transactional(readOnly = true)
    public PaginationResponse<DeviceDto> getAllDevices(
            PaginationRequest paginationRequest,
            String name,
            String serialNumber,
            EDeviceType type,
            EDeviceStatus status) {
        return deviceQueryService.getAllDevices(paginationRequest, name, serialNumber, type, status);
    }

    @Transactional(readOnly = true)
    public DeviceDto getDeviceById(Long id) {
        return deviceQueryService.getDeviceById(id);
    }

    @Transactional
    public DeviceDto createDevice(CreateDeviceDto request) {
        return deviceCommandService.createDevice(request);
    }

    @Transactional
    public DeviceDto updateDevice(Long id, UpdateDeviceDto request) {
        return deviceCommandService.updateDevice(id, request);
    }

    @Transactional
    public void deleteDevice(Long id) {
        deviceCommandService.deleteDevice(id);
    }
}
