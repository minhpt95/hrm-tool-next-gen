package com.vatek.hrmtoolnextgen.dto.project;

import com.vatek.hrmtoolnextgen.dto.user.UserDto;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private Long id;
    private String name;
    private String description;
    private Boolean isDelete;
    private EProjectStatus projectStatus;
    private UserDto managerUser;
    private List<UserDto> members;
    private ZonedDateTime endTime;
}
