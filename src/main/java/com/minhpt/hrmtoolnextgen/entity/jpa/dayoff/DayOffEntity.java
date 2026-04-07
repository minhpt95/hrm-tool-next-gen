package com.minhpt.hrmtoolnextgen.entity.jpa.dayoff;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "day_offs", indexes = {
        @Index(name = "idx_day_offs_requested_status", columnList = "requested_by, status"),
        @Index(name = "idx_day_offs_requested_range", columnList = "requested_by, start_time, end_time"),
        @Index(name = "idx_day_offs_delete_requested_at", columnList = "is_delete, requested_at")
})
@SQLDelete(sql = "UPDATE day_offs SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
@Getter
@Setter
public class DayOffEntity extends IdentityEntity {

    @Column
    private String title;
    @Column
    private String reason;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime startTime;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime endTime;

    @Column
    @Enumerated(EnumType.STRING)
    private EDayOffStatus status;

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


