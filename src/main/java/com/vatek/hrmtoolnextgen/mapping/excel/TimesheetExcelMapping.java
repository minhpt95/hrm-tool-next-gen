package com.vatek.hrmtoolnextgen.mapping.excel;

import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetExcelDto;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.util.DateUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.time.LocalDate;

@Mapper(componentModel = "spring")
public interface TimesheetExcelMapping {
    @Mappings({
            @Mapping(target = "date", expression = "java(convertInstantToDateString(entity.getWorkingDay()))"),
            @Mapping(target = "taskDescription", source = "description"),
            @Mapping(target = "no", ignore = true)
    })
    @Named(value = "toExcelDto")
    TimesheetExcelDto toExcelDto(TimesheetEntity entity);

    default String convertInstantToDateString(LocalDate workingDay) {
        return DateUtils.convertLocalDateToStringDate(workingDay);
    }
}

