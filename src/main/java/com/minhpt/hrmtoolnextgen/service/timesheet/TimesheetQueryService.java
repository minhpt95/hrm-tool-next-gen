package com.minhpt.hrmtoolnextgen.service.timesheet;

import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class TimesheetQueryService {

    private static final String DEFAULT_SORT_BY = "createdDate";
    private static final String DEFAULT_DIRECTION = "DESC";

    private final TimesheetRepository timesheetRepository;
    private final TimesheetMapping timesheetMapping;

    @Transactional(readOnly = true)
    public PaginationResponse<TimesheetDto> getTimesheetsByManagerWithFilters(
            Long managerId,
            PaginationRequest paginationRequest,
            ETimesheetStatus status,
            Long projectId) {
        log.debug("Getting timesheets for manager id: {} - status: {}, projectId: {}", managerId, status, projectId);

        Specification<TimesheetEntity> spec = buildManagerTimesheetSpecification(managerId, status, projectId);
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, DEFAULT_SORT_BY, DEFAULT_DIRECTION);

        Page<TimesheetEntity> entityPage = timesheetRepository.findAll(spec, pageable);
        Page<TimesheetDto> dtoPage = timesheetMapping.toDtoPageable(entityPage);

        String actualSortBy = hasText(paginationRequest.getSortBy()) ? paginationRequest.getSortBy() : DEFAULT_SORT_BY;
        String actualDirection = hasText(paginationRequest.getDirection()) ? paginationRequest.getDirection() : DEFAULT_DIRECTION;
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest,
                actualSortBy,
                actualDirection
        );

        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    private Specification<TimesheetEntity> buildManagerTimesheetSpecification(
            Long managerId,
            ETimesheetStatus status,
            Long projectId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var projectJoin = root.join("projectEntity");
            predicates.add(cb.equal(projectJoin.get("projectManager").get("id"), managerId));
            predicates.add(cb.equal(root.get("delete"), false));

            if (projectId != null) {
                predicates.add(cb.equal(projectJoin.get("id"), projectId));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
