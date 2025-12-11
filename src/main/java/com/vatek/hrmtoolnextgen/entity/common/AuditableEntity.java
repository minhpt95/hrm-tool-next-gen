package com.vatek.hrmtoolnextgen.entity.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serial;
import java.io.Serializable;
import java.time.ZonedDateTime;

@MappedSuperclass
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditableEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 4017054090299241908L;

    @CreatedDate
    @Column(name = "create_date", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @JsonIgnore
    private ZonedDateTime createdDate;

    @CreatedBy
    @Column(name = "create_by", nullable = false, updatable = false)
    @JsonIgnore
    private Long createdBy;

    @LastModifiedDate
    @Column(name = "last_modified_date", columnDefinition = "TIMESTAMP")
    @JsonIgnore
    private ZonedDateTime lastModifiedDate;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    @JsonIgnore
    private Long lastModifiedBy;
}
