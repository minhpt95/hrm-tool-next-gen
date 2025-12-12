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
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<ProjectDto> getProjectsByMemberId(Long memberId) {
        ensureUserExists(memberId);
        List<ProjectEntity> projects = projectRepository.findDistinctByMembers_IdAndDeleteFalseAndProjectStatus(
                memberId, EProjectStatus.RUNNING);
        return projectMapping.toDto(projects);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto> getProjectsByManagerId(Long managerId) {
        ensureUserExists(managerId);
        List<ProjectEntity> projects = projectRepository.findByProjectManager_IdAndDeleteFalseAndProjectStatus(
                managerId, EProjectStatus.RUNNING);
        return projectMapping.toDto(projects);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Project not found with id: " + id));
        return projectMapping.toDto(projectEntity);
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        // Check if project name already exists
        projectRepository.findAll().stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getProjectName()) && !p.isDelete())
                .findFirst()
                .ifPresent(p -> {
                    throw new BadRequestException("Project with name '" + request.getProjectName() + "' already exists");
                });

        ProjectEntity projectEntity = projectMapping.fromCreateRequest(request);

        // Set project manager
        if (request.getProjectManager() != null) {
            UserEntity manager = userRepository.findById(request.getProjectManager())
                    .orElseThrow(() -> new BadRequestException("Project manager not found with id: " + request.getProjectManager()));
            projectEntity.setProjectManager(manager);
        }

        // Set start time
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            LocalDate startTime = parseDate(request.getStartDate());
            projectEntity.setStartTime(startTime);
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
            projectRepository.findAll().stream()
                    .filter(p -> p.getName().equalsIgnoreCase(request.getProjectName())
                            && !p.isDelete()
                            && !p.getId().equals(id))
                    .findFirst()
                    .ifPresent(p -> {
                        throw new BadRequestException("Project with name '" + request.getProjectName() + "' already exists");
                    });
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
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            projectEntity.setStartTime(parseDate(request.getStartDate()));
        }
        if (request.getEndDate() != null && !request.getEndDate().isBlank()) {
            projectEntity.setEndTime(parseDate(request.getEndDate()));
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

