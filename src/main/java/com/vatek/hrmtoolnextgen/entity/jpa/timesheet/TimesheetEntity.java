package com.vatek.hrmtoolnextgen.entity.jpa.timesheet;


import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "timesheets")
@Getter
@Setter
public class TimesheetEntity extends IdentityEntity {
    @Column
    private String title;

    @Column
    private String description;

    @Column(name = "working_hours", columnDefinition = "TIME")
    private LocalTime workingHours;

    @Column
    @Enumerated(EnumType.STRING)
    private ETimesheetType type;

    @Column(columnDefinition = "DATE")
    private LocalDate workingDay;

    @Column
    @Enumerated(EnumType.STRING)
    private ETimesheetStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ProjectEntity projectEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;
}
