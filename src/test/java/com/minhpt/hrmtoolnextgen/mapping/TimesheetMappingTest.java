package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.minhpt.hrmtoolnextgen.dto.timesheet.TimesheetDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link TimesheetMapping} (impl {@code TimesheetMappingImpl}).
 *
 * <p>The generated impl field-injects UserMapping and ProjectMapping; the real
 * implementations are supplied so nested user/project mapping is exercised for real.
 * The headline behaviour here is the {@code type} ↔ {@code timesheetType} rename.
 */
class TimesheetMappingTest {

    private TimesheetMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new TimesheetMappingImpl();
        UserMapping userMapping = new UserMappingImpl();
        ProjectMapping projectMapping = new ProjectCrudMappingImpl();
        ReflectionTestUtils.setField(projectMapping, "userMapping", userMapping);
        ReflectionTestUtils.setField(mapping, "userMapping", userMapping);
        ReflectionTestUtils.setField(mapping, "projectMapping", projectMapping);
    }

    // -------------------------------------------------------------------------
    // type <-> timesheetType rename
    // -------------------------------------------------------------------------

    @Test
    void toDto_renamesTypeToTimesheetType() {
        UserEntity owner = Fixtures.buildUser(1L);
        owner.setId(1L);
        ProjectEntity project = Fixtures.buildProject(2L);
        project.setId(2L);

        TimesheetEntity entity = Fixtures.buildTimesheet(3L, owner, project);
        entity.setId(3L);
        entity.setType(ETimesheetType.OVERTIME);

        TimesheetDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertEquals(ETimesheetType.OVERTIME, dto.getTimesheetType());
        assertEquals(entity.getTitle(), dto.getTitle());
        assertEquals(entity.getDescription(), dto.getDescription());
        assertEquals(entity.getWorkingHours(), dto.getWorkingHours());
        assertEquals(entity.getWorkingDay(), dto.getWorkingDay());
        assertEquals(ETimesheetStatus.PENDING, dto.getStatus());
        assertNotNull(dto.getUserEntity());
        assertEquals(1L, dto.getUserEntity().getId());
        assertNotNull(dto.getProjectEntity());
        assertEquals(2L, dto.getProjectEntity().getId());
    }

    @Test
    void toEntity_renamesTimesheetTypeToType() {
        TimesheetDto dto = new TimesheetDto();
        dto.setTitle("Sprint work");
        dto.setDescription("Implementation");
        dto.setWorkingHours(LocalTime.of(7, 30));
        dto.setWorkingDay(LocalDate.of(2026, 5, 4));
        dto.setTimesheetType(ETimesheetType.NORMAL);
        dto.setStatus(ETimesheetStatus.APPROVED);

        TimesheetEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals(ETimesheetType.NORMAL, entity.getType());
        assertEquals("Sprint work", entity.getTitle());
        assertEquals(LocalTime.of(7, 30), entity.getWorkingHours());
        assertEquals(LocalDate.of(2026, 5, 4), entity.getWorkingDay());
        assertEquals(ETimesheetStatus.APPROVED, entity.getStatus());
    }

    @Test
    void toDto_withoutAssociations_leavesNestedDtosNull() {
        TimesheetEntity entity = Fixtures.buildTimesheet(4L);
        entity.setId(4L);

        TimesheetDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertNull(dto.getUserEntity());
        assertNull(dto.getProjectEntity());
    }

    // -------------------------------------------------------------------------
    // Collections and paging
    // -------------------------------------------------------------------------

    @Test
    void toDto_list_mapsEveryElement() {
        TimesheetEntity a = Fixtures.buildTimesheet(1L);
        a.setId(1L);
        a.setType(ETimesheetType.NORMAL);
        TimesheetEntity b = Fixtures.buildTimesheet(2L);
        b.setId(2L);
        b.setType(ETimesheetType.OVERTIME);

        List<TimesheetDto> dtos = mapping.toDto(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(ETimesheetType.NORMAL, dtos.get(0).getTimesheetType());
        assertEquals(ETimesheetType.OVERTIME, dtos.get(1).getTimesheetType());
    }

    @Test
    void toDtoPageable_mapsContent() {
        TimesheetEntity entity = Fixtures.buildTimesheet(5L);
        entity.setId(5L);
        Page<TimesheetEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);

        Page<TimesheetDto> mapped = mapping.toDtoPageable(page);

        assertEquals(1, mapped.getTotalElements());
        assertEquals(entity.getTitle(), mapped.getContent().get(0).getTitle());
    }

    // -------------------------------------------------------------------------
    // @MappingTarget update overloads and Set overloads
    // -------------------------------------------------------------------------

    @Test
    void toEntity_ontoExistingTarget_mergesFields() {
        TimesheetEntity target = Fixtures.buildTimesheet(20L);
        target.setId(20L);

        TimesheetDto patch = new TimesheetDto();
        patch.setDescription("Patched description");

        TimesheetEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        assertEquals("Patched description", target.getDescription());
    }

    /**
     * The {@code @MappingTarget} update overload inherits no {@code @Mapping} annotations,
     * so the type↔timesheetType rename is not applied — only same-named properties merge.
     */
    @Test
    void toDto_ontoExistingTarget_mergesOnlySameNamedProperties() {
        TimesheetEntity source = Fixtures.buildTimesheet(21L);
        source.setId(21L);
        source.setType(ETimesheetType.OVERTIME);
        TimesheetDto target = new TimesheetDto();

        TimesheetDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals(source.getTitle(), target.getTitle());
        assertEquals(source.getWorkingDay(), target.getWorkingDay());
        // The renamed property is not transferred by the update overload.
        assertNull(target.getTimesheetType());
    }

    @Test
    void updateOverloads_nullSource_returnTargetUnchanged() {
        TimesheetEntity entityTarget = Fixtures.buildTimesheet(22L);
        TimesheetDto dtoTarget = new TimesheetDto();

        assertSame(entityTarget, mapping.toEntity(null, entityTarget));
        assertSame(dtoTarget, mapping.toDto(null, dtoTarget));
    }

    @Test
    void setOverloads_mapEveryElement() {
        TimesheetEntity entity = Fixtures.buildTimesheet(23L);
        entity.setId(23L);
        entity.setType(ETimesheetType.NORMAL);

        Set<TimesheetDto> dtos = mapping.toDto(Set.of(entity));
        assertEquals(1, dtos.size());
        assertEquals(ETimesheetType.NORMAL, dtos.iterator().next().getTimesheetType());

        TimesheetDto dto = new TimesheetDto();
        dto.setTimesheetType(ETimesheetType.OVERTIME);
        Set<TimesheetEntity> entities = mapping.toEntity(Set.of(dto));
        assertEquals(1, entities.size());
        assertEquals(ETimesheetType.OVERTIME, entities.iterator().next().getType());

        assertNull(mapping.toDto((Set<TimesheetEntity>) null));
        assertNull(mapping.toEntity((Set<TimesheetDto>) null));
    }

    @Test
    void nullInputs_returnNull() {
        assertNull(mapping.toDto((TimesheetEntity) null));
        assertNull(mapping.toEntity((TimesheetDto) null));
        assertNull(mapping.toDto((List<TimesheetEntity>) null));
        assertNull(mapping.toEntity((List<TimesheetDto>) null));
    }
}
