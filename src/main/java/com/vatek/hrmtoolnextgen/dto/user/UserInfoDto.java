package com.vatek.hrmtoolnextgen.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Personal profile information for an employee")
public class UserInfoDto {
    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Government identity number")
    private String identityCard;

    @Schema(description = "Primary phone number")
    private String phoneNumber1;

    @Schema(description = "Current residential address")
    private String currentAddress;

    @Schema(description = "Permanent address on file")
    private String permanentAddress;

    @Schema(description = "Avatar image URL")
    private String avatarUrl;
}
