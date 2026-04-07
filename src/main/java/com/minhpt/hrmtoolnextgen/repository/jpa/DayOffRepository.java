package com.minhpt.hrmtoolnextgen.repository.jpa;


import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DayOffRepository extends JpaRepository<DayOffEntity, Long>, JpaSpecificationExecutor<DayOffEntity> {
    boolean existsByRequestedByIdAndDeleteFalseAndStartTimeLessThanAndEndTimeGreaterThan(
	    Long requestedById,
	    LocalDateTime endTime,
	    LocalDateTime startTime);

    @EntityGraph(attributePaths = {"requestedBy", "requestedBy.userInfo"})
    Optional<DayOffEntity> findByRequestedByIdAndStartTimeAndEndTimeAndDeleteFalse(
	    Long requestedById,
	    LocalDateTime startTime,
	    LocalDateTime endTime);
}