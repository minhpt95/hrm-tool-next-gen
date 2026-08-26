package com.minhpt.hrmtoolnextgen.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.minhpt.hrmtoolnextgen.dto.dashboard.DashboardSummaryDto;
import com.minhpt.hrmtoolnextgen.dto.dashboard.ProjectStatusCountDto;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.projection.ProjectStatusCountProjection;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;

/**
 * Unit tests for DashboardService.
 *
 * R20.1 getDashboardSummary — aggregates projectRepository.countProjectsByStatus()
 * into one ProjectStatusCountDto per EProjectStatus value (missing statuses default to 0),
 * and carries userRepository.countByActiveTrueAndDeleteFalse() as activeEmployeeCount.
 *
 * Repo methods under test:
 *   - projectRepository.countProjectsByStatus()
 *   - userRepository.countByActiveTrueAndDeleteFalse()
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    // -------------------------------------------------------------------------
    // R20.1 — happy path: partial status coverage from repo, missing status → 0
    // -------------------------------------------------------------------------

    @Test
    void getDashboardSummary_withKnownCounts_returnsCorrectlyAggregatedDto() {
        // Arrange — repo returns counts for only RUNNING and DONE; INCOMING is absent
        ProjectStatusCountProjection runningProjection = stubProjection(EProjectStatus.RUNNING, 5L);
        ProjectStatusCountProjection doneProjection    = stubProjection(EProjectStatus.DONE,    3L);

        when(projectRepository.countProjectsByStatus())
                .thenReturn(List.of(runningProjection, doneProjection));
        when(userRepository.countByActiveTrueAndDeleteFalse()).thenReturn(12L);

        // Act
        DashboardSummaryDto result = dashboardService.getDashboardSummary();

        // Assert — DTO fields
        assertNotNull(result);
        assertEquals(12L, result.getActiveEmployeeCount());

        // One entry per EProjectStatus value (RUNNING, INCOMING, DONE — 3 total)
        List<ProjectStatusCountDto> counts = result.getProjectStatusCounts();
        assertNotNull(counts);
        assertEquals(EProjectStatus.values().length, counts.size());

        // RUNNING = 5, DONE = 3, INCOMING = 0 (absent from repo result → defaulted)
        assertEquals(5L, countFor(counts, EProjectStatus.RUNNING));
        assertEquals(3L, countFor(counts, EProjectStatus.DONE));
        assertEquals(0L, countFor(counts, EProjectStatus.INCOMING));

        // Assert — correct repo methods called
        verify(projectRepository).countProjectsByStatus();
        verify(userRepository).countByActiveTrueAndDeleteFalse();
    }

    // -------------------------------------------------------------------------
    // R20.1 — all statuses present from repo
    // -------------------------------------------------------------------------

    @Test
    void getDashboardSummary_allStatusesReturnedByRepo_mapsAllCountsCorrectly() {
        ProjectStatusCountProjection running  = stubProjection(EProjectStatus.RUNNING,  10L);
        ProjectStatusCountProjection incoming = stubProjection(EProjectStatus.INCOMING,  4L);
        ProjectStatusCountProjection done     = stubProjection(EProjectStatus.DONE,      7L);

        when(projectRepository.countProjectsByStatus())
                .thenReturn(List.of(running, incoming, done));
        when(userRepository.countByActiveTrueAndDeleteFalse()).thenReturn(50L);

        DashboardSummaryDto result = dashboardService.getDashboardSummary();

        assertEquals(50L, result.getActiveEmployeeCount());

        List<ProjectStatusCountDto> counts = result.getProjectStatusCounts();
        assertEquals(EProjectStatus.values().length, counts.size());
        assertEquals(10L, countFor(counts, EProjectStatus.RUNNING));
        assertEquals(4L,  countFor(counts, EProjectStatus.INCOMING));
        assertEquals(7L,  countFor(counts, EProjectStatus.DONE));
    }

    // -------------------------------------------------------------------------
    // R20.1 — edge case: no projects at all; zero active employees
    // -------------------------------------------------------------------------

    @Test
    void getDashboardSummary_noProjectsAndNoActiveEmployees_returnsAllZeros() {
        when(projectRepository.countProjectsByStatus()).thenReturn(List.of());
        when(userRepository.countByActiveTrueAndDeleteFalse()).thenReturn(0L);

        DashboardSummaryDto result = dashboardService.getDashboardSummary();

        assertNotNull(result);
        assertEquals(0L, result.getActiveEmployeeCount());

        List<ProjectStatusCountDto> counts = result.getProjectStatusCounts();
        // Every EProjectStatus value must still appear with total = 0
        assertEquals(EProjectStatus.values().length, counts.size());
        for (ProjectStatusCountDto dto : counts) {
            assertEquals(0L, dto.getTotal(),
                    "Expected 0 for status " + dto.getStatus() + " when repo returns empty list");
        }

        verify(projectRepository).countProjectsByStatus();
        verify(userRepository).countByActiveTrueAndDeleteFalse();
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private static long countFor(List<ProjectStatusCountDto> counts, EProjectStatus status) {
        return counts.stream()
                .filter(dto -> dto.getStatus() == status)
                .mapToLong(ProjectStatusCountDto::getTotal)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No entry for status: " + status));
    }

    /** Anonymous projection backed by simple return values — no Mockito mock overhead. */
    private static ProjectStatusCountProjection stubProjection(EProjectStatus status, long total) {
        return new ProjectStatusCountProjection() {
            @Override public EProjectStatus getStatus() { return status; }
            @Override public Long           getTotal()  { return total; }
        };
    }
}
