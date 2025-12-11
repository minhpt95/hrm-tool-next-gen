package com.vatek.hrmtoolnextgen.repository.jpa;

import com.vatek.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.vatek.hrmtoolnextgen.enumeration.EProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, JpaSpecificationExecutor<ProjectEntity> {
    @Override
    @EntityGraph(attributePaths = {
            "projectManager",
            "members"
    })
    Page<ProjectEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {
            "projectManager",
            "members"
    })
    List<ProjectEntity> findDistinctByMembers_IdAndDeleteFalseAndProjectStatus(Long memberId, EProjectStatus status);

    @EntityGraph(attributePaths = {
            "projectManager",
            "members"
    })
    List<ProjectEntity> findByProjectManager_IdAndDeleteFalseAndProjectStatus(Long managerId, EProjectStatus status);
}
