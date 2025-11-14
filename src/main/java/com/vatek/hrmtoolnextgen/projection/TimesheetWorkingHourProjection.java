package com.vatek.hrmtoolnextgen.projection;

import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;

import java.time.ZonedDateTime;

public interface TimesheetWorkingHourProjection {
    Integer getWorkingHours();
    ETimesheetType getType();
    ZonedDateTime getWorkingDay();
    ETimesheetStatus getStatus();
    UserEntity getUserEntity();
}
