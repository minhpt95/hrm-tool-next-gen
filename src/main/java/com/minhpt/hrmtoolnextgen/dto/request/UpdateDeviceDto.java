package com.minhpt.hrmtoolnextgen.dto.request;

import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload for updating an existing device")
public class UpdateDeviceDto {
    @Schema(description = "Updated device name")
    private String name;

    @Schema(description = "Updated device description")
    private String description;

    @Schema(description = "Updated serial number; must remain unique across active devices")
    private String serialNumber;

    @Schema(description = "Updated device type")
    private EDeviceType type;

    @Schema(description = "Updated device status")
    private EDeviceStatus status;
}
