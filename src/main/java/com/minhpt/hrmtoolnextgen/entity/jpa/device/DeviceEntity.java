package com.minhpt.hrmtoolnextgen.entity.jpa.device;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "devices")
@SQLDelete(sql = "UPDATE devices SET is_delete = true WHERE id = ?")
@SQLRestriction("is_delete = false")
@Getter
@Setter
public class DeviceEntity extends IdentityEntity {

    @Column
    private String name;

    @Column
    private String description;

    @Column(name = "serial_number", unique = true, nullable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    private EDeviceType type;

    @Enumerated(EnumType.STRING)
    private EDeviceStatus status;

    @ManyToMany(mappedBy = "devices", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();
}
