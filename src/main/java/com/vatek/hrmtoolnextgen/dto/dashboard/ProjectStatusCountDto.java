package com.vatek.hrmtoolnextgen.dto.dashboard;

import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Aggregated count for a given project status")
public class ProjectStatusCountDto {

    @Schema(description = "Project status")
    private EProjectStatus status;

    @Schema(description = "Number of projects with the status")
    private Long total;
}

