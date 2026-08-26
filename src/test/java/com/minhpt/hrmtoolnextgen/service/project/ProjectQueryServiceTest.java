package com.minhpt.hrmtoolnextgen.service.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.ProjectMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for ProjectQueryService.
 *
 * R13.4 getAllProjects — delegates to projectRepository.findAll(Pageable).
 * R13.4 getAllProjectsForAdmin — delegates to projectRepository.findAll(Spec, Pageable).
 * R13.4 getProjectsByManagerIdWithFilters — user existence checked; spec + pageable forwarded.
 * R8.4  getProjectsByMemberIdWithFilters  — user existence checked; spec + pageable forwarded.
 * R13.4 getProjectById — not-found id → NotFoundException.
 *
 * Fetch-plan / N+1 concerns are already covered by the two existing methods in
 * ProjectRepositoryFetchPlanTest; no new fetch-plan assertions are added here.
 */
@ExtendWith(MockitoExtension.class)
class ProjectQueryServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository    userRepository;
    @Mock private ProjectMapping    projectMapping;
    @Mock private MessageService    messageService;

    @InjectMocks
    private ProjectQueryService queryService;

    private static final long MANAGER_ID = 1L;
    private static final long MEMBER_ID  = 2L;
    private static final long PROJECT_ID = 10L;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PaginationRequest defaultPage() {
        return PaginationRequest.builder().page(0).size(10).build();
    }

    // -------------------------------------------------------------------------
    // R13.4 — getAllProjects: delegates to repo.findAll(Pageable)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getAllProjects_returnsPagedResult() {
        ProjectEntity entity = Fixtures.buildProject(1L);
        Page<ProjectEntity> entityPage = new PageImpl<>(List.of(entity));
        when(projectRepository.findAll(any(Pageable.class))).thenReturn(entityPage);

        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of(dto)));

        PaginationResponse<ProjectDto> result = queryService.getAllProjects(defaultPage());

        verify(projectRepository).findAll(any(Pageable.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllProjects_emptyRepository_returnsEmptyPage() {
        Page<ProjectEntity> emptyPage = new PageImpl<>(List.of());
        when(projectRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);
        when(projectMapping.toDtoPageable(emptyPage)).thenReturn(new PageImpl<>(List.of()));

        PaginationResponse<ProjectDto> result = queryService.getAllProjects(defaultPage());

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // R13.4 — getAllProjectsForAdmin: delegates to repo.findAll(Spec, Pageable)
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getAllProjectsForAdmin_noFilters_queriesWithSpecAndPageable() {
        ProjectEntity entity = Fixtures.buildProject(1L);
        Page<ProjectEntity> entityPage = new PageImpl<>(List.of(entity));
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of(dto)));

        PaginationResponse<ProjectDto> result = queryService.getAllProjectsForAdmin(defaultPage(), null, null);

        verify(projectRepository).findAll(any(Specification.class), any(Pageable.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAllProjectsForAdmin_withNameAndStatusFilters_stillDelegatesToRepoWithSpec() {
        Page<ProjectEntity> entityPage = new PageImpl<>(List.of());
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of()));

        PaginationResponse<ProjectDto> result =
                queryService.getAllProjectsForAdmin(defaultPage(), "Alpha", EProjectStatus.RUNNING);

        ArgumentCaptor<Specification<ProjectEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(projectRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertNotNull(specCaptor.getValue());
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    // -------------------------------------------------------------------------
    // R13.4 — getProjectsByManagerIdWithFilters
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getProjectsByManagerIdWithFilters_existingManager_returnsPage() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        ProjectEntity entity = Fixtures.buildProject(1L, manager);
        Page<ProjectEntity> entityPage = new PageImpl<>(List.of(entity));
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of(dto)));

        PaginationResponse<ProjectDto> result =
                queryService.getProjectsByManagerIdWithFilters(MANAGER_ID, defaultPage(), null, null);

        verify(userRepository).findById(MANAGER_ID);
        verify(projectRepository).findAll(any(Specification.class), any(Pageable.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProjectsByManagerIdWithFilters_managerNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found", 99L)).thenReturn("User not found");

        assertThrows(NotFoundException.class,
                () -> queryService.getProjectsByManagerIdWithFilters(99L, defaultPage(), null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProjectsByManagerIdWithFilters_withFilters_stillDelegatesToRepoWithSpec() {
        UserEntity manager = Fixtures.buildUser(MANAGER_ID);
        when(userRepository.findById(MANAGER_ID)).thenReturn(Optional.of(manager));

        Page<ProjectEntity> entityPage = new PageImpl<>(List.of());
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of()));

        PaginationResponse<ProjectDto> result =
                queryService.getProjectsByManagerIdWithFilters(MANAGER_ID, defaultPage(), "ProjectX", EProjectStatus.DONE);

        ArgumentCaptor<Specification<ProjectEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(projectRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertNotNull(specCaptor.getValue());
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // R8.4 — getProjectsByMemberIdWithFilters
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void getProjectsByMemberIdWithFilters_existingMember_returnsPage() {
        UserEntity member = Fixtures.buildUser(MEMBER_ID);
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        ProjectEntity entity = Fixtures.buildProject(1L);
        Page<ProjectEntity> entityPage = new PageImpl<>(List.of(entity));
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);

        ProjectDto dto = new ProjectDto();
        dto.setId(1L);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of(dto)));

        PaginationResponse<ProjectDto> result =
                queryService.getProjectsByMemberIdWithFilters(MEMBER_ID, defaultPage(), null, null);

        verify(userRepository).findById(MEMBER_ID);
        verify(projectRepository).findAll(any(Specification.class), any(Pageable.class));
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getProjectsByMemberIdWithFilters_memberNotFound_throwsNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("user.not.found", 99L)).thenReturn("User not found");

        assertThrows(NotFoundException.class,
                () -> queryService.getProjectsByMemberIdWithFilters(99L, defaultPage(), null, null));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getProjectsByMemberIdWithFilters_withFilters_stillDelegatesToRepoWithSpec() {
        UserEntity member = Fixtures.buildUser(MEMBER_ID);
        when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        Page<ProjectEntity> entityPage = new PageImpl<>(List.of());
        when(projectRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
        when(projectMapping.toDtoPageable(entityPage)).thenReturn(new PageImpl<>(List.of()));

        PaginationResponse<ProjectDto> result =
                queryService.getProjectsByMemberIdWithFilters(MEMBER_ID, defaultPage(), "ProjectY", EProjectStatus.RUNNING);

        ArgumentCaptor<Specification<ProjectEntity>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(projectRepository).findAll(specCaptor.capture(), any(Pageable.class));
        assertNotNull(specCaptor.getValue());
        assertNotNull(result);
    }

    // -------------------------------------------------------------------------
    // R13.4 — getProjectById
    // -------------------------------------------------------------------------

    @Test
    void getProjectById_existingId_returnsDto() {
        ProjectEntity entity = Fixtures.buildProject(1L);
        entity.setId(PROJECT_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(entity));

        ProjectDto dto = new ProjectDto();
        dto.setId(PROJECT_ID);
        when(projectMapping.toDto(entity)).thenReturn(dto);

        ProjectDto result = queryService.getProjectById(PROJECT_ID);

        verify(projectRepository).findById(PROJECT_ID);
        assertNotNull(result);
        assertEquals(PROJECT_ID, result.getId());
    }

    @Test
    void getProjectById_notFoundId_throwsNotFoundException() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessage("project.not.found", 99L)).thenReturn("Not found");

        assertThrows(NotFoundException.class, () -> queryService.getProjectById(99L));
    }
}
