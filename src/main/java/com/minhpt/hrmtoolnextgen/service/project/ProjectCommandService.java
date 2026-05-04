package com.minhpt.hrmtoolnextgen.service.project;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.ProjectMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProjectCommandService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapping projectMapping;
    private final MessageService messageService;

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        log.info("Creating project with name: {}", request.getProjectName());
        assertProjectNameAvailable(request.getProjectName());

        ProjectEntity projectEntity = projectMapping.fromCreateRequest(request);
        projectEntity.setProjectManager(resolveManager(request.getProjectManager()));
        projectEntity.setStartTime(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        assignMembers(projectEntity, request.getMemberId());
        projectEntity.setDelete(false);

        ProjectEntity savedEntity = projectRepository.save(projectEntity);
        log.info("Created project with id: {}", savedEntity.getId());
        return projectMapping.toDto(savedEntity);
    }

    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
        log.info("Updating project with id: {}", id);
        ProjectEntity projectEntity = getProjectForUpdate(id);

        if (request.getProjectName() != null && !request.getProjectName().equals(projectEntity.getName())) {
            assertProjectNameAvailableForUpdate(id, request.getProjectName());
        }

        projectMapping.updateEntityFromRequest(request, projectEntity);

        if (request.getProjectManager() != null) {
            projectEntity.setProjectManager(resolveManager(request.getProjectManager()));
        }
        if (request.getStartDate() != null) {
            projectEntity.setStartTime(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            projectEntity.setEndTime(request.getEndDate());
        }
        if (request.getMemberIds() != null) {
            replaceMembers(projectEntity, request.getMemberIds());
        }

        ProjectEntity updatedEntity = projectRepository.save(projectEntity);
        log.info("Updated project with id: {}", updatedEntity.getId());
        return projectMapping.toDto(updatedEntity);
    }

    @Transactional
    public void deleteProject(Long id) {
        log.info("Deleting project with id: {}", id);
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("project.not.found", id)));

        if (projectEntity.isDelete()) {
            throw new BadRequestException(messageService.getMessage("project.already.deleted"));
        }

        projectEntity.setDelete(true);
        projectRepository.save(projectEntity);
        log.info("Deleted project with id: {}", id);
    }

    private void assertProjectNameAvailable(String projectName) {
        if (projectRepository.existsByNameIgnoreCaseAndDeleteFalse(projectName)) {
            throw new BadRequestException(messageService.getMessage("project.name.exists", projectName));
        }
    }

    private void assertProjectNameAvailableForUpdate(Long projectId, String projectName) {
        Specification<ProjectEntity> nameSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(cb.lower(root.get("name")), projectName.toLowerCase()));
            predicates.add(cb.equal(root.get("delete"), false));
            predicates.add(cb.notEqual(root.get("id"), projectId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        if (projectRepository.count(nameSpec) > 0) {
            throw new BadRequestException(messageService.getMessage("project.name.exists", projectName));
        }
    }

    private ProjectEntity getProjectForUpdate(Long id) {
        ProjectEntity projectEntity = projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("project.not.found", id)));

        if (projectEntity.isDelete()) {
            throw new BadRequestException(messageService.getMessage("project.cannot.update.deleted"));
        }
        return projectEntity;
    }

    private UserEntity resolveManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("project.manager.not.found", managerId)));
    }

    private void assignMembers(ProjectEntity projectEntity, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        resolveMembers(memberIds).forEach(projectEntity::addMemberToProject);
    }

    private void replaceMembers(ProjectEntity projectEntity, List<Long> memberIds) {
        new ArrayList<>(projectEntity.getMembers()).forEach(projectEntity::removeMemberFromProject);
        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }
        resolveMembers(memberIds).forEach(projectEntity::addMemberToProject);
    }

    private List<UserEntity> resolveMembers(List<Long> memberIds) {
        List<UserEntity> members = userRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new BadRequestException(messageService.getMessage("project.member.ids.invalid"));
        }
        return members;
    }
}
