package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.dto.dashboard.DashboardSummaryDto;
import com.minhpt.hrmtoolnextgen.dto.dashboard.ProjectStatusCountDto;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.projection.ProjectStatusCountProjection;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class DashboardService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        log.info("Fetching dashboard summary");
        List<ProjectStatusCountDto> projectStatusCounts = buildProjectStatusCounts();
        long activeEmployees = userRepository.countByActiveTrueAndDeleteFalse();

        log.debug("Dashboard summary - projectStatusCounts: {}, activeEmployees: {}", projectStatusCounts, activeEmployees);

        return new DashboardSummaryDto(projectStatusCounts, activeEmployees);
    }

    private List<ProjectStatusCountDto> buildProjectStatusCounts() {
        Map<EProjectStatus, Long> counts = new EnumMap<>(EProjectStatus.class);
        List<ProjectStatusCountProjection> aggregated = projectRepository.countProjectsByStatus();

        aggregated.forEach(projection -> counts.put(projection.getStatus(), projection.getTotal()));

        return Arrays.stream(EProjectStatus.values())
                .map(status -> new ProjectStatusCountDto(status, counts.getOrDefault(status, 0L)))
                .toList();
    }
}


