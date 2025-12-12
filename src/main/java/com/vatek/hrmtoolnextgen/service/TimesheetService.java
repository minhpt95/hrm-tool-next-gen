package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.constant.DateConstant;
import com.vatek.hrmtoolnextgen.constant.ErrorConstant;
import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.dto.request.ApprovalTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.CreateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateTimesheetRequest;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.vatek.hrmtoolnextgen.enumeration.ETimesheetType;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.exception.CommonException;
import com.vatek.hrmtoolnextgen.mapping.TimesheetMapping;
import com.vatek.hrmtoolnextgen.projection.TimesheetWorkingHourProjection;
import com.vatek.hrmtoolnextgen.repository.jpa.DayOffRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.DateUtils;
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
import com.vatek.hrmtoolnextgen.util.CommonUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@AllArgsConstructor
@Log4j2
public class TimesheetService {
    private TimesheetRepository timesheetRepository;
    private UserRepository userRepository;
    private ProjectRepository projectRepository;
    private DayOffRepository dayOffRepository;
    private TimesheetMapping timesheetMapping;

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

        LocalDate workingDayDate = DateUtils.convertStringDateToLocalDate(form.getWorkingDay());

        var workingDayInstantDayOfWeek = workingDayDate.getDayOfWeek();

        if (
                form.getTimesheetType() == ETimesheetType.NORMAL && (workingDayInstantDayOfWeek == DayOfWeek.SATURDAY || workingDayInstantDayOfWeek == DayOfWeek.SUNDAY)
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
                            workingDayDate,
                            ETimesheetStatus.REJECTED
                    );

            var totalWorkingHours = getTimesheetProjection
                    .stream()
                    .mapToInt(TimesheetWorkingHourProjection::getWorkingHours)
                    .sum();

            // Check for approved day off requests on the same day
            Specification<DayOffEntity> dayOffEntitySpecification = (root, query, criteriaBuilder) -> {
                var predicates = new ArrayList<Predicate>();
                predicates.add(criteriaBuilder.equal(
                        root.get("dayoffEntityId").get("dateOff"),
                        workingDayDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                ));
                predicates.add(criteriaBuilder.equal(
                        root.get("dayoffEntityId").get("user").get("id"), 
                        currentUser.getId()
                ));
                // Only check APPROVED day off requests
                predicates.add(criteriaBuilder.equal(root.get("status"), EDayOffStatus.APPROVED));
                Predicate[] p = new Predicate[predicates.size()];
                return criteriaBuilder.and(predicates.toArray(p));
            };

            var approvedDayOffs = dayOffRepository.findAll(dayOffEntitySpecification);

            // Rule 3: Cannot log when request dayoff FULL is approved same day
            for (DayOffEntity dayOff : approvedDayOffs) {
                if (dayOff.getDayoffEntityId().getType() == EDayOffType.FULL) {
                    throw new CommonException(
                            ErrorConstant.Message.CANNOT_LOG_ON_FULL_DAY_OFF,
                            HttpStatus.BAD_REQUEST
                    );
                }
            }

            // Rule 4: Cannot log more than (8 - numberOfHours) when request dayoff is PARTIAL
            int maxAllowedHours = 8;
            for (DayOffEntity dayOff : approvedDayOffs) {
                if (dayOff.getDayoffEntityId().getType() == EDayOffType.PARTIAL) {
                    Integer dayOffHours = dayOff.getNumberOfHours();
                    if (dayOffHours == null || dayOffHours <= 0) {
                        throw new BadRequestException("Day off PARTIAL type must have numberOfHours > 0");
                    }
                    maxAllowedHours = 8 - dayOffHours;
                    break; // Only consider the first PARTIAL day off if multiple exist
                }
            }

            // Rule 1: Cannot log total more than maxAllowedHours (8 hours normally, or 8 - dayOffHours if PARTIAL day off exists)
            if (totalWorkingHours + form.getWorkingHours() > maxAllowedHours) {
                throw new CommonException(
                        String.format(ErrorConstant.Message.CANNOT_LOG_TIMESHEET, maxAllowedHours),
                        HttpStatus.BAD_REQUEST
                );
            }
        }


        TimesheetEntity timesheetEntity = new TimesheetEntity();
        timesheetEntity.setTitle(form.getTitle());
        timesheetEntity.setDescription(form.getDescription());
        timesheetEntity.setWorkingHours(form.getWorkingHours());
        timesheetEntity.setWorkingDay(workingDayDate);
        timesheetEntity.setProjectEntity(projectEntity);
        timesheetEntity.setStatus(ETimesheetStatus.PENDING);
        timesheetEntity.setUserEntity(userEntity);
        timesheetEntity.setType(form.getTimesheetType());
        timesheetEntity = timesheetRepository.save(timesheetEntity);

        return timesheetMapping.toDto(timesheetEntity);
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
            timesheetEntity.setWorkingDay(DateUtils.convertStringDateToLocalDate(form.getWorkingDay(), DateConstant.DD_MM_YYYY));

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
            case APPROVED, REJECTED -> {
                throw new BadRequestException(ErrorConstant.Message.CANNOT_CHANGE_TIMESHEET_STATUS);
            }
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
