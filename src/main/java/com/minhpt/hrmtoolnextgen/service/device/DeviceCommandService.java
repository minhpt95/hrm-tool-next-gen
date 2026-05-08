package com.minhpt.hrmtoolnextgen.service.device;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceDto;
import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
import com.minhpt.hrmtoolnextgen.dto.request.CreateDeviceDto;
import com.minhpt.hrmtoolnextgen.dto.request.UpdateDeviceDto;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.exception.NotFoundException;
import com.minhpt.hrmtoolnextgen.mapping.DeviceMapping;
import com.minhpt.hrmtoolnextgen.mapping.DeviceUserMapping;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class DeviceCommandService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceMapping deviceMapping;
    private final DeviceUserMapping deviceUserMapping;
    private final MessageService messageService;

    @Transactional
    public DeviceDto createDevice(CreateDeviceDto request) {
        log.info("Creating device with serial number: {}", request.getSerialNumber());
        assertSerialNumberAvailable(request.getSerialNumber());

        DeviceEntity entity = deviceMapping.fromCreateRequest(request);
        entity.setDelete(false);

        DeviceEntity saved = deviceRepository.save(entity);
        log.info("Created device with id: {}", saved.getId());
        return deviceMapping.toDto(saved);
    }

    @Transactional
    public DeviceDto updateDevice(Long id, UpdateDeviceDto request) {
        log.info("Updating device with id: {}", id);
        DeviceEntity entity = getDeviceForUpdate(id);

        if (request.getSerialNumber() != null
                && !request.getSerialNumber().equalsIgnoreCase(entity.getSerialNumber())) {
            assertSerialNumberAvailableForUpdate(id, request.getSerialNumber());
        }

        deviceMapping.updateEntityFromRequest(request, entity);

        DeviceEntity updated = deviceRepository.save(entity);
        log.info("Updated device with id: {}", updated.getId());
        return deviceMapping.toDto(updated);
    }

    @Transactional
    public void deleteDevice(Long id) {
        log.info("Deleting device with id: {}", id);
        DeviceEntity entity = deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("device.not.found", id)));

        if (entity.isDelete()) {
            throw new BadRequestException(messageService.getMessage("device.already.deleted"));
        }

        deviceRepository.delete(entity);
        log.info("Deleted device with id: {}", id);
    }

    @Transactional
    public List<DeviceUserDto> manageDeviceUsers(Long deviceId, List<Long> targetUserIds) {
        List<Long> safeTargetIds = targetUserIds == null ? List.of() : targetUserIds.stream().distinct().toList();
        log.info("Syncing device id: {} to {} target user(s)", deviceId, safeTargetIds.size());

        DeviceEntity device = deviceRepository.findByIdWithUsers(deviceId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("device.not.found", deviceId)));
        if (device.isDelete()) {
            throw new BadRequestException(messageService.getMessage("device.cannot.update.deleted"));
        }

        List<UserEntity> targetUsers = safeTargetIds.isEmpty() ? List.of() : resolveUsers(safeTargetIds);
        Set<Long> targetIdSet = targetUsers.stream().map(UserEntity::getId).collect(Collectors.toSet());
        Set<Long> currentIdSet = device.getUsers().stream().map(UserEntity::getId).collect(Collectors.toSet());

        List<UserEntity> usersToAdd = targetUsers.stream()
                .filter(u -> !currentIdSet.contains(u.getId()))
                .toList();
        List<UserEntity> usersToRemove = device.getUsers().stream()
                .filter(u -> !targetIdSet.contains(u.getId()))
                .toList();

        usersToAdd.forEach(u -> u.getDevices().add(device));
        usersToRemove.forEach(u -> u.getDevices().removeIf(d -> d.getId().equals(deviceId)));

        if (!usersToAdd.isEmpty() || !usersToRemove.isEmpty()) {
            List<UserEntity> dirty = new ArrayList<>(usersToAdd.size() + usersToRemove.size());
            dirty.addAll(usersToAdd);
            dirty.addAll(usersToRemove);
            userRepository.saveAll(dirty);
        }

        log.info("Device id: {} synced — added {}, removed {}, total {} user(s)",
                deviceId, usersToAdd.size(), usersToRemove.size(), targetIdSet.size());

        List<UserEntity> resultUsers = new ArrayList<>(targetUsers);
        resultUsers.sort(Comparator.comparing(UserEntity::getId));
        return deviceUserMapping.toDtoList(resultUsers);
    }

    private List<UserEntity> resolveUsers(List<Long> userIds) {
        List<UserEntity> users = userRepository.findAllById(userIds);
        if (users.size() != userIds.size()) {
            throw new BadRequestException(messageService.getMessage("device.user.ids.invalid"));
        }
        return users;
    }

    private void assertSerialNumberAvailable(String serialNumber) {
        if (deviceRepository.existsBySerialNumberIgnoreCaseAndDeleteFalse(serialNumber)) {
            throw new BadRequestException(messageService.getMessage("device.serial.exists", serialNumber));
        }
    }

    private void assertSerialNumberAvailableForUpdate(Long deviceId, String serialNumber) {
        Specification<DeviceEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(cb.lower(root.get("serialNumber")), serialNumber.toLowerCase()));
            predicates.add(cb.equal(root.get("delete"), false));
            predicates.add(cb.notEqual(root.get("id"), deviceId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        if (deviceRepository.count(spec) > 0) {
            throw new BadRequestException(messageService.getMessage("device.serial.exists", serialNumber));
        }
    }

    private DeviceEntity getDeviceForUpdate(Long id) {
        DeviceEntity entity = deviceRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage("device.not.found", id)));

        if (entity.isDelete()) {
            throw new BadRequestException(messageService.getMessage("device.cannot.update.deleted"));
        }
        return entity;
    }
}
