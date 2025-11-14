package com.vatek.hrmtoolnextgen.dto.request;

import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateProjectRequest {
    @NotEmpty
    private String projectName;

    private String projectDescription;

    @NotNull
    private String projectManager;
    private List<Long> memberId;
    @NotNull
    private EProjectStatus projectStatus;
    private String startDate;
}
