package com.minhpt.hrmtoolnextgen.entity.jpa.device;

import com.minhpt.hrmtoolnextgen.entity.common.IdentityEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "devices")
@SQLDelete(sql = "UPDATE devices SET is_delete = true WHERE id = ? AND version = ?")
@SQLRestriction("is_delete = false")
@Getter
@Setter
public class DeviceEntity extends IdentityEntity {

	@Version
	@Column(name = "version", nullable = false)
	private Long version;

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
