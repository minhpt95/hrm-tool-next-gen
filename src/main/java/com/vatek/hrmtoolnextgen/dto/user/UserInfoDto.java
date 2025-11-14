package com.vatek.hrmtoolnextgen.dto.user;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserInfoDto {
    private String firstName;
    private String lastName;
    private String identityCard;
    private String phoneNumber1;
    private String currentAddress;
    private String permanentAddress;
    private String avatarUrl;
}
