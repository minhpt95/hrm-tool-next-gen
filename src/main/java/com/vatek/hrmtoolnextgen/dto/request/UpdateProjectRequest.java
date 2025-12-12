package com.vatek.hrmtoolnextgen.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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

    @Schema(description = "ID of the new project manager")
    private Long projectManager;

    @Schema(description = "Replacement list of member IDs")
    private List<Long> memberId;

    @Schema(description = "Updated lifecycle status")
    private EProjectStatus projectStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "New start date")
    private LocalDate startDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @Schema(description = "End date")
    private LocalDate endDate;
}

