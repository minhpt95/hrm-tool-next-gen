package com.minhpt.hrmtoolnextgen.entity.jpa.request;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.ERequestDeviceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_devices")
@SQLDelete(sql = "UPDATE request_devices SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
@Getter
@Setter
public class RequestDeviceEntity extends IdentityEntity {

    @Column
    private String reason;

    @Enumerated(EnumType.STRING)
    private ERequestDeviceStatus status;

    @Column(columnDefinition = "TIMESTAMP",name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(columnDefinition = "TIMESTAMP",name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserEntity decidedBy;
}
