package com.vatek.hrmtoolnextgen.repository.jpa;


import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.projection.TimesheetWorkingHourProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetRepository extends JpaRepository<TimesheetEntity, Long>, JpaSpecificationExecutor<TimesheetEntity>  {
    List<TimesheetWorkingHourProjection> findByUserEntityIdAndWorkingDayAndStatusNot(Long userId, ZonedDateTime workingDay, ETimesheetStatus status);
}
