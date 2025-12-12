package com.vatek.hrmtoolnextgen.repository.jpa;

import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmailOrUserInfo_IdentityCard(String email, String identityCard);

    Optional<UserEntity> findByEmail(String email);

    int countAllByEmail(String email);

    @EntityGraph(attributePaths = {"userInfo", "roles"})
    Page<UserEntity> findAll(Pageable pageable);

    Optional<UserEntity> findAllByEmail(String email);

    UserEntity findUserEntityByEmail(String email);

    Collection<UserEntity> findUserEntitiesByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"workingProject"})
    Optional<UserEntity> findById(Long id);

    boolean existsByWorkingProjectIdAndId(Long projectId, Long userId);
}
