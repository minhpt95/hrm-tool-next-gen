package com.vatek.hrmtoolnextgen.mapping;

import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {UserMapping.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        implementationName = "ProjectCrudMappingImpl"
)
public interface ProjectMapping extends BasePagingMapper<ProjectDto, ProjectEntity> {
    
    @Override
    @Mapping(target = "delete", source = "isDelete")
    ProjectEntity toEntity(ProjectDto dto);

    @Mappings({
            @Mapping(target = "managerUser", source = "projectManager"),
            @Mapping(target = "members", source = "members"),
            @Mapping(target = "isDelete", source = "delete")
    })
    @Override
    ProjectDto toDto(ProjectEntity entity);

    @Mapping(target = "name", source = "projectName")
    @Mapping(target = "description", source = "projectDescription")
    @Mapping(target = "projectManager", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "timesheetEntities", ignore = true)
    ProjectEntity fromCreateRequest(CreateProjectRequest request);

    @Mapping(target = "name", source = "projectName")
    @Mapping(target = "description", source = "projectDescription")
    @Mapping(target = "projectManager", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "startTime", ignore = true)
    @Mapping(target = "endTime", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "timesheetEntities", ignore = true)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget ProjectEntity entity);
}

