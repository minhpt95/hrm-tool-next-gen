package com.vatek.hrmtoolnextgen.entity.jpa.project;

import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "projects")
@Getter
@Setter
public class ProjectEntity extends IdentityEntity {
    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "project_status")
    @Enumerated(EnumType.STRING)
    private EProjectStatus projectStatus;

    @Column(name = "start_time", columnDefinition = "DATE")
    private LocalDate startTime;

    @Column(name = "end_time", columnDefinition = "DATE")
    private LocalDate endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_manager")
    private UserEntity projectManager;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "projectEntity")
    @OrderBy("workingDay asc")
    private Collection<TimesheetEntity> timesheetEntities = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "workingProject", cascade = {
            CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
    })
    private Set<UserEntity> members = new HashSet<>();

    public void addMemberToProject(UserEntity userEntity) {
        members.add(userEntity);
        userEntity.getWorkingProject().add(this);
    }

    public void removeMemberFromProject(UserEntity userEntity) {
        members.remove(userEntity);
        userEntity.getWorkingProject().remove(this);
    }
}
