package com.minhpt.hrmtoolnextgen.mapping;

import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", uses = {UserMapping.class, ProjectMapping.class}, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TimesheetMapping extends BasePagingMapper<TimesheetDto, TimesheetEntity> {

    @Override
    @Mappings({
	    @Mapping(target = "type", source = "timesheetType")
    })
    TimesheetEntity toEntity(TimesheetDto dto);

    @Override
    @Mappings({
	    @Mapping(target = "timesheetType", source = "type")
    })
    TimesheetDto toDto(TimesheetEntity entity);
}
