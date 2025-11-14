package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotEmpty
    private String email;
    @NotNull
    private UserInfoDto userInfo;
    @NotEmpty
    private Collection<RoleDto> roles;
}
