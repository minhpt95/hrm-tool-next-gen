package com.vatek.hrmtoolnextgen.entity.jpa.user;

import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.Collection;

@Entity
@Table(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoleEntity extends IdentityEntity {

    @Enumerated(EnumType.STRING)
    private EUserRole userRole;

    @ManyToMany(mappedBy = "roles",fetch = FetchType.LAZY)
    private Collection<UserEntity> users;
}