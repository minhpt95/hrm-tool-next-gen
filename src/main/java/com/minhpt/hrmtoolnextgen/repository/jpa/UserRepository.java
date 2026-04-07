package com.minhpt.hrmtoolnextgen.repository.jpa;

import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmailOrUserInfo_IdentityCard(String email, String identityCard);

    @EntityGraph(attributePaths = {"userInfo", "roles"})
    Optional<UserEntity> findByEmail(String email);

    int countAllByEmail(String email);

    @Override
    @EntityGraph(attributePaths = {"userInfo"})
    @NonNull Page<UserEntity> findAll(@NonNull Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"userInfo"})
    @NonNull Page<UserEntity> findAll(@Nullable org.springframework.data.jpa.domain.Specification<UserEntity> spec,
                                      @NonNull Pageable pageable);

    Optional<UserEntity> findAllByEmail(String email);

    UserEntity findUserEntityByEmail(String email);

    Collection<UserEntity> findUserEntitiesByIdIn(List<Long> ids);

    @Override
    @EntityGraph(attributePaths = {"userInfo", "roles", "workingProject"})
    @NonNull Optional<UserEntity> findById(@NonNull Long id);

    boolean existsByWorkingProjectIdAndId(Long projectId, Long userId);

    long countByActiveTrueAndDeleteFalse();
}
