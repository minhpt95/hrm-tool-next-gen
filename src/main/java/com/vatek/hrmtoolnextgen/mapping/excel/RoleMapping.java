package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.RoleEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RoleMapping extends BasePagingMapper<RoleDto, RoleEntity> {
}
