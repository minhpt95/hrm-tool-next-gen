package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for creating a project")
public class CreateProjectRequest {
    @NotEmpty
    @Schema(description = "Project name displayed to end users")
    private String projectName;

    @Schema(description = "Optional project summary/description")
    private String projectDescription;

    @NotNull
    @Schema(description = "ID of the assigned project manager")
    private Long projectManager;

    @Schema(description = "IDs of users to add as project members")
    private List<Long> memberId;

    @NotNull
    @Schema(description = "Lifecycle status of the project")
    private EProjectStatus projectStatus;

    @Schema(description = "Planned start date in dd/MM/yyyy format")
    private String startDate;
}
