package com.minhpt.hrmtoolnextgen.service.device;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
import com.minhpt.hrmtoolnextgen.dto.request.PaginationRequest;
import com.minhpt.hrmtoolnextgen.dto.response.PaginationResponse;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.DeviceMapping;
import com.minhpt.hrmtoolnextgen.mapping.DeviceUserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.util.CommonUtils;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class DeviceQueryService {

    private static final String DEFAULT_SORT_BY = "createdDate";
    private static final String DEFAULT_DIRECTION = "DESC";

    private final DeviceRepository deviceRepository;
    private final DeviceMapping deviceMapping;
    private final DeviceUserMapping deviceUserMapping;
    private final MessageService messageService;

    @Transactional(readOnly = true)
    public PaginationResponse<DeviceDto> getAllDevices(
            PaginationRequest paginationRequest,
            String name,
            String serialNumber,
            EDeviceType type,
            EDeviceStatus status) {
        log.debug("Listing devices - name: {}, serialNumber: {}, type: {}, status: {}",
                name, serialNumber, type, status);

        Specification<DeviceEntity> spec = buildFilterSpecification(name, serialNumber, type, status);
        Pageable pageable = CommonUtils.buildPageableWithDefaultSort(paginationRequest, DEFAULT_SORT_BY, DEFAULT_DIRECTION);

        Page<DeviceEntity> entityPage = deviceRepository.findAll(spec, pageable);
        Page<DeviceDto> dtoPage = deviceMapping.toDtoPageable(entityPage);

        String actualSortBy = hasText(paginationRequest.getSortBy()) ? paginationRequest.getSortBy() : DEFAULT_SORT_BY;
        String actualDirection = hasText(paginationRequest.getDirection()) ? paginationRequest.getDirection() : DEFAULT_DIRECTION;
        PaginationRequest responseRequest = CommonUtils.buildPaginationRequestForResponse(
                paginationRequest, actualSortBy, actualDirection);

        return CommonUtils.buildPaginationResponse(dtoPage, responseRequest);
    }

    @Transactional(readOnly = true)
    public DeviceDto getDeviceById(Long id) {
        log.debug("Getting device by id: {}", id);
        DeviceEntity entity = deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("device.not.found", id)));
        return deviceMapping.toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<DeviceUserDto> getDeviceUsers(Long deviceId) {
        log.debug("Listing users assigned to device id: {}", deviceId);
        DeviceEntity entity = deviceRepository.findByIdWithUsers(deviceId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("device.not.found", deviceId)));

        List<UserEntity> users = new ArrayList<>(entity.getUsers());
        users.sort(Comparator.comparing(UserEntity::getId));
        return deviceUserMapping.toDtoList(users);
    }

    private Specification<DeviceEntity> buildFilterSpecification(
            String name,
            String serialNumber,
            EDeviceType type,
            EDeviceStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delete"), false));

            if (StringUtils.hasText(name)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (StringUtils.hasText(serialNumber)) {
                predicates.add(cb.like(cb.lower(root.get("serialNumber")), "%" + serialNumber.toLowerCase() + "%"));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
