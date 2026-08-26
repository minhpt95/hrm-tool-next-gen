package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.minhpt.hrmtoolnextgen.dto.project.ProjectDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateProjectRequest;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateProjectRequest;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link ProjectMapping} (impl {@code ProjectCrudMappingImpl}).
 *
 * <p>The generated impl field-injects the {@code uses = UserMapping.class} collaborator,
 * so the real UserMappingImpl is injected via ReflectionTestUtils rather than starting
 * a Spring context — the assertions then cover the nested manager/member mapping too.
 */
class ProjectMappingTest {

    private ProjectMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new ProjectCrudMappingImpl();
        ReflectionTestUtils.setField(mapping, "userMapping", new UserMappingImpl());
    }

    // -------------------------------------------------------------------------
    // toDto — projectManager becomes managerUser; delete becomes isDelete
    // -------------------------------------------------------------------------

    @Test
    void toDto_mapsManagerMembersAndDeleteFlag() {
        UserEntity manager = Fixtures.buildUser(1L);
        manager.setId(1L);
        UserEntity member = Fixtures.buildUser(2L);
        member.setId(2L);

        ProjectEntity entity = Fixtures.buildProject(10L, manager);
        entity.setId(10L);
        entity.setMembers(List.of(member));
        entity.setDelete(true);

        ProjectDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertEquals(10L, dto.getId());
        assertEquals("Project-10", dto.getName());
        assertEquals(EProjectStatus.RUNNING, dto.getProjectStatus());
        assertEquals(Boolean.TRUE, dto.getIsDelete());
        assertNotNull(dto.getManagerUser());
        assertEquals(1L, dto.getManagerUser().getId());
        assertEquals(1, dto.getMembers().size());
        assertEquals(2L, dto.getMembers().get(0).getId());
    }

    @Test
    void toDto_nullManager_leavesManagerUserNull() {
        ProjectEntity entity = Fixtures.buildProject(11L, null);
        entity.setId(11L);

        ProjectDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertNull(dto.getManagerUser());
    }

    @Test
    void toEntity_mapsDeleteFlagBack() {
        ProjectDto dto = new ProjectDto();
        dto.setId(12L);
        dto.setName("Apollo");
        dto.setProjectStatus(EProjectStatus.RUNNING);
        dto.setIsDelete(true);

        ProjectEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals(12L, entity.getId());
        assertEquals("Apollo", entity.getName());
        assertEquals(true, entity.isDelete());
    }

    // -------------------------------------------------------------------------
    // fromCreateRequest — projectName/projectDescription are renamed; dates ignored
    // -------------------------------------------------------------------------

    @Test
    void fromCreateRequest_renamesNameAndDescriptionAndIgnoresDates() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setProjectName("Gemini");
        request.setProjectDescription("Second programme");
        request.setProjectStatus(EProjectStatus.RUNNING);
        request.setStartDate(LocalDate.of(2026, 3, 1));

        ProjectEntity entity = mapping.fromCreateRequest(request);

        assertNotNull(entity);
        assertEquals("Gemini", entity.getName());
        assertEquals("Second programme", entity.getDescription());
        assertEquals(EProjectStatus.RUNNING, entity.getProjectStatus());
        // Explicitly ignored by the mapping; the service assigns these.
        assertNull(entity.getId());
        assertNull(entity.getStartTime());
        assertNull(entity.getEndTime());
        assertNull(entity.getProjectManager());
    }

    @Test
    void fromCreateRequest_null_returnsNull() {
        assertNull(mapping.fromCreateRequest(null));
    }

    // -------------------------------------------------------------------------
    // updateEntityFromRequest — in-place partial update
    // -------------------------------------------------------------------------

    @Test
    void updateEntityFromRequest_overwritesRenamedFields() {
        ProjectEntity target = Fixtures.buildProject(13L);
        target.setId(13L);
        LocalDate originalStart = target.getStartTime();

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectName("Renamed project");
        request.setProjectDescription("New description");
        request.setProjectStatus(EProjectStatus.DONE);
        request.setStartDate(LocalDate.of(2027, 1, 1));

        mapping.updateEntityFromRequest(request, target);

        assertEquals("Renamed project", target.getName());
        assertEquals("New description", target.getDescription());
        assertEquals(EProjectStatus.DONE, target.getProjectStatus());
        assertEquals(13L, target.getId(), "id is ignored by the mapping");
        assertEquals(originalStart, target.getStartTime(), "startTime is ignored by the mapping");
    }

    @Test
    void updateEntityFromRequest_nullProperties_leaveExistingValues() {
        ProjectEntity target = Fixtures.buildProject(14L);
        String originalName = target.getName();

        UpdateProjectRequest request = new UpdateProjectRequest();
        request.setProjectDescription("Only description");

        mapping.updateEntityFromRequest(request, target);

        assertEquals("Only description", target.getDescription());
        assertEquals(originalName, target.getName());
    }

    // -------------------------------------------------------------------------
    // Collections and paging
    // -------------------------------------------------------------------------

    @Test
    void toDto_list_mapsEveryElement() {
        ProjectEntity a = Fixtures.buildProject(1L);
        a.setId(1L);
        ProjectEntity b = Fixtures.buildProject(2L);
        b.setId(2L);

        List<ProjectDto> dtos = mapping.toDto(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getId());
    }

    @Test
    void toDtoPageable_mapsContent() {
        ProjectEntity entity = Fixtures.buildProject(15L);
        entity.setId(15L);
        Page<ProjectEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);

        Page<ProjectDto> mapped = mapping.toDtoPageable(page);

        assertEquals(1, mapped.getTotalElements());
        assertEquals(15L, mapped.getContent().get(0).getId());
    }

    // -------------------------------------------------------------------------
    // @MappingTarget update overloads and Set overloads
    // -------------------------------------------------------------------------

    @Test
    void toEntity_ontoExistingTarget_mergesNonNullFields() {
        ProjectEntity target = Fixtures.buildProject(20L);
        target.setId(20L);
        String originalName = target.getName();

        ProjectDto patch = new ProjectDto();
        patch.setDescription("Patched description");

        ProjectEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        assertEquals("Patched description", target.getDescription());
        assertEquals(originalName, target.getName(), "null source fields are ignored");
    }

    @Test
    void toDto_ontoExistingTarget_mergesFields() {
        ProjectEntity source = Fixtures.buildProject(21L);
        source.setId(21L);
        ProjectDto target = new ProjectDto();

        ProjectDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals(21L, target.getId());
    }

    @Test
    void updateOverloads_nullSource_returnTargetUnchanged() {
        ProjectEntity entityTarget = Fixtures.buildProject(22L);
        ProjectDto dtoTarget = new ProjectDto();

        assertSame(entityTarget, mapping.toEntity(null, entityTarget));
        assertSame(dtoTarget, mapping.toDto(null, dtoTarget));
    }

    @Test
    void setOverloads_mapEveryElement() {
        ProjectEntity project = Fixtures.buildProject(23L);
        project.setId(23L);

        Set<ProjectDto> dtos = mapping.toDto(Set.of(project));
        assertEquals(1, dtos.size());
        assertEquals(23L, dtos.iterator().next().getId());

        ProjectDto dto = new ProjectDto();
        dto.setId(24L);
        Set<ProjectEntity> entities = mapping.toEntity(Set.of(dto));
        assertEquals(1, entities.size());

        assertNull(mapping.toDto((Set<ProjectEntity>) null));
        assertNull(mapping.toEntity((Set<ProjectDto>) null));
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapping.toDto((ProjectEntity) null));
        assertNull(mapping.toEntity((ProjectDto) null));
        assertNull(mapping.toDto((List<ProjectEntity>) null));
        assertNull(mapping.toEntity((List<ProjectDto>) null));
    }
}
