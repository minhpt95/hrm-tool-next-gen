package com.minhpt.hrmtoolnextgen.repository.jpa;

import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> , JpaSpecificationExecutor<DeviceEntity> {
}
