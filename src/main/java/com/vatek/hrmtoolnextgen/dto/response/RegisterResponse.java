package com.vatek.hrmtoolnextgen.dto.response;


import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    private Long id;
    private String email;
    private String password;
}
