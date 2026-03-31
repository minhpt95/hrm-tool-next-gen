package com.minhpt.hrmtoolnextgen.mapping.excel;

import com.minhpt.hrmtoolnextgen.dto.project.ProjectExcelDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.mapping.common.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;


@Mapper(componentModel = "spring", uses = {UserExcelMapping.class})
public interface ProjectExcelMapping {
    @Mappings({
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "members", source = "members")

    })

    @Named(value = "toExcelDto")
    ProjectExcelDto toExcelDto(ProjectEntity entity);
}
