package com.minhpt.hrmtoolnextgen.mapping;

import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapping.class, ProjectMapping.class})
public interface TimesheetMapping extends BasePagingMapper<TimesheetDto, TimesheetEntity> {
}
