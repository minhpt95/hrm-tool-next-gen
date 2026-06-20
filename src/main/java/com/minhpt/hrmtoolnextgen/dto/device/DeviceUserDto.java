package com.minhpt.hrmtoolnextgen.dto.device;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Lightweight user representation returned with a device assignment")
public class DeviceUserDto {

    @Schema(description = "User identifier")
    private Long id;

    @Schema(description = "User email")
    private String email;

    @Schema(description = "User first name")
    private String firstName;

    @Schema(description = "User last name")
    private String lastName;

    @Schema(description = "Avatar image URL")
    private String avatarUrl;
}
