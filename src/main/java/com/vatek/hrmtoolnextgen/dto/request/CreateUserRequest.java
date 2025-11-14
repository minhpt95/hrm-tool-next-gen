package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import java.util.Collection;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {

    @NotEmpty
    private String email;

    private UserInfoDto userInfo;

    private MultipartFile avatarImage;
    @NotEmpty
    private Collection<RoleDto> roles;
}
