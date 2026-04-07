package com.minhpt.hrmtoolnextgen.service;

import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectQueryService projectQueryService;
    private final ProjectCommandService projectCommandService;

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjects(PaginationRequest paginationRequest) {
        return projectQueryService.getAllProjects(paginationRequest);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getAllProjectsForAdmin(
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        return projectQueryService.getAllProjectsForAdmin(paginationRequest, projectName, projectStatus);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getProjectsByMemberIdWithFilters(
            Long memberId,
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        return projectQueryService.getProjectsByMemberIdWithFilters(memberId, paginationRequest, projectName, projectStatus);
    }

    @Transactional(readOnly = true)
    public PaginationResponse<ProjectDto> getProjectsByManagerIdWithFilters(
            Long managerId,
            PaginationRequest paginationRequest,
            String projectName,
            EProjectStatus projectStatus) {
        return projectQueryService.getProjectsByManagerIdWithFilters(managerId, paginationRequest, projectName, projectStatus);
    }

    @Transactional(readOnly = true)
    public ProjectDto getProjectById(Long id) {
        return projectQueryService.getProjectById(id);
    }

    @Transactional
    public ProjectDto createProject(CreateProjectRequest request) {
        return projectCommandService.createProject(request);
    }

    @Transactional
    public ProjectDto updateProject(Long id, UpdateProjectRequest request) {
        return projectCommandService.updateProject(id, request);
    }

    @Transactional
    public void deleteProject(Long id) {
        projectCommandService.deleteProject(id);
    }
}

