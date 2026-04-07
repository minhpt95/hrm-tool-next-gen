package com.minhpt.hrmtoolnextgen.entity.jpa.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_active_delete", columnList = "is_active, is_delete")
})
@SQLDelete(sql = "UPDATE users SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
@Getter
@Setter
public class UserEntity extends IdentityEntity {

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password")
    private String password;

    @OneToOne(targetEntity = UserInfoEntity.class, cascade = CascadeType.ALL)
    @Fetch(value = FetchMode.JOIN)
    private UserInfoEntity userInfo;

    @OneToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH
    }, mappedBy = "userEntity")
    @OrderBy("workingDay asc")
    private List<TimesheetEntity> timesheets = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH
    }, mappedBy = "projectManager")
    private List<ProjectEntity> projectManagements = new ArrayList<>();

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "role_id", referencedColumnName = "id"))
    private List<RoleEntity> roles = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = {
            CascadeType.DETACH,
            CascadeType.MERGE,
            CascadeType.PERSIST,
            CascadeType.REFRESH,
    })
    @JoinTable(
            name = "users_projects_working",
            joinColumns = @JoinColumn(
                    name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(
                    name = "project_id", referencedColumnName = "id"))
    private Set<ProjectEntity> workingProject = new HashSet<>();

    @Transient
    private List<TimesheetEntity> normalHours = new ArrayList<>();

    @Transient
    private List<TimesheetEntity> overtimeHours = new ArrayList<>();

    @Transient
    private List<TimesheetEntity> bonusHours = new ArrayList<>();

    @PostLoad
        @SuppressWarnings("unused")
    private void loadToTransientData() {
        this.normalHours = timesheets.stream().filter(x -> x.getType() == ETimesheetType.NORMAL).toList();
        this.overtimeHours = timesheets.stream().filter(x -> x.getType() == ETimesheetType.OVERTIME).toList();
        this.bonusHours = timesheets.stream().filter(x -> x.getType() == ETimesheetType.BONUS).toList();
    }
}
