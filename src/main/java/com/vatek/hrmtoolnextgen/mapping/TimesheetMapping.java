package com.vatek.hrmtoolnextgen.mapping;

import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BasePagingMapper;
import com.vatek.hrmtoolnextgen.mapping.excel.ProjectMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",uses = {UserMapping.class, ProjectMapping.class})
public interface TimesheetMapping extends BasePagingMapper<TimesheetDto, TimesheetEntity> {
}
