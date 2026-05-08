package com.minhpt.hrmtoolnextgen.dto.device;

import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Device data exposed via the API")
public class DeviceDto {
    @Schema(description = "Unique identifier of the device")
    private Long id;

    @Schema(description = "Device display name")
    private String name;

    @Schema(description = "Device description")
    private String description;

    @Schema(description = "Unique device serial number")
    private String serialNumber;

    @Schema(description = "Device type (LAPTOP, MOUSE, ...)")
    private EDeviceType type;

    @Schema(description = "Lifecycle status of the device")
    private EDeviceStatus status;

    @Schema(description = "Flag indicating whether the device is soft deleted")
    private Boolean isDelete;
}
