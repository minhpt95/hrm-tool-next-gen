package com.minhpt.hrmtoolnextgen.entity.jpa.role;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "roles")
@SQLDelete(sql = "UPDATE roles SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RoleEntity extends IdentityEntity {

    @Enumerated(EnumType.STRING)
    private EUserRole userRole;

    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private List<UserEntity> users = new ArrayList<>();
}