package com.vatek.hrmtoolnextgen.entity.jpa.dayoff;

import com.vatek.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.vatek.hrmtoolnextgen.enumeration.EDayOffType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "day_off")
@Getter
@Setter
public class DayOffEntity {

    @Column
    private String requestTitle;
    @Column
    private String requestReason;

    @EmbeddedId
    private DayOffEntityId dayoffEntityId;
    @Column
    @Enumerated(EnumType.STRING)
    private EDayOffStatus status;


    @Embeddable
    @Getter
    @Setter
    public static class DayOffEntityId implements Serializable {
        @Serial
        private static final long serialVersionUID = -6491357190187436940L;

        @Column(columnDefinition = "DATE")
        private Instant dateOff;

        @Column
        @Enumerated(EnumType.STRING)
        private EDayOffType type;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id")
        private UserEntity user;
    }
}


