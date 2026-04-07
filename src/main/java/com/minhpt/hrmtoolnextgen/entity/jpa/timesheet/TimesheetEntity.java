package com.minhpt.hrmtoolnextgen.entity.jpa.timesheet;


import java.time.LocalDate;
import java.time.LocalTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "timesheets", indexes = {
        @Index(name = "idx_timesheets_user_day_status", columnList = "user_id, working_day, status"),
        @Index(name = "idx_timesheets_project_status", columnList = "project_id, status"),
        @Index(name = "idx_timesheets_delete_created", columnList = "is_delete, create_date")
})
@SQLDelete(sql = "UPDATE timesheets SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
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
