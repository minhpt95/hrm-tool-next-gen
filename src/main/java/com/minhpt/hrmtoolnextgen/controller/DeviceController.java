package com.minhpt.hrmtoolnextgen.controller;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.constant.ApiConstant;
import com.minhpt.hrmtoolnextgen.constant.RoleConstant;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.ManageDeviceUsersDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.response.CommonSuccessResponse;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.service.device.DeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping({ApiConstant.DEVICE_BASE, ApiConstant.DEVICE_V1_BASE})
@Tag(name = "Device", description = "Device management APIs")
public class DeviceController {

    private final DeviceService deviceService;
    private final MessageService messageService;

    @PreAuthorize(RoleConstant.HAS_ADMIN_AUTHORITY)
    @PostMapping
    @Operation(
            summary = "Create device",
            description = "Registers a new device asset. Serial number must be unique across active devices."
    )
    public ResponseEntity<CommonSuccessResponse<DeviceDto>> createDevice(
            @Valid @RequestBody CreateDeviceDto createDeviceDto,
            HttpServletRequest request) {
        DeviceDto device = deviceService.createDevice(createDeviceDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildSuccessResponse(device, request));
    }

    @PreAuthorize(RoleConstant.HAS_ADMIN_AUTHORITY)
    @PutMapping("/{id}")
    @Operation(
            summary = "Update device",
            description = "Updates device metadata, type, status, and serial number."
    )
    public ResponseEntity<CommonSuccessResponse<DeviceDto>> updateDevice(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDeviceDto updateDeviceDto,
            HttpServletRequest request) {
        DeviceDto device = deviceService.updateDevice(id, updateDeviceDto);
        return ResponseEntity.ok(buildSuccessResponse(device, request));
    }

    @PreAuthorize(RoleConstant.HAS_ADMIN_AUTHORITY)
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete device",
            description = "Soft-deletes a device by setting its `isDelete` flag."
    )
    public ResponseEntity<CommonSuccessResponse<Void>> deleteDevice(
            @PathVariable Long id,
            HttpServletRequest request) {
        deviceService.deleteDevice(id);
        return ResponseEntity.ok(buildSuccessResponse(null, request));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get device detail",
            description = "Fetches a single device by ID."
    )
    public ResponseEntity<CommonSuccessResponse<DeviceDto>> getDeviceById(
            @PathVariable Long id,
            HttpServletRequest request) {
        DeviceDto device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(buildSuccessResponse(device, request));
    }

    @GetMapping
    @Operation(
            summary = "List devices",
            description = "Returns a paginated list of non-deleted devices. Supports filters by name, serial number, type, and status. Default sort by createdDate descending."
    )
    public ResponseEntity<CommonSuccessResponse<PaginationResponse<DeviceDto>>> getAllDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String serialNumber,
            @RequestParam(required = false) EDeviceType type,
            @RequestParam(required = false) EDeviceStatus status,
            HttpServletRequest request) {

        PaginationRequest paginationRequest = PaginationRequest.builder()
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .direction(direction)
                .build();

        PaginationResponse<DeviceDto> devices = deviceService.getAllDevices(
                paginationRequest, name, serialNumber, type, status);
        return ResponseEntity.ok(buildSuccessResponse(devices, request));
    }

    @PreAuthorize(RoleConstant.HAS_ADMIN_AUTHORITY)
    @PostMapping("/{id}/users")
    @Operation(
            summary = "Sync users assigned to a device",
            description = "Replace semantics: the supplied `userIds` becomes the device's full assigned-user set. The server diffs against the current state — users in the list but not yet assigned are added, users currently assigned but not in the list are removed, users in both are kept. Sending an empty list detaches all users."
    )
    public ResponseEntity<CommonSuccessResponse<List<DeviceUserDto>>> manageDeviceUsers(
            @PathVariable Long id,
            @Valid @RequestBody ManageDeviceUsersDto manageDeviceUsersDto,
            HttpServletRequest request) {
        List<DeviceUserDto> users = deviceService.manageDeviceUsers(id, manageDeviceUsersDto.getUserIds());
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    @PreAuthorize(RoleConstant.HAS_ADMIN_AUTHORITY)
    @GetMapping("/{id}/users")
    @Operation(
            summary = "List users assigned to a device",
            description = "Returns the lightweight list of users currently assigned to the device."
    )
    public ResponseEntity<CommonSuccessResponse<List<DeviceUserDto>>> getDeviceUsers(
            @PathVariable Long id,
            HttpServletRequest request) {
        List<DeviceUserDto> users = deviceService.getDeviceUsers(id);
        return ResponseEntity.ok(buildSuccessResponse(users, request));
    }

    private <T> CommonSuccessResponse<T> buildSuccessResponse(T data, HttpServletRequest request) {
        return CommonSuccessResponse.<T>commonSuccessResponseBuilder()
                .path(request.getServletPath())
                .httpStatusCode(HttpStatus.OK)
                .message(messageService.getMessage("success"))
                .data(data)
                .build();
    }
}
