package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.user.UserExcelDto;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring",uses = {TimesheetExcelMapping.class})
public interface UserExcelMapping {
    @Mappings({
            @Mapping(target = "name",source = "userInfo.firstName"),
            @Mapping(target = "normalHours",source = "normalHours"),
            @Mapping(target = "overtimeHours",source = "overtimeHours")
    })
    UserExcelDto toDto(UserEntity entity);
}
