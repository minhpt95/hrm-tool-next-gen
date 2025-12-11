package com.vatek.hrmtoolnextgen.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Project data exposed via the API")
public class ProjectDto {
    @Schema(description = "Unique identifier of the project")
    private Long id;

    @Schema(description = "Project name")
    private String name;

    @Schema(description = "Project description")
    private String description;

    @Schema(description = "Flag indicating whether the project is soft deleted")
    private Boolean isDelete;

    @Schema(description = "Lifecycle status of the project")
    private EProjectStatus projectStatus;

    @Schema(description = "Assigned project manager summary")
    private UserDto managerUser;

    @Schema(description = "Team members in this project")
    private List<UserDto> members;

    @Schema(description = "Planned start time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate startTime;

    @Schema(description = "Planned end time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate endTime;
}
