package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetExcelDto;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.mapping.common.BaseMapper;
import com.vatek.hrmtoolnextgen.util.DateUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.time.Instant;
import java.time.ZonedDateTime;

@Mapper(componentModel = "spring")
public interface TimesheetExcelMapping {
    @Mappings({
            @Mapping(target = "date",expression = "java(convertInstantToDateString(entity.getWorkingDay()))"),
            @Mapping(target = "taskDescription",source = "description")
    })
    @Named(value = "toExcelDto")
    TimesheetExcelDto toExcelDto(TimesheetEntity entity);

    default String convertInstantToDateString(ZonedDateTime zonedDateTime){
        return DateUtils.convertInstantToStringDate(zonedDateTime);
    }
}

