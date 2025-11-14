package com.vatek.hrmtoolnextgen.mapping;

import com.vatek.hrmtoolnextgen.dto.request.CreateUserRequest;
import com.vatek.hrmtoolnextgen.dto.request.RegisterRequest;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.apache.catalina.User;
import org.mapstruct.*;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.stream.Collectors;

@Mapper(
        componentModel = "spring",
        uses = {RoleEntity.class, UserInfoDto.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapping extends BasePagingMapper<UserDto, UserEntity> {
    @Mappings({
            @Mapping(target = "id",source = "id"),
            @Mapping(target = "roles",source = "roles"),
    })
    @Named("toCustomDto")
    UserDto toCustomDto(UserEntity userEntity);

    @Override
    UserEntity toEntity(UserDto var1);

    @Override
    UserDto toDto(UserEntity var1);

    @Mappings({
            @Mapping(target = "userInfo",source = "userInfo"),
    })
    UserEntity createUser(RegisterRequest var1);


}
