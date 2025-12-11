package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.user.RoleDto;
import com.vatek.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapping extends BasePagingMapper<RoleDto, RoleEntity> {
}
