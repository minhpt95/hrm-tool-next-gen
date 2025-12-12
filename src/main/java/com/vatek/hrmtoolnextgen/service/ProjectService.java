package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.project.ProjectDto;
import com.vatek.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.request.PaginationRequest;
import com.vatek.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.vatek.hrmtoolnextgen.dto.response.PaginationResponse;
import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import com.vatek.hrmtoolnextgen.exception.BadRequestException;
import com.vatek.hrmtoolnextgen.mapping.ProjectMapping;
import com.vatek.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.vatek.hrmtoolnextgen.repository.jpa.UserRepository;
import com.vatek.hrmtoolnextgen.util.CommonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapping projectMapping;

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjects(PaginationRequest paginationRequest) {
        Page<ProjectEntity> entityPage = projectRepository.findAll(CommonUtils.buildPageable(paginationRequest));
        Page<ProjectDto> dtoPage = projectMapping.toDtoPageable(entityPage);
        return CommonUtils.buildPaginationResponse(dtoPage, paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjectsForAdmin(
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        
        // Build specification for filtering
        Specification<ProjectEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filter by delete = false
            predicates.add(cb.equal(root.get("delete"), false));
            
            // Filter by project name if provided
            if (StringUtils.hasText(projectName)) {
                predicates.add(cb.like(
                    cb.lower(root.get("name")),
                    "%" + projectName.toLowerCase() + "%"
                ));
            }
            
            // Filter by project status if provided
            if (projectStatus != null) {
                predicates.add(cb.equal(root.get("projectStatus"), projectStatus));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        // Build pageable with default sort by createdDate desc
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, "createdDate", "DESC");
        
        Page<ProjectEntity> entityPage = projectRepository.findAll(spec, pageable);
        Page<ProjectDto> dtoPage = projectMapping.toDtoPageable(entityPage);
        
        // Build pagination request for response
        String actualSortBy = paginationRequest.getSortBy() != null && !paginationRequest.getSortBy().isBlank() 
                ? paginationRequest.getSortBy() : "createdDate";
        String actualDirection = paginationRequest.getDirection() != null && !paginationRequest.getDirection().isBlank()
                ? paginationRequest.getDirection() : "DESC";
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest, actualSortBy, actualDirection);
        
        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    /**
     * Builds a specification for filtering projects by name and status
     */
    private Specification<ProjectEntity> buildProjectFilterSpecification(String projectName, EProjectStatus projectStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filter by delete = false
            predicates.add(cb.equal(root.get("delete"), false));
            
            // Filter by project name if provided
            if (StringUtils.hasText(projectName)) {
                predicates.add(cb.like(
                    cb.lower(root.get("name")),
                    "%" + projectName.toLowerCase() + "%"
                ));
            }
            
            // Filter by project status if provided
            if (projectStatus != null) {
                predicates.add(cb.equal(root.get("projectStatus"), projectStatus));
            }
            
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Executes paginated query and builds response
     */
    private PaginationResponse<ProjectDto> executePaginatedProjectQuery(
            Specification<ProjectEntity> spec,
            PaginationRequest paginationRequest) {
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, "createdDate", "DESC");
        
        Page<ProjectEntity> entityPage = projectRepository.findAll(spec, pageable);
        Page<ProjectDto> dtoPage = projectMapping.toDtoPageable(entityPage);
        
        // Build pagination request for response
        String actualSortBy = paginationRequest.getSortBy() != null && !paginationRequest.getSortBy().isBlank() 
                ? paginationRequest.getSortBy() : "createdDate";
        String actualDirection = paginationRequest.getDirection() != null && !paginationRequest.getDirection().isBlank()
                ? paginationRequest.getDirection() : "DESC";
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest, actualSortBy, actualDirection);
        
        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getProjectsByMemberIdWithFilters(
            Long memberId,
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        
        ensureUserExists(memberId);
        
        // Build specification for filtering
        Specification<ProjectEntity> baseSpec = buildProjectFilterSpecification(projectName, projectStatus);
        Specification<ProjectEntity> spec = baseSpec.and((root, query, cb) -> {
            // Join members and filter by member ID
            var membersJoin = root.join("members");
            query.distinct(true); // Ensure distinct results since we're joining with members
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
        
        ensureUserExists(managerId);
        
        // Build specification for filtering
        Specification<ProjectEntity> baseSpec = buildProjectFilterSpecification(projectName, projectStatus);
        Specification<ProjectEntity> spec = baseSpec.and((root, query, cb) -> 
            cb.equal(root.get("projectManager").get("id"), managerId)
        );
        
        return executePaginatedProjectQuery(spec, paginationRequest);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Project not found with id: " + id));
        return projectMapping.toDto(projectEntity);
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        // Check if project name already exists using a query instead of loading all projects
        if (projectRepository.existsByNameIgnoreCaseAndDeleteFalse(request.getProjectName())) {
            throw new BadRequestException("Project with name '" + request.getProjectName() + "' already exists");
        }

        ProjectEntity projectEntity = projectMapping.fromCreateRequest(request);

        // Set project manager
        if (request.getProjectManager() != null) {
            UserEntity manager = userRepository.findById(request.getProjectManager())
                    .orElseThrow(() -> new BadRequestException("Project manager not found with id: " + request.getProjectManager()));
            projectEntity.setProjectManager(manager);
        }

        // Set start time
        if (request.getStartDate() != null) {
            projectEntity.setStartTime(request.getStartDate());
        } else {
            projectEntity.setStartTime(LocalDate.now());
        }

        // Add members
        if (request.getMemberId() != null && !request.getMemberId().isEmpty()) {
            List<UserEntity> members = userRepository.findAllById(request.getMemberId());
            if (members.size() != request.getMemberId().size()) {
                throw new BadRequestException("Some member IDs are invalid");
            }
            members.forEach(projectEntity::addMemberToProject);
        }

        projectEntity.setDelete(false);
        ProjectEntity savedEntity = projectRepository.save(projectEntity);
        log.info("Created project with id: {}", savedEntity.getId());
        return projectMapping.toDto(savedEntity);
    }

    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Project not found with id: " + id));

        if (projectEntity.isDelete()) {
            throw new BadRequestException("Cannot update deleted project");
        }

        // Check if project name already exists (excluding current project)
        if (request.getProjectName() != null && !request.getProjectName().equals(projectEntity.getName())) {
            // Use a query to check if another project with the same name exists
            Specification<ProjectEntity> nameSpec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(cb.lower(root.get("name")), request.getProjectName().toLowerCase()));
                predicates.add(cb.equal(root.get("delete"), false));
                predicates.add(cb.notEqual(root.get("id"), id));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            if (projectRepository.count(nameSpec) > 0) {
                throw new BadRequestException("Project with name '" + request.getProjectName() + "' already exists");
            }
        }

        // Update basic fields
        projectMapping.updateEntityFromRequest(request, projectEntity);

        // Update project manager
        if (request.getProjectManager() != null) {
            UserEntity manager = userRepository.findById(request.getProjectManager())
                    .orElseThrow(() -> new BadRequestException("Project manager not found with id: " + request.getProjectManager()));
            projectEntity.setProjectManager(manager);
        }

        // Update dates
        if (request.getStartDate() != null) {
            projectEntity.setStartTime(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            projectEntity.setEndTime(request.getEndDate());
        }

        // Update members
        if (request.getMemberId() != null) {
            // Remove all existing members
            new ArrayList<>(projectEntity.getMembers()).forEach(projectEntity::removeMemberFromProject);

            // Add new members
            if (!request.getMemberId().isEmpty()) {
                List<UserEntity> members = userRepository.findAllById(request.getMemberId());
                if (members.size() != request.getMemberId().size()) {
                    throw new BadRequestException("Some member IDs are invalid");
                }
                members.forEach(projectEntity::addMemberToProject);
            }
        }

        ProjectEntity updatedEntity = projectRepository.save(projectEntity);
        log.info("Updated project with id: {}", updatedEntity.getId());
        return projectMapping.toDto(updatedEntity);
    }

    @Transactional
    public void deleteProject(Long id) {
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Project not found with id: " + id));

        if (projectEntity.isDelete()) {
            throw new BadRequestException("Project already deleted");
        }

        projectEntity.setDelete(true);
        projectRepository.save(projectEntity);
        log.info("Deleted project with id: {}", id);
    }

    private void ensureUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + userId));
    }

    private LocalDate parseDate(String dateString) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            try {
                // Try parsing as LocalDate to provide a helpful error
                return java.time.LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e2) {
                log.error("Error parsing date: {}", dateString, e2);
                throw new BadRequestException("Invalid date format. Expected format: dd/MM/yyyy");
            }
        }
    }
}

