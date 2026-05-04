package com.minhpt.hrmtoolnextgen.dto.request;

import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeviceDto {
    @Schema(description = "Device name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Device description")
    private String description;

    @NotEmpty
    @Schema(description = "Device serial number", requiredMode = Schema.RequiredMode.REQUIRED)
    private String serialNumber;

    @Schema(description = "Device type", requiredMode = Schema.RequiredMode.REQUIRED)
    private EDeviceType type;

    @Schema(description = "Device status", requiredMode = Schema.RequiredMode.REQUIRED)
    private EDeviceStatus status;
}
