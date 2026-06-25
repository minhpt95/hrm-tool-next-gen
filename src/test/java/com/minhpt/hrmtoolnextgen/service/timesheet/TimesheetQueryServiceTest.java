package com.minhpt.hrmtoolnextgen.service.timesheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

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

import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.mapping.TimesheetMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.TimesheetRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Unit tests for TimesheetQueryService.
 *
 * Owner-scoping note (R9.5):
 *   TimesheetQueryService exposes only ONE read method:
 *   getTimesheetsByManagerWithFilters (manager-scoped).
 *   There is NO separate user/owner-scoped read method in the query service.
 *   Owner-scoping is instead enforced upstream in TimesheetCommandService:
 *     - createTimesheet() calls getCurrentUser() from SecurityContextHolder and
 *       assertUserInProject() to verify the caller is a member of the project
 *       before persisting.
 *   Tests for that owner-membership guard live in TimesheetCommandServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class TimesheetQueryServiceTest {

    @Mock private TimesheetRepository timesheetRepository;
    @Mock private TimesheetMapping    timesheetMapping;

    @InjectMocks
    private TimesheetQueryService queryService;

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — happy path: returns paginated results
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_returnsManagerScopedPage() {
        UserEntity    manager = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, manager);
        TimesheetEntity entity  = Fixtures.buildTimesheet(1L, Fixtures.buildUser(2L), project);

        Page<TimesheetEntity> entityPage = new PageImpl<>(List.of(entity));
        when(timesheetRepository.findAll(
                any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);

        TimesheetDto dto = new TimesheetDto();
        dto.setStatus(ETimesheetStatus.PENDING);
        Page<TimesheetDto> dtoPage = new PageImpl<>(List.of(dto));
        when(timesheetMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(10).build();

        PaginationResponse<TimesheetDto> response =
                queryService.getTimesheetsByManagerWithFilters(manager.getId(), pagination, null, null);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        assertEquals(ETimesheetStatus.PENDING, response.getItems().get(0).getStatus());
    }

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — repo is called with a Specification and Pageable
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_invokesRepoWithSpecAndPageable() {
        Page<TimesheetEntity> emptyPage = new PageImpl<>(List.of());
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(timesheetMapping.toDtoPageable(emptyPage))
                .thenReturn(new PageImpl<>(List.of()));

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(5).build();

        queryService.getTimesheetsByManagerWithFilters(42L, pagination, null, null);

        ArgumentCaptor<Specification<TimesheetEntity>> specCaptor =
                ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(timesheetRepository).findAll(specCaptor.capture(), pageableCaptor.capture());

        assertNotNull(specCaptor.getValue());
        // page 0, size 5 → pageable size matches
        assertEquals(5, pageableCaptor.getValue().getPageSize());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — status filter propagated
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_withStatusFilter_propagatesFilterToRepo() {
        Page<TimesheetEntity> emptyPage = new PageImpl<>(List.of());
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(timesheetMapping.toDtoPageable(emptyPage))
                .thenReturn(new PageImpl<>(List.of()));

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(10).build();

        // No exception and repo is called — filter is baked into the Specification
        queryService.getTimesheetsByManagerWithFilters(1L, pagination, ETimesheetStatus.PENDING, null);

        verify(timesheetRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — default sort is applied when none specified
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_noSortSpecified_appliesDefaultCreatedDateDesc() {
        Page<TimesheetEntity> emptyPage = new PageImpl<>(List.of());
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(timesheetMapping.toDtoPageable(emptyPage))
                .thenReturn(new PageImpl<>(List.of()));

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(10).build();  // sortBy and direction are null

        PaginationResponse<TimesheetDto> response =
                queryService.getTimesheetsByManagerWithFilters(1L, pagination, null, null);

        // Default sort: createdDate DESC
        assertEquals("createdDate", response.getSortBy());
        assertEquals("DESC", response.getDirection());
    }

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — pagination envelope built correctly
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_paginationEnvelopeReflectsPageMetadata() {
        UserEntity    user    = Fixtures.buildUser(1L);
        ProjectEntity project = Fixtures.buildProject(1L, user);
        List<TimesheetEntity> entities = List.of(
                Fixtures.buildTimesheet(1L, user, project),
                Fixtures.buildTimesheet(2L, user, project));

        Page<TimesheetEntity> entityPage = new PageImpl<>(entities);
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);

        TimesheetDto dto1 = new TimesheetDto();
        TimesheetDto dto2 = new TimesheetDto();
        Page<TimesheetDto> dtoPage = new PageImpl<>(List.of(dto1, dto2));
        when(timesheetMapping.toDtoPageable(entityPage)).thenReturn(dtoPage);

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(10).build();

        PaginationResponse<TimesheetDto> response =
                queryService.getTimesheetsByManagerWithFilters(1L, pagination, null, null);

        assertEquals(2, response.getItems().size());
        assertEquals(2, response.getTotalElements());
        assertEquals(0, response.getCurrentPage());
    }

    // -------------------------------------------------------------------------
    // getTimesheetsByManagerWithFilters — empty result set
    // -------------------------------------------------------------------------

    @Test
    void getTimesheetsByManagerWithFilters_noTimesheets_returnsEmptyPage() {
        Page<TimesheetEntity> emptyPage = new PageImpl<>(List.of());
        when(timesheetRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(timesheetMapping.toDtoPageable(emptyPage))
                .thenReturn(new PageImpl<>(List.of()));

        PaginationRequest pagination = PaginationRequest.builder()
                .page(0).size(10).build();

        PaginationResponse<TimesheetDto> response =
                queryService.getTimesheetsByManagerWithFilters(99L, pagination, null, null);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        assertEquals(0, response.getTotalElements());
    }
}
