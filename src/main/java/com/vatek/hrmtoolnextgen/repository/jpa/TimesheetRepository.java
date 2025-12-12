package com.vatek.hrmtoolnextgen.repository.jpa;


import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.projection.TimesheetWorkingHourProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetRepository extends JpaRepository<TimesheetEntity, Long>, JpaSpecificationExecutor<TimesheetEntity>  {
    List<TimesheetWorkingHourProjection> findByUserEntityIdAndWorkingDayAndStatusNot(Long userId, LocalDate workingDay, ETimesheetStatus status);

    @EntityGraph(attributePaths = {
            "projectEntity",
            "projectEntity.projectManager",
            "userEntity"
    })
    Page<TimesheetEntity> findAll(Specification<TimesheetEntity> spec, Pageable pageable);
}
