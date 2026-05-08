package com.minhpt.hrmtoolnextgen.repository.jpa;

import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceRepository extends JpaRepository<DeviceEntity, Long>, JpaSpecificationExecutor<DeviceEntity> {

    boolean existsBySerialNumberIgnoreCaseAndDeleteFalse(String serialNumber);

    @Override
    @NonNull
    Page<DeviceEntity> findAll(@NonNull Pageable pageable);

    @Override
    @NonNull
    Page<DeviceEntity> findAll(@Nullable Specification<DeviceEntity> spec, @NonNull Pageable pageable);

    @Override
    @NonNull
    Optional<DeviceEntity> findById(@NonNull Long id);
}
