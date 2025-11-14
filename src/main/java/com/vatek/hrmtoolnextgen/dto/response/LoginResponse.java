package com.vatek.hrmtoolnextgen.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private Long id;
    private String accessToken;
    private String type = "Bearer";
    private String firstName;
    private String lastName;
    private String email;
    private String refreshToken;
    private List<String> roles;
}
