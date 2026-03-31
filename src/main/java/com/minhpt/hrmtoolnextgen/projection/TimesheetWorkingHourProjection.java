package com.minhpt.hrmtoolnextgen.projection;

import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;

import java.time.LocalDate;
import java.time.LocalTime;

public interface TimesheetWorkingHourProjection {
    LocalTime getWorkingHours();

    ETimesheetType getType();

    LocalDate getWorkingDay();

    ETimesheetStatus getStatus();

    UserEntity getUserEntity();
}
