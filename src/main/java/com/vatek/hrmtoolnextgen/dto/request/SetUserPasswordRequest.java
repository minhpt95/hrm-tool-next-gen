package com.vatek.hrmtoolnextgen.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SetUserPasswordRequest {
    @NotNull
    @NotEmpty
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;
}






























