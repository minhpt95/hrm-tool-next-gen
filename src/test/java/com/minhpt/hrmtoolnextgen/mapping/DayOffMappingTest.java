package com.minhpt.hrmtoolnextgen.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

/**
 * Mapping tests for {@link DayOffMapping} (MapStruct impl {@code DayOffCrudMappingImpl}).
 *
 * <p>The generated implementation is instantiated directly — it has no {@code uses}
 * dependencies, so no injection is needed and no Spring context is started.
 *
 * <p>Covers the renamed fields (id→requestId, title→requestTitle, reason→requestReason),
 * the {@code fullName} qualifier including its null branches, the collection/page
 * overloads inherited from BasePagingMapper, and null inputs.
 */
class DayOffMappingTest {

    private DayOffMapping mapping;

    @BeforeEach
    void setUp() {
        mapping = new DayOffCrudMappingImpl();
    }

    // -------------------------------------------------------------------------
    // toDto — field renames and nested requester flattening
    // -------------------------------------------------------------------------

    @Test
    void toDto_mapsRenamedFieldsAndRequesterDetails() {
        UserEntity requester = Fixtures.buildUser(1L);
        DayOffEntity entity = Fixtures.buildDayOff(7L, requester);
        entity.setId(7L);

        DayOffDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertEquals(7L, dto.getRequestId());
        assertEquals(entity.getTitle(), dto.getRequestTitle());
        assertEquals(entity.getReason(), dto.getRequestReason());
        assertEquals(requester.getEmail(), dto.getRequesterEmail());
        assertEquals("First1 Last1", dto.getRequesterName());
        assertEquals(entity.getStartTime(), dto.getStartTime());
        assertEquals(entity.getEndTime(), dto.getEndTime());
        assertEquals(EDayOffStatus.PENDING, dto.getStatus());
    }

    @Test
    void toDto_nullRequester_leavesRequesterFieldsNull() {
        DayOffEntity entity = Fixtures.buildDayOff(8L, null);
        entity.setId(8L);

        DayOffDto dto = mapping.toDto(entity);

        assertNotNull(dto);
        assertNull(dto.getRequesterName());
        assertNull(dto.getRequesterEmail());
    }

    @Test
    void toDto_null_returnsNull() {
        assertNull(mapping.toDto((DayOffEntity) null));
    }

    // -------------------------------------------------------------------------
    // fullName qualifier — both null branches
    // -------------------------------------------------------------------------

    @Test
    void fullName_nullUser_returnsNull() {
        assertNull(mapping.fullName(null));
    }

    @Test
    void fullName_userWithoutUserInfo_returnsNull() {
        UserEntity user = new UserEntity();
        assertNull(mapping.fullName(user));
    }

    @Test
    void fullName_joinsFirstAndLastName() {
        UserEntity user = Fixtures.buildUser(3L);
        assertEquals("First3 Last3", mapping.fullName(user));
    }

    // -------------------------------------------------------------------------
    // toEntity — reverse renames; associations are ignored by configuration
    // -------------------------------------------------------------------------

    @Test
    void toEntity_mapsRenamedFieldsAndIgnoresAssociations() {
        DayOffDto dto = DayOffDto.builder()
                .requestId(11L)
                .requestTitle("Annual leave")
                .requestReason("Vacation")
                .startTime(LocalDateTime.of(2026, 7, 1, 9, 0))
                .endTime(LocalDateTime.of(2026, 7, 1, 18, 0))
                .status(EDayOffStatus.APPROVED)
                .build();

        DayOffEntity entity = mapping.toEntity(dto);

        assertNotNull(entity);
        assertEquals(11L, entity.getId());
        assertEquals("Annual leave", entity.getTitle());
        assertEquals("Vacation", entity.getReason());
        assertEquals(EDayOffStatus.APPROVED, entity.getStatus());
        // Explicitly ignored in the mapping configuration.
        assertNull(entity.getRequestedBy());
        assertNull(entity.getDecidedBy());
        assertNull(entity.getRequestedAt());
        assertNull(entity.getDecidedAt());
    }

    @Test
    void toEntity_null_returnsNull() {
        assertNull(mapping.toEntity((DayOffDto) null));
    }

    // -------------------------------------------------------------------------
    // Collection and page overloads
    // -------------------------------------------------------------------------

    @Test
    void toDto_list_mapsEveryElement() {
        DayOffEntity a = Fixtures.buildDayOff(1L, Fixtures.buildUser(1L));
        a.setId(1L);
        DayOffEntity b = Fixtures.buildDayOff(2L, Fixtures.buildUser(2L));
        b.setId(2L);

        List<DayOffDto> dtos = mapping.toDto(List.of(a, b));

        assertEquals(2, dtos.size());
        assertEquals(1L, dtos.get(0).getRequestId());
        assertEquals(2L, dtos.get(1).getRequestId());
    }

    @Test
    void toDto_set_mapsEveryElement() {
        DayOffEntity a = Fixtures.buildDayOff(1L, Fixtures.buildUser(1L));
        a.setId(1L);

        Set<DayOffDto> dtos = mapping.toDto(Set.of(a));

        assertEquals(1, dtos.size());
        assertEquals(1L, dtos.iterator().next().getRequestId());
    }

    @Test
    void toEntity_list_mapsEveryElement() {
        List<DayOffEntity> entities = mapping.toEntity(List.of(
                DayOffDto.builder().requestId(4L).requestTitle("t").build()));

        assertEquals(1, entities.size());
        assertEquals(4L, entities.get(0).getId());
    }

    @Test
    void toDtoPageable_mapsPageContentAndPreservesTotal() {
        DayOffEntity entity = Fixtures.buildDayOff(9L, Fixtures.buildUser(9L));
        entity.setId(9L);
        Page<DayOffEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 5), 1);

        Page<DayOffDto> mapped = mapping.toDtoPageable(page);

        assertEquals(1, mapped.getTotalElements());
        assertEquals(9L, mapped.getContent().get(0).getRequestId());
        assertTrue(mapped.getContent().get(0).getRequesterName().startsWith("First9"));
    }

    /**
     * Pins a sharp edge of the generated mapper: the {@code @MappingTarget} update
     * overloads inherited from BaseMapper carry NO {@code @Mapping} annotations, so the
     * renamed fields (id↔requestId, title↔requestTitle, reason↔requestReason) are not
     * transferred. Only the same-named properties — startTime, endTime, status — merge.
     */
    @Test
    void toEntity_ontoExistingTarget_mergesOnlySameNamedProperties() {
        DayOffEntity target = Fixtures.buildDayOff(20L, Fixtures.buildUser(1L));
        target.setId(20L);
        String originalTitle = target.getTitle();
        String originalReason = target.getReason();

        DayOffDto patch = DayOffDto.builder()
                .requestReason("Patched reason")
                .status(EDayOffStatus.APPROVED)
                .startTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();

        DayOffEntity result = mapping.toEntity(patch, target);

        assertSame(target, result);
        // Same-named properties merge.
        assertEquals(EDayOffStatus.APPROVED, target.getStatus());
        assertEquals(LocalDateTime.of(2026, 8, 1, 9, 0), target.getStartTime());
        // Renamed properties do NOT merge on the update overload.
        assertEquals(originalReason, target.getReason());
        assertEquals(originalTitle, target.getTitle());
    }

    @Test
    void toDto_ontoExistingTarget_mergesOnlySameNamedProperties() {
        DayOffEntity source = Fixtures.buildDayOff(21L, Fixtures.buildUser(2L));
        source.setId(21L);
        source.setStatus(EDayOffStatus.REJECTED);
        DayOffDto target = DayOffDto.builder().build();

        DayOffDto result = mapping.toDto(source, target);

        assertSame(target, result);
        assertEquals(EDayOffStatus.REJECTED, target.getStatus());
        assertEquals(source.getStartTime(), target.getStartTime());
        // requestId is a rename, so the update overload leaves it unset.
        assertNull(target.getRequestId());
    }

    @Test
    void updateOverloads_nullSource_returnTargetUnchanged() {
        DayOffEntity entityTarget = Fixtures.buildDayOff(22L, null);
        DayOffDto dtoTarget = DayOffDto.builder().requestTitle("Kept").build();

        assertSame(entityTarget, mapping.toEntity(null, entityTarget));
        assertSame(dtoTarget, mapping.toDto(null, dtoTarget));
        assertEquals("Kept", dtoTarget.getRequestTitle());
    }

    @Test
    void toDto_nullCollections_returnNull() {
        assertNull(mapping.toDto((List<DayOffEntity>) null));
        assertNull(mapping.toDto((Set<DayOffEntity>) null));
        assertNull(mapping.toEntity((List<DayOffDto>) null));
        assertNull(mapping.toEntity((Set<DayOffDto>) null));
    }
}
