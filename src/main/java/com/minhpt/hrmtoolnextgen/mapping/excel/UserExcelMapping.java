package com.minhpt.hrmtoolnextgen.mapping.excel;

import com.minhpt.hrmtoolnextgen.dto.user.UserExcelDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {TimesheetExcelMapping.class})
public interface UserExcelMapping {
    @Mappings({
            @Mapping(target = "name", source = "userInfo.firstName"),
            @Mapping(target = "normalHours", source = "normalHours"),
            @Mapping(target = "overtimeHours", source = "overtimeHours")
    })
    UserExcelDto toDto(UserEntity entity);
}
