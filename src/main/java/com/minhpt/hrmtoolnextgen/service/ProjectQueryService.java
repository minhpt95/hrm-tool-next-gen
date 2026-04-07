package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.ProjectMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProjectQueryService {

    private static final String DEFAULT_SORT_BY = "createdDate";
    private static final String DEFAULT_DIRECTION = "DESC";

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapping projectMapping;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjects(PaginationRequest paginationRequest) {
        log.debug("Getting all projects - page: {}, size: {}", paginationRequest.getPage(), paginationRequest.getSize());
        Page<ProjectEntity> entityPage = projectRepository.findAll(CommonUtils.buildPageable(paginationRequest));
        Page<ProjectDto> dtoPage = projectMapping.toDtoPageable(entityPage);
        return CommonUtils.buildPaginationResponse(dtoPage, paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjectsForAdmin(
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        log.debug("Getting all projects for admin - name: {}, status: {}", projectName, projectStatus);
        return executePaginatedProjectQuery(buildProjectFilterSpecification(projectName, projectStatus), paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getProjectsByMemberIdWithFilters(
            Long memberId,
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        log.debug("Getting projects for member id: {} - name: {}, status: {}", memberId, projectName, projectStatus);
        ensureUserExists(memberId);

        Specification<ProjectEntity> spec = buildProjectFilterSpecification(projectName, projectStatus)
                .and((root, query, cb) -> {
                    var membersJoin = root.join("members");
                    if (query != null) {
                        query.distinct(true);
                    }
                    return cb.equal(membersJoin.get("id"), memberId);
                });

        return executePaginatedProjectQuery(spec, paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getProjectsByManagerIdWithFilters(
            Long managerId,
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        log.debug("Getting projects for manager id: {} - name: {}, status: {}", managerId, projectName, projectStatus);
        ensureUserExists(managerId);

        Specification<ProjectEntity> spec = buildProjectFilterSpecification(projectName, projectStatus)
                .and((root, query, cb) -> cb.equal(root.get("projectManager").get("id"), managerId));

        return executePaginatedProjectQuery(spec, paginationRequest);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        log.debug("Getting project by id: {}", id);
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("project.not.found", id)));
        return projectMapping.toDto(projectEntity);
    }

    private Specification<ProjectEntity> buildProjectFilterSpecification(String projectName, EProjectStatus projectStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delete"), false));

            if (StringUtils.hasText(projectName)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + projectName.toLowerCase() + "%"));
            }

            if (projectStatus != null) {
                predicates.add(cb.equal(root.get("projectStatus"), projectStatus));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private PaginationResponse<ProjectDto> executePaginatedProjectQuery(
            Specification<ProjectEntity> spec,
            PaginationRequest paginationRequest) {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, DEFAULT_SORT_BY, DEFAULT_DIRECTION);
        Page<ProjectEntity> entityPage = projectRepository.findAll(spec, pageable);
        Page<ProjectDto> dtoPage = projectMapping.toDtoPageable(entityPage);

        String actualSortBy = hasText(paginationRequest.getSortBy()) ? paginationRequest.getSortBy() : DEFAULT_SORT_BY;
        String actualDirection = hasText(paginationRequest.getDirection()) ? paginationRequest.getDirection() : DEFAULT_DIRECTION;
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest,
                actualSortBy,
                actualDirection
        );

        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    private void ensureUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("user.not.found", userId)));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
