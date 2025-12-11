package com.vatek.hrmtoolnextgen.mapping;

import com.vatek.hrmtoolnextgen.dto.user.UserInfoDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserInfoMapping extends BasePagingMapper<UserInfoEntity, UserInfoDto> {
}
