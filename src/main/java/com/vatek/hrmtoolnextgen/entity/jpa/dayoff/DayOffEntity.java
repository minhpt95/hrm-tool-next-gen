package com.vatek.hrmtoolnextgen.entity.jpa.dayoff;

import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import jakarta.persistence.*;
import com.vatek.hrmtoolnextgen.entity.common.IdentityEntity;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "day_off")
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


