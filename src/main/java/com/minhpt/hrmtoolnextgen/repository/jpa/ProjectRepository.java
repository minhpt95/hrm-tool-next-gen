package com.minhpt.hrmtoolnextgen.repository.jpa;

import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.projection.ProjectStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long>, JpaSpecificationExecutor<ProjectEntity> {
    @Override
    @EntityGraph(attributePaths = {
            "projectManager"
    })
    @NonNull Page<ProjectEntity> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {
            "projectManager"
    })
    @NonNull Page<ProjectEntity> findAll(@Nullable Specification<ProjectEntity> spec, @NonNull Pageable pageable);

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

    boolean existsByNameIgnoreCaseAndDeleteFalse(String name);

    @Override
    @EntityGraph(attributePaths = {
            "projectManager",
            "members"
    })
    @NonNull Optional<ProjectEntity> findById(@NonNull Long id);

    @Query("""
            SELECT p.projectStatus as status, COUNT(p) as total
            FROM ProjectEntity p
            WHERE p.delete = false
            GROUP BY p.projectStatus
            """)
    List<ProjectStatusCountProjection> countProjectsByStatus();
}
