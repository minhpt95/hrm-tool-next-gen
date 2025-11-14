package com.vatek.hrmtoolnextgen.dto.project;

import com.vatek.hrmtoolnextgen.dto.user.UserExcelDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProjectExcelDto {
    private String name;
    private List<UserExcelDto> members = new ArrayList<>();
}
