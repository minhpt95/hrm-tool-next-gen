package com.vatek.hrmtoolnextgen.repository.jpa;


import com.vatek.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DayOffRepository extends JpaRepository<DayOffEntity, Long>, JpaSpecificationExecutor<DayOffEntity> {
}