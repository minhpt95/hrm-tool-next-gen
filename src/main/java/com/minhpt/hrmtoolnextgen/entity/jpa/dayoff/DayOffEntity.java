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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "day_off")
@SQLDelete(sql = "UPDATE day_off SET is_delete = TRUE, deleted_date = NOW() WHERE id = ?")
@SQLRestriction("is_delete = FALSE")
@Getter
@Setter
public class DayOffEntity extends IdentityEntity {

    @Column
    private String requestTitle;
    @Column
    private String requestReason;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime startTime;

    @Column(columnDefinition = "TIMESTAMP")
    private LocalDateTime endTime;

    @Column
    @Enumerated(EnumType.STRING)
    private EDayOffStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;
}


