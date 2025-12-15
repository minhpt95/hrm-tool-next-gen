package com.vatek.hrmtoolnextgen.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dashboard summary data")
public class DashboardSummaryDto {

    @Schema(description = "Project counts grouped by status")
    private List<ProjectStatusCountDto> projectStatusCounts;

    @Schema(description = "Number of active employees")
    private Long activeEmployeeCount;
}


