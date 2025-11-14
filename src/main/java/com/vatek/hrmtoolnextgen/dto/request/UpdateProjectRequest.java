package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for updating an existing project")
public class UpdateProjectRequest {
    @NotEmpty
    @Schema(description = "Updated project name")
    private String projectName;

    @Schema(description = "Updated project description")
    private String projectDescription;

    @Schema(description = "Email of the new project manager")
    private String projectManager;

    @Schema(description = "Replacement list of member IDs")
    private List<Long> memberId;

    @Schema(description = "Updated lifecycle status")
    private EProjectStatus projectStatus;

    @Schema(description = "New start date in dd/MM/yyyy format")
    private String startDate;

    @Schema(description = "End date in dd/MM/yyyy format")
    private String endDate;
}

