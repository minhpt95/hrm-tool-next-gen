package com.minhpt.hrmtoolnextgen.mapping;

import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        implementationName = "DeviceCrudMappingImpl"
)
public interface DeviceMapping extends BasePagingMapper<DeviceDto, DeviceEntity> {

    @Override
    @Mapping(target = "delete", source = "isDelete")
    @Mapping(target = "users", ignore = true)
    DeviceEntity toEntity(DeviceDto dto);

    @Override
    @Mapping(target = "isDelete", source = "delete")
    DeviceDto toDto(DeviceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "users", ignore = true)
    DeviceEntity fromCreateRequest(CreateDeviceDto request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    @Mapping(target = "users", ignore = true)
    void updateEntityFromRequest(UpdateDeviceDto request, @MappingTarget DeviceEntity entity);
}
