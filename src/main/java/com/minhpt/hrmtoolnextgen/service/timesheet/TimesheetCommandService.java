package com.minhpt.hrmtoolnextgen.service.timesheet;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.minhpt.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.service.WorkHoursCalculatorService;
import com.minhpt.hrmtoolnextgen.util.TimeUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class TimesheetCommandService {

    private final TimesheetRepository timesheetRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DayOffRepository dayOffRepository;
    private final TimesheetMapping timesheetMapping;
    private final WorkHoursCalculatorService workHoursCalculatorService;
    private final MessageService messageService;

    @Transactional
    public TimesheetDto createTimesheet(CreateTimesheetRequest form) {
        UserPrincipalDto currentUser = getCurrentUser();
        log.info("Creating timesheet for user: {} on project: {} for date: {}", currentUser.getId(), form.getProjectId(), form.getWorkingDay());

        UserEntity userEntity = requireUser(currentUser.getId());
        ProjectEntity projectEntity = requireProject(form.getProjectId());
        assertUserInProject(projectEntity.getId(), currentUser.getId());

        if (form.getTimesheetType() == ETimesheetType.NORMAL) {
            assertNotWeekend(form.getWorkingDay());
            assertCanLogNormalTimesheet(currentUser.getId(), form.getWorkingDay(), form.getWorkingHours());
        }

        TimesheetEntity savedEntity = timesheetRepository.save(buildTimesheetEntity(form, projectEntity, userEntity));
        log.info("Timesheet created successfully for user: {} on date: {}", currentUser.getId(), form.getWorkingDay());
        return timesheetMapping.toDto(savedEntity);
    }

    @Transactional
    public TimesheetDto updateTimesheet(UpdateTimesheetRequest form) {
        Long timesheetId = requireId(form.getId());
        log.info("Updating timesheet with id: {}", timesheetId);

        TimesheetEntity timesheetEntity = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("timesheet.not.found", timesheetId)));

        if (timesheetEntity.getStatus() != ETimesheetStatus.PENDING) {
            throw new BadRequestException(messageService.getMessage("timesheet.cannot.update"));
        }

        if (form.getTitle() != null) {
            timesheetEntity.setTitle(form.getTitle());
        }
        if (form.getDescription() != null) {
            timesheetEntity.setDescription(form.getDescription());
        }
        if (form.getTimesheetType() != null) {
            timesheetEntity.setType(form.getTimesheetType());
        }
        if (form.getProjectId() != null) {
            timesheetEntity.setProjectEntity(requireProject(form.getProjectId()));
        }
        if (form.getWorkingHours() != null) {
            timesheetEntity.setWorkingHours(form.getWorkingHours());
        }
        if (form.getWorkingDay() != null) {
            timesheetEntity.setWorkingDay(form.getWorkingDay());
        }

        TimesheetEntity updatedEntity = timesheetRepository.save(timesheetEntity);
        log.info("Timesheet updated successfully with id: {}", updatedEntity.getId());
        return timesheetMapping.toDto(updatedEntity);
    }

    @Transactional
    public TimesheetDto approvalTimesheet(ApprovalTimesheetRequest form) {
        Long timesheetId = requireId(form.getId());
        log.info("Processing timesheet approval for id: {} with status: {}", timesheetId, form.getTimesheetStatus());

        TimesheetEntity timesheetEntity = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("timesheet.not.found", timesheetId)));

        switch (timesheetEntity.getStatus()) {
            case PENDING -> {
                if (form.getTimesheetStatus() == null) {
                    throw new BadRequestException(messageService.getMessage("timesheet.status.not.found"));
                }
                timesheetEntity.setStatus(form.getTimesheetStatus());
            }
            case APPROVED, REJECTED -> throw new BadRequestException(messageService.getMessage("timesheet.cannot.change.status"));
        }

        TimesheetEntity savedEntity = timesheetRepository.save(timesheetEntity);
        log.info("Timesheet approval processed for id: {} with status: {}", timesheetId, savedEntity.getStatus());
        return timesheetMapping.toDto(savedEntity);
    }

    private TimesheetEntity buildTimesheetEntity(CreateTimesheetRequest form, ProjectEntity projectEntity, UserEntity userEntity) {
        TimesheetEntity timesheetEntity = new TimesheetEntity();
        timesheetEntity.setTitle(form.getTitle());
        timesheetEntity.setDescription(form.getDescription());
        timesheetEntity.setWorkingHours(form.getWorkingHours());
        timesheetEntity.setWorkingDay(form.getWorkingDay());
        timesheetEntity.setProjectEntity(projectEntity);
        timesheetEntity.setStatus(ETimesheetStatus.PENDING);
        timesheetEntity.setUserEntity(userEntity);
        timesheetEntity.setType(form.getTimesheetType());
        return timesheetEntity;
    }

    private UserPrincipalDto getCurrentUser() {
        return (UserPrincipalDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("user.not.found", userId)));
    }

    private ProjectEntity requireProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("project.not.found", projectId)));
    }

    private void assertUserInProject(Long projectId, Long userId) {
        if (!userRepository.existsByWorkingProjectIdAndId(projectId, userId)) {
            throw new BadRequestException(messageService.getMessage("error.user.not.in.project"));
        }
    }

    private void assertNotWeekend(LocalDate workingDay) {
        DayOfWeek dayOfWeek = workingDay.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new BadRequestException(messageService.getMessage("timesheet.cannot.log.weekend"));
        }
    }

    private void assertCanLogNormalTimesheet(Long userId, LocalDate workingDay, LocalTime newWorkingHours) {
        double totalWorkingHours = timesheetRepository
                .findByUserEntityIdAndWorkingDayAndStatusNot(userId, workingDay, ETimesheetStatus.REJECTED)
                .stream()
                .mapToDouble(projection -> TimeUtils.convertTimeToHours(projection.getWorkingHours()))
                .sum();

        double maxAllowedHours = calculateMaxAllowedHoursForDay(userId, workingDay);
        double newHours = TimeUtils.convertTimeToHours(newWorkingHours);
        if (totalWorkingHours + newHours > maxAllowedHours) {
            throw new BadRequestException(messageService.getMessage("timesheet.cannot.log", maxAllowedHours));
        }
    }

    private double calculateMaxAllowedHoursForDay(Long userId, LocalDate workingDay) {
        double dailyWorkHours = workHoursCalculatorService.getDailyWorkHours();
        LocalDateTime dayStart = workingDay.atStartOfDay();
        LocalDateTime dayEnd = workingDay.plusDays(1).atStartOfDay();
        Specification<DayOffEntity> spec = buildApprovedDayOffOverlapSpec(userId, dayStart, dayEnd);

        double usedByDayOffHours = 0.0;
        for (DayOffEntity dayOff : dayOffRepository.findAll(spec)) {
            if (dayOff.getStartTime() == null || dayOff.getEndTime() == null) {
                continue;
            }

            Map<LocalDate, Double> remainingByDate =
                    workHoursCalculatorService.calculateRemainingHours(dayOff.getStartTime(), dayOff.getEndTime());
            Double remaining = remainingByDate.get(workingDay);
            if (remaining == null) {
                continue;
            }

            double used = dailyWorkHours - remaining;
            if (used > 0) {
                usedByDayOffHours += used;
                if (usedByDayOffHours >= dailyWorkHours) {
                    return dailyWorkHours;
                }
            }
        }

        return Math.max(0.0, dailyWorkHours - usedByDayOffHours);
    }

    private Specification<DayOffEntity> buildApprovedDayOffOverlapSpec(Long userId, LocalDateTime dayStart, LocalDateTime dayEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.lessThan(root.get("startTime"), dayEnd));
            predicates.add(cb.greaterThan(root.get("endTime"), dayStart));
            predicates.add(cb.equal(root.get("requestedBy").get("id"), userId));
            predicates.add(cb.equal(root.get("status"), EDayOffStatus.APPROVED));
            predicates.add(cb.equal(root.get("delete"), false));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Long requireId(Long id) {
        if (id == null) {
            throw new BadRequestException(messageService.getMessage("not.null"));
        }
        return id;
    }
}
