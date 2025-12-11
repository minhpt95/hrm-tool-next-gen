package com.vatek.hrmtoolnextgen.mapping;

import com.vatek.hrmtoolnextgen.dto.request.RegisterRequest;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import com.vatek.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import java.util.Collection;
import java.util.stream.Collectors;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {UserInfoDto.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapping extends BasePagingMapper<UserDto, UserEntity> {
    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "roles", source = "roles"),
            @Mapping(target = "enabled", source = "active"),
    })
    @Named("toCustomDto")
    UserDto toCustomDto(UserEntity userEntity);

    @Override
    UserEntity toEntity(UserDto var1);

    @Override
    UserDto toDto(UserEntity var1);

    @Mappings({
            @Mapping(target = "userInfo", source = "userInfo"),
    })
    UserEntity createUser(RegisterRequest var1);

    @Named("roleToEnum")
    default EUserRole mapRoleToEnum(RoleEntity role) {
        return role != null ? role.getUserRole() : null;
    }

    @Named("enumToRole")
    default RoleEntity mapEnumToRole(EUserRole role) {
        if (role == null) {
            return null;
        }
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setUserRole(role);
        return roleEntity;
    }

    default Collection<EUserRole> mapRolesToEnum(Collection<RoleEntity> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(this::mapRoleToEnum)
                .collect(Collectors.toList());
    }

    default Collection<RoleEntity> mapEnumsToRoles(Collection<EUserRole> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(this::mapEnumToRole)
                .collect(Collectors.toList());
    }
}
