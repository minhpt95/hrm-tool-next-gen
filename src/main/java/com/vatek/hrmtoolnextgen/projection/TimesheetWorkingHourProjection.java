package com.vatek.hrmtoolnextgen.projection;

import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;

import java.time.LocalDate;
import java.time.LocalTime;

public interface TimesheetWorkingHourProjection {
    LocalTime getWorkingHours();
    ETimesheetType getType();
    LocalDate getWorkingDay();
    ETimesheetStatus getStatus();
    UserEntity getUserEntity();
}
