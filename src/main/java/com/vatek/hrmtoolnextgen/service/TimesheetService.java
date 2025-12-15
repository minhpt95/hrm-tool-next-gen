package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.constant.ErrorConstant;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
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
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@Log4j2
public class TimesheetService {
    private TimesheetRepository timesheetRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private DayOffRepository dayOffRepository;
    private TimesheetMapping timesheetMapping;
    private WorkHoursCalculatorService workHoursCalculatorService;

    @Transactional
    public TimesheetDto createTimesheet(CreateTimesheetRequest form) {
        var currentUser = (UserPrincipalDto) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        UserEntity userEntity = userRepository.findById(currentUser.getId()).orElse(null);

        ProjectEntity projectEntity = projectRepository.findById(form.getProjectId()).orElse(null);

        if (projectEntity == null) {
            throw new CommonException(
                    String.format(ErrorConstant.Message.NOT_FOUND, "Project id : " + form.getProjectId()),
                    HttpStatus.BAD_REQUEST
            );
        }

        // Check if user is in the project using a query instead of loading all projects
        if (!userRepository.existsByWorkingProjectIdAndId(form.getProjectId(), currentUser.getId())) {
            throw new BadRequestException(
                    "You are not in project"
            );
        }


        var dayOfWeek = form.getWorkingDay().getDayOfWeek();

        if (
                form.getTimesheetType() == ETimesheetType.NORMAL && (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY)
        ) {
            throw new BadRequestException(
                    "Cannot log timesheet on weekend"
            );
        }

        // Only validate for NORMAL timesheet type
        if (form.getTimesheetType() == ETimesheetType.NORMAL) {
            // Rule 2: Cannot log on weekend with normal timesheet (already checked above)
            // This is already implemented in lines 85-91

            // Get existing timesheets for the day (excluding rejected)
            var getTimesheetProjection = timesheetRepository
                    .findByUserEntityIdAndWorkingDayAndStatusNot(
                            currentUser.getId(),
                            form.getWorkingDay(),
                            ETimesheetStatus.REJECTED
                    );

            // Convert LocalTime to hours for calculation
            double totalWorkingHours = getTimesheetProjection
                    .stream()
                    .mapToDouble(proj -> TimeUtils.convertTimeToHours(proj.getWorkingHours()))
                    .sum();

            // Check for approved day off requests on the same day
            LocalDate workingDay = form.getWorkingDay();
            LocalDateTime dayStart = workingDay.atStartOfDay();
            LocalDateTime dayEnd = workingDay.plusDays(1).atStartOfDay();

            Specification<DayOffEntity> dayOffEntitySpecification = (root, query, criteriaBuilder) -> {
                var predicates = new ArrayList<Predicate>();
                // Overlap check: existing.start < dayEnd AND existing.end > dayStart
                predicates.add(criteriaBuilder.lessThan(root.get("startTime"), dayEnd));
                predicates.add(criteriaBuilder.greaterThan(root.get("endTime"), dayStart));
                predicates.add(criteriaBuilder.equal(
                        root.get("user").get("id"),
                        currentUser.getId()
                ));
                // Only check APPROVED day off requests
                predicates.add(criteriaBuilder.equal(root.get("status"), EDayOffStatus.APPROVED));
                // Exclude deleted records
                predicates.add(criteriaBuilder.equal(root.get("delete"), false));
                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            };

            var approvedDayOffs = dayOffRepository.findAll(dayOffEntitySpecification);

            double dailyWorkHours = workHoursCalculatorService.getDailyWorkHours();
            double overlapHours = 0.0;
            for (DayOffEntity dayOff : approvedDayOffs) {
                if (dayOff.getStartTime() == null || dayOff.getEndTime() == null) {
                    continue;
                }
                Map<LocalDate, Double> remainingByDate = workHoursCalculatorService.calculateRemainingHours(
                        dayOff.getStartTime(), dayOff.getEndTime());
                Double remainingForDay = remainingByDate.get(workingDay);
                if (remainingForDay == null) {
                    continue;
                }
                double used = dailyWorkHours - remainingForDay;
                if (used > 0) {
                    overlapHours += used;
                }
                if (overlapHours >= dailyWorkHours) {
                    overlapHours = dailyWorkHours;
                    break;
                }
            }

            double maxAllowedHours = dailyWorkHours - overlapHours;
            if (maxAllowedHours < 0) {
                maxAllowedHours = 0;
            }

            // Rule: Cannot log total more than maxAllowedHours for that day
            double newWorkingHours = TimeUtils.convertTimeToHours(form.getWorkingHours());
            if (totalWorkingHours + newWorkingHours > maxAllowedHours) {
                throw new BadRequestException(
                        String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, maxAllowedHours)
                );
            }
        }


        TimesheetEntity timesheetEntity = getTimesheetEntity(form, projectEntity, userEntity);
        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
    }

    private static TimesheetEntity getTimesheetEntity(CreateTimesheetRequest form, ProjectEntity projectEntity, UserEntity userEntity) {
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
                            String.format(ErrorConstant.Message.NOT_FOUND, "Project with id : " + form.getId())
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
            case ETimesheetStatus.PENDING -> {
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

        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
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
