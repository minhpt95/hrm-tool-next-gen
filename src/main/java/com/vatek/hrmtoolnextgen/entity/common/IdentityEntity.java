package com.vatek.hrmtoolnextgen.entity.common;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true,of = "id")
@MappedSuperclass
@Data
public class IdentityEntity extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;
}
