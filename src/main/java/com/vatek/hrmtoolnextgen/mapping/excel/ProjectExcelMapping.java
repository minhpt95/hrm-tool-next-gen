package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.project.ProjectExcelDto;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;


@Mapper(componentModel = "spring",uses = {UserExcelMapping.class})
public interface ProjectExcelMapping {
    @Mappings({
            @Mapping(target = "name",source = "name"),
            @Mapping(target = "members",source = "members")

    })

    @Named(value = "toExcelDto")
    ProjectExcelDto toExcelDto(ProjectEntity entity);
}
