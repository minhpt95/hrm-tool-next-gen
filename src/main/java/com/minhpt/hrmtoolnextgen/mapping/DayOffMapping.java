package com.minhpt.hrmtoolnextgen.mapping;

import com.minhpt.hrmtoolnextgen.dto.dayoff.DayOffDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.mapping.common.BasePagingMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        implementationName = "DayOffCrudMappingImpl"
)
public interface DayOffMapping extends BasePagingMapper<DayOffDto, DayOffEntity> {

    @Override
    @Mapping(target = "requestId", source = "id")
    @Mapping(target = "requesterName", source = "requestedBy", qualifiedByName = "fullName")
    @Mapping(target = "requesterEmail", source = "requestedBy.email")
    @Mapping(target = "requestTitle", source = "title")
    @Mapping(target = "requestReason", source = "reason")
    DayOffDto toDto(DayOffEntity entity);

    @Override
    @Mapping(target = "id", source = "requestId")
    @Mapping(target = "title", source = "requestTitle")
    @Mapping(target = "reason", source = "requestReason")
    @Mapping(target = "requestedBy", ignore = true)
    @Mapping(target = "decidedBy", ignore = true)
    @Mapping(target = "requestedAt", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "deletedDate", ignore = true)
    DayOffEntity toEntity(DayOffDto dto);

    @Named("fullName")
    default String fullName(UserEntity user) {
        if (user == null) {
            return null;
        }
        UserInfoEntity info = user.getUserInfo();
        if (info == null) {
            return null;
        }
        return info.getFirstName() + " " + info.getLastName();
    }
}
