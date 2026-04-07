package com.minhpt.hrmtoolnextgen.dto.user;

import java.time.LocalDate;

import com.minhpt.hrmtoolnextgen.annotation.SafeString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Personal profile information for an employee")
public class UserInfoDto {
    @SafeString
    @Schema(description = "First name")
    private String firstName;

    @SafeString
    @Schema(description = "Last name")
    private String lastName;

    @SafeString
    @Schema(description = "Government identity number")
    private String identityCard;

    @SafeString
    @Schema(description = "Primary phone number")
    private String phoneNumber1;

    @SafeString
    @Schema(description = "Current residential address")
    private String currentAddress;

    @SafeString
    @Schema(description = "Permanent address on file")
    private String permanentAddress;

    @SafeString
    @Schema(description = "Avatar image URL")
    private String avatarUrl;

    @Schema(description = "Onboard date of the employee")
    private LocalDate onboardDate;

    @Schema(description = "Birth date of the employee")
    private LocalDate birthDate;
}
