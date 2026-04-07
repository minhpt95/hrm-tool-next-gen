package com.minhpt.hrmtoolnextgen.mapping.excel;

import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetExcelDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.util.DateUtils;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TimesheetExcelMapping {
    @Mappings({
            @Mapping(target = "date", expression = "java(convertInstantToDateString(entity.getWorkingDay()))"),
            @Mapping(target = "taskDescription", source = "description"),
            @Mapping(target = "no", ignore = true)
    })
    @Named(value = "toExcelDto")
    TimesheetExcelDto toExcelDto(TimesheetEntity entity);

    @IterableMapping(qualifiedByName = "toExcelDto")
    List<TimesheetExcelDto> toExcelDtos(List<TimesheetEntity> entities);

    default String convertInstantToDateString(LocalDate workingDay) {
        return DateUtils.convertLocalDateToStringDate(workingDay);
    }
}

