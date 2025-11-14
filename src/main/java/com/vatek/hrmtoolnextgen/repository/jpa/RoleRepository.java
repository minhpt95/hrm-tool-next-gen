package com.vatek.hrmtoolnextgen.repository.jpa;

import com.vatek.hrmtoolnextgen.entity.jpa.user.RoleEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import javax.management.relation.Role;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, Long>, JpaSpecificationExecutor<RoleEntity> {
    RoleEntity findByUserRole(EUserRole role);

    List<RoleEntity> findByUserRoleIn(List<EUserRole> roles);
}
