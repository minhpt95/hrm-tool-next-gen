package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.mapping.UserMapping;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring",uses = {UserMapping.class})
public interface ProjectMapping{
}
