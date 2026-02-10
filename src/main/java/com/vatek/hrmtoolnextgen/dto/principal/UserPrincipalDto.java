package com.vatek.hrmtoolnextgen.dto.principal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.List;

@Getter
@Setter
@Builder(builderMethodName = "internalBuilder")
public class UserPrincipalDto implements UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;

    @Getter
    private Long id;

    private String firstName;

    private String lastName;

    @Getter
    private String email;

    @JsonIgnore
    private String password;

    private boolean isEnabled;

    private String accessToken;

    private Long remainTime;

    private List<? extends GrantedAuthority> authorities;

    @Setter
    @Getter
    private List<String> roles;

    public static UserPrincipalDtoBuilder userPrincipalDtoBuilder(UserEntity userEntity) {

        return internalBuilder()
                .id(userEntity.getId())
                .firstName(userEntity.getUserInfo().getFirstName())
                .lastName(userEntity.getUserInfo().getLastName())
                .email(userEntity.getEmail())
                .password(userEntity.getPassword())
                .isEnabled(userEntity.isActive())
                .roles(userEntity.getRoles().stream().map(x -> x.getUserRole().name()).toList());
    }


    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }


    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
