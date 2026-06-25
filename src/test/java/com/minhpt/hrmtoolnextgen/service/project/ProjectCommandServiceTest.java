package com.minhpt.hrmtoolnextgen.service.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.ProjectMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for ProjectCommandService.
 *
 * Delete mechanism: ACTIVE-FLAG soft-delete — deleteProject() sets isDelete = true
 * and calls projectRepository.save(). The @SQLDelete annotation on ProjectEntity
 * is NOT triggered because the code does not call repository.delete(entity).
 *
 * R13.1 createProject — persists; repo.save invoked; dto returned; delete=false set.
 * R13.2 updateProject — updates fields; not-found id → NotFoundException.
 * R13.3 deleteProject — sets isDelete=true; repo.save invoked; not-found → NotFoundException;
 *                        already-deleted → BadRequestException.
 */
@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository    userRepository;
    @Mock private ProjectMapping    projectMapping;
    @Mock private MessageService    messageService;

    @InjectMocks
    private ProjectCommandService commandService;

    private static final long PROJECT_ID = 10L;
    private static final long MANAGER_ID = 1L;

    // -------------------------------------------------------------------------
    // R13.1 — createProject: happy path
    // -------------------------------------------------------------------------

    @Test
    void createProject_validRequest_savesEntityAndReturnsDto() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);

        ProjectEntity mappedEntity = Fixtures.buildProject(1L, manager);
        when(projectMapping.fromCreateRequest(any())).thenReturn(mappedEntity);
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Alpha")).thenReturn(false);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        ProjectEntity saved = Fixtures.buildProject(1L, manager);
        saved.setId(PROJECT_ID);
        when(projectRepository.save(any(ProjectEntity.class))).thenReturn(saved);

        ProjectDto expectedDto = new ProjectDto();
        expectedDto.setId(PROJECT_ID);
        expectedDto.setProjectStatus(EProjectStatus.RUNNING);
        when(projectMapping.toDto(saved)).thenReturn(expectedDto);

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Alpha");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.RUNNING);

        ProjectDto result = commandService.createProject(req);

        // repo.save must be called once
        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captor.capture());

        // delete flag must be false on the entity that was saved
        assertFalse(captor.getValue().isDelete(),
                "createProject must set isDelete = false before save");

        assertNotNull(result);
        assertEquals(PROJECT_ID, result.getId());
    }

    @Test
    void createProject_noStartDateProvided_defaultsToToday() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);

        ProjectEntity mappedEntity = Fixtures.buildProject(1L, manager);
        when(projectMapping.fromCreateRequest(any())).thenReturn(mappedEntity);
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Beta")).thenReturn(false);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        ProjectEntity saved = Fixtures.buildProject(1L, manager);
        saved.setId(PROJECT_ID);
        when(projectRepository.save(any())).thenReturn(saved);
        when(projectMapping.toDto(saved)).thenReturn(new ProjectDto());

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Beta");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.INCOMING);
        // startDate is null → service should default to LocalDate.now()

        commandService.createProject(req);

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectRepository).save(captor.capture());
        assertNotNull(captor.getValue().getStartTime(),
                "startTime must be set to today when not provided in request");
    }

    @Test
    void createProject_duplicateName_throwsBadRequestException() {
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Duplicate")).thenReturn(true);
        when(messageService.getMessage("project.name.exists", "Duplicate")).thenReturn("Name exists");

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Duplicate");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.RUNNING);

        assertThrows(BadRequestException.class, () -> commandService.createProject(req));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void createProject_managerNotFound_throwsNotFoundException() {
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Gamma")).thenReturn(false);

        ProjectEntity mappedEntity = Fixtures.buildProject(1L);
        when(projectMapping.fromCreateRequest(any())).thenReturn(mappedEntity);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.empty());
        when(messageService.getMessage("project.manager.not.found", MANAGER_ID)).thenReturn("Manager not found");

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Gamma");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.RUNNING);

        assertThrows(NotFoundException.class, () -> commandService.createProject(req));
    }

    // -------------------------------------------------------------------------
    // R13.2 — updateProject: happy path
    // -------------------------------------------------------------------------

    @Test
    void updateProject_existingProject_updatesFieldsAndReturnsDto() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        ProjectEntity existing = Fixtures.buildProject(1L, manager);
        existing.setId(PROJECT_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(existing));

        ProjectEntity updated = Fixtures.buildProject(1L, manager);
        updated.setId(PROJECT_ID);
        when(projectRepository.save(existing)).thenReturn(updated);

        ProjectDto expectedDto = new ProjectDto();
        expectedDto.setId(PROJECT_ID);
        when(projectMapping.toDto(updated)).thenReturn(expectedDto);

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setProjectName("Updated Name");
        req.setProjectStatus(EProjectStatus.DONE);
        req.setStartDate(LocalDate.of(2026, 3, 1));
        req.setEndDate(LocalDate.of(2026, 12, 31));

        ProjectDto result = commandService.updateProject(PROJECT_ID, req);

        verify(projectRepository).save(existing);
        assertNotNull(result);
    }

    @Test
    void updateProject_notFoundId_throwsNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("project.not.found", 99L)).thenReturn("Not found");

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setProjectName("Any");

        assertThrows(NotFoundException.class, () -> commandService.updateProject(99L, req));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_deletedProject_throwsBadRequestException() {
        ProjectEntity deleted = Fixtures.buildProject(1L);
        deleted.setId(PROJECT_ID);
        deleted.setDelete(true);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(deleted));
        when(messageService.getMessage("project.cannot.update.deleted")).thenReturn("Cannot update deleted");

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setProjectName("New Name");

        assertThrows(BadRequestException.class, () -> commandService.updateProject(PROJECT_ID, req));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void updateProject_withNewManager_resolvesManager() {
        long newManagerId = 42L;
        UserEntity newManager = Fixtures.buildUser(newManagerId);
        ProjectEntity existing = Fixtures.buildProject(1L);
        existing.setId(PROJECT_ID);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(existing));
        when(userRepository.findById(newManagerId)).thenReturn(Optional.of(newManager));

        ProjectEntity savedEntity = Fixtures.buildProject(1L, newManager);
        savedEntity.setId(PROJECT_ID);
        when(projectRepository.save(existing)).thenReturn(savedEntity);
        when(projectMapping.toDto(savedEntity)).thenReturn(new ProjectDto());

        UpdateProjectRequest req = new UpdateProjectRequest();
        req.setProjectName("Renamed");
        req.setProjectManager(newManagerId);

        commandService.updateProject(PROJECT_ID, req);

        assertEquals(newManager, existing.getProjectManager(),
                "project manager must be updated to the resolved UserEntity");
    }

    // -------------------------------------------------------------------------
    // R13.3 — deleteProject: soft-delete mechanism (active-flag, NOT @SQLDelete)
    //
    // The service calls setDelete(true) + save(), NOT repository.delete(entity).
    // Therefore @SQLDelete is irrelevant here; we assert the flag is set and save is called.
    // -------------------------------------------------------------------------

    @Test
    void deleteProject_existingProject_setsDeleteFlagAndCallsSave() {
        ProjectEntity existing = Fixtures.buildProject(1L);
        existing.setId(PROJECT_ID);
        existing.setDelete(false);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(existing));
        when(projectRepository.save(existing)).thenReturn(existing);

        commandService.deleteProject(PROJECT_ID);

        // Soft-delete: isDelete flag must be true after the call
        assertTrue(existing.isDelete(), "deleteProject must set isDelete = true (active-flag soft-delete)");

        // Save must be invoked (not delete!)
        verify(projectRepository).save(existing);
    }

    @Test
    void deleteProject_notFoundId_throwsNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("project.not.found", 99L)).thenReturn("Not found");

        assertThrows(NotFoundException.class, () -> commandService.deleteProject(99L));
        verify(projectRepository, never()).save(any());
    }

    @Test
    void deleteProject_alreadyDeletedProject_throwsBadRequestException() {
        ProjectEntity alreadyDeleted = Fixtures.buildProject(1L);
        alreadyDeleted.setId(PROJECT_ID);
        alreadyDeleted.setDelete(true);

        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(alreadyDeleted));
        when(messageService.getMessage("project.already.deleted")).thenReturn("Already deleted");

        assertThrows(BadRequestException.class, () -> commandService.deleteProject(PROJECT_ID));
        verify(projectRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // R13.1 — createProject with members
    // -------------------------------------------------------------------------

    @Test
    void createProject_withValidMembers_assignsMembersToProject() {
        long memberId1 = 2L;
        long memberId2 = 3L;
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        UserEntity member1 = Fixtures.buildUser(memberId1);
        UserEntity member2 = Fixtures.buildUser(memberId2);

        ProjectEntity mappedEntity = Fixtures.buildProject(1L, manager);
        when(projectMapping.fromCreateRequest(any())).thenReturn(mappedEntity);
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Delta")).thenReturn(false);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        when(userRepository.findAllById(List.of(memberId1, memberId2)))
                .thenReturn(List.of(member1, member2));

        ProjectEntity saved = Fixtures.buildProject(1L, manager);
        saved.setId(PROJECT_ID);
        when(projectRepository.save(any())).thenReturn(saved);
        when(projectMapping.toDto(saved)).thenReturn(new ProjectDto());

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Delta");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.RUNNING);
        req.setMemberId(List.of(memberId1, memberId2));

        commandService.createProject(req);

        verify(userRepository).findAllById(List.of(memberId1, memberId2));
    }

    @Test
    void createProject_withInvalidMemberIds_throwsBadRequestException() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);

        ProjectEntity mappedEntity = Fixtures.buildProject(1L, manager);
        when(projectMapping.fromCreateRequest(any())).thenReturn(mappedEntity);
        when(projectRepository.existsByNameIgnoreCaseAndDeleteFalse("Epsilon")).thenReturn(false);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));
        // Only 1 of 2 requested IDs found
        when(userRepository.findAllById(List.of(99L, 100L)))
                .thenReturn(List.of(Fixtures.buildUser(99L)));
        when(messageService.getMessage("project.member.ids.invalid")).thenReturn("Invalid member IDs");

        CreateProjectRequest req = new CreateProjectRequest();
        req.setProjectName("Epsilon");
        req.setProjectManager(MANAGER_ID);
        req.setProjectStatus(EProjectStatus.RUNNING);
        req.setMemberId(List.of(99L, 100L));

        assertThrows(BadRequestException.class, () -> commandService.createProject(req));
        verify(projectRepository, never()).save(any());
    }
}
