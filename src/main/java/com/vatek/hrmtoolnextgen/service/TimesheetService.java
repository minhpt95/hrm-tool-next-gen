package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.constant.ErrorConstant;
import com.vatek.hrmtoolnextgen.dto.principal.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.exception.CommonException;
import com.vatek.hrmtoolnextgen.mapping.TimesheetMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import com.vatek.hrmtoolnextgen.util.TimeUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class TimesheetService {
    private final TimesheetRepository timesheetRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final DayOffRepository dayOffRepository;
    private final TimesheetMapping timesheetMapping;
    private final WorkHoursCalculatorService workHoursCalculatorService;

    @Transactional
    public TimesheetDto createTimesheet(CreateTimesheetRequest form) {
        UserPrincipalDto currentUser = getCurrentUser();
        UserEntity userEntity = requireUser(currentUser.getId());
        ProjectEntity projectEntity = requireProject(form.getProjectId());
        assertUserInProject(projectEntity.getId(), currentUser.getId());

        if (form.getTimesheetType() == ETimesheetType.NORMAL) {
            assertNotWeekend(form.getWorkingDay());
            assertCanLogNormalTimesheet(currentUser.getId(), form.getWorkingDay(), form.getWorkingHours());
        }

        TimesheetEntity timesheetEntity = buildTimesheetEntity(form, projectEntity, userEntity);
        return timesheetMapping.toDto(timesheetRepository.save(timesheetEntity));
    }

    private static TimesheetEntity buildTimesheetEntity(CreateTimesheetRequest form, ProjectEntity projectEntity, UserEntity userEntity) {
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
        UserEntity userEntity = userRepository.findById(userId).orElse(null);
        if (userEntity == null) {
            throw new CommonException(
                    String.format(ErrorConstant.Message.NOT_FOUND, "User id : " + userId),
                    HttpStatus.BAD_REQUEST
            );
        }
        return userEntity;
    }

    private ProjectEntity requireProject(Long projectId) {
        ProjectEntity projectEntity = projectRepository.findById(projectId).orElse(null);
        if (projectEntity == null) {
            throw new CommonException(
                    String.format(ErrorConstant.Message.NOT_FOUND, "Project id : " + projectId),
                    HttpStatus.BAD_REQUEST
            );
        }
        return projectEntity;
    }

    private void assertUserInProject(Long projectId, Long userId) {
        if (!userRepository.existsByWorkingProjectIdAndId(projectId, userId)) {
            throw new BadRequestException("You are not in project");
        }
    }

    private void assertNotWeekend(LocalDate workingDay) {
        DayOfWeek dayOfWeek = workingDay.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Cannot log timesheet on weekend");
        }
    }

    private void assertCanLogNormalTimesheet(Long userId, LocalDate workingDay, java.time.LocalTime newWorkingHours) {
        double totalWorkingHours = timesheetRepository
                .findByUserEntityIdAndWorkingDayAndStatusNot(userId, workingDay, ETimesheetStatus.REJECTED)
                .stream()
                .mapToDouble(p -> TimeUtils.convertTimeToHours(p.getWorkingHours()))
                .sum();

        double maxAllowedHours = calculateMaxAllowedHoursForDay(userId, workingDay);
        double newHours = TimeUtils.convertTimeToHours(newWorkingHours);
        if (totalWorkingHours + newHours > maxAllowedHours) {
            throw new BadRequestException(String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, maxAllowedHours));
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
                    usedByDayOffHours = dailyWorkHours;
                    break;
                }
            }
        }

        double maxAllowed = dailyWorkHours - usedByDayOffHours;
        return Math.max(0.0, maxAllowed);
    }

    private Specification<DayOffEntity> buildApprovedDayOffOverlapSpec(Long userId, LocalDateTime dayStart, LocalDateTime dayEnd) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            // Overlap check: existing.start < dayEnd AND existing.end > dayStart
            predicates.add(cb.lessThan(root.get("startTime"), dayEnd));
            predicates.add(cb.greaterThan(root.get("endTime"), dayStart));
            predicates.add(cb.equal(root.get("user").get("id"), userId));
            predicates.add(cb.equal(root.get("status"), EDayOffStatus.APPROVED));
            predicates.add(cb.equal(root.get("delete"), false));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public TimesheetDto updateTimesheet(UpdateTimesheetRequest form) {
        TimesheetEntity timesheetEntity = timesheetRepository.findById(form.getId()).orElseThrow(
                () -> new BadRequestException(
                        String.format(ErrorConstant.Message.NOT_FOUND, "Timesheet with id : " + form.getId())
                )
        );

        if (timesheetEntity.getStatus() != ETimesheetStatus.PENDING) {
            throw new BadRequestException(
                    ErrorConstant.Message.CANNOT_UPDATE_TIMESHEET
            );
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
            ProjectEntity projectEntity = projectRepository.findById(form.getProjectId()).orElseThrow(
                    () -> new BadRequestException(
                        String.format(ErrorConstant.Message.NOT_FOUND, "Project with id : " + form.getProjectId())
                    )
            );
            timesheetEntity.setProjectEntity(projectEntity);
        }

        if (form.getWorkingHours() != null) {
            timesheetEntity.setWorkingHours(form.getWorkingHours());
        }

        if (form.getWorkingDay() != null)
            timesheetEntity.setWorkingDay(form.getWorkingDay());

        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
    }

    public TimesheetDto approvalTimesheet(ApprovalTimesheetRequest form) {
        TimesheetEntity timesheetEntity = timesheetRepository.findById(form.getId()).orElseThrow(
                () -> new BadRequestException(
                        String.format(ErrorConstant.Message.NOT_FOUND, "Timesheet with id : " + form.getId())
                )
        );

        switch (timesheetEntity.getStatus()) {
            case PENDING -> {
                if (form.getTimesheetStatus() == null) {
                    throw new BadRequestException(
                            String.format(ErrorConstant.Message.NOT_FOUND, "Status")
                    );
                }
                timesheetEntity.setStatus(form.getTimesheetStatus());
            }
            case APPROVED, REJECTED ->
                    throw new BadRequestException(ErrorConstant.Message.CANNOT_CHANGE_TIMESHEET_STATUS);
        }

        return timesheetMapping.toDto(timesheetRepository.save(timesheetEntity));
    }

    @Transactional(readOnly = true)
    public PaginationResponse<TimesheetDto> getTimesheetsByManagerWithFilters(
            Long managerId,
            PaginationRequest paginationRequest,
            ETimesheetStatus status,
            Long projectId) {

        // Build specification for filtering
        Specification<TimesheetEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by manager ID (through project's projectManager)
            var projectJoin = root.join("projectEntity");
            predicates.add(cb.equal(projectJoin.get("projectManager").get("id"), managerId));

            // Filter by delete = false
            predicates.add(cb.equal(root.get("delete"), false));

            // Filter by project if provided
            if (projectId != null) {
                predicates.add(cb.equal(projectJoin.get("id"), projectId));
            }

            // Filter by status if provided
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Build pageable with default sort by createdDate desc
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, "createdDate", "DESC");

        Page<TimesheetEntity> entityPage = timesheetRepository.findAll(spec, pageable);
        Page<TimesheetDto> dtoPage = timesheetMapping.toDtoPageable(entityPage);

        // Build pagination request for response
        String actualSortBy = paginationRequest.getSortBy() != null && !paginationRequest.getSortBy().isBlank()
                ? paginationRequest.getSortBy() : "createdDate";
        String actualDirection = paginationRequest.getDirection() != null && !paginationRequest.getDirection().isBlank()
                ? paginationRequest.getDirection() : "DESC";
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest, actualSortBy, actualDirection);

        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }
}
