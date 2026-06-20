package com.minhpt.hrmtoolnextgen.service.device;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import com.minhpt.hrmtoolnextgen.HrmToolNextGenApplication;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.exception.BadRequestException;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;

/**
 * Focused integration test for DeviceCommandService.resolveUsers (called via
 * manageDeviceUsers). Verifies that when one or more target user IDs do not
 * exist in the database, a BadRequestException is thrown whose message NAMES
 * the invalid IDs — proving the fix that surfaces unknown IDs in the error.
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, DeviceCommandServiceResolveUsersTest.MailTestConfig.class})
@Transactional
class DeviceCommandServiceResolveUsersTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @SuppressWarnings("unused")
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @Autowired
    private DeviceCommandService deviceCommandService;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Long deviceId;

    @BeforeEach
    void setUp() {
        DeviceEntity device = new DeviceEntity();
        long seed = System.nanoTime();
        device.setName("ResolveUsers-Test-Device-" + seed);
        device.setSerialNumber("SN-RUT-" + seed);
        device.setType(EDeviceType.LAPTOP);
        device.setStatus(EDeviceStatus.ACTIVE);
        deviceId = deviceRepository.save(device).getId();
    }

    /**
     * All IDs are non-existent: the exception message must contain the invalid ID.
     */
    @Test
    void manageDeviceUsers_allInvalidIds_throwsBadRequestNamingInvalidId() {
        long nonExistentId = 999_999L;

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> deviceCommandService.manageDeviceUsers(deviceId, List.of(nonExistentId)));

        String msg = ex.getMessage();
        assertTrue(msg.contains("999999"),
                "Exception message should contain the invalid ID '999999', but was: " + msg);
        assertTrue(msg.contains("invalid"),
                "Exception message should contain the word 'invalid', but was: " + msg);
    }

    /**
     * Mix of one valid and one non-existent ID: the exception message must name
     * the invalid ID and must NOT mention the valid one.
     */
    @Test
    void manageDeviceUsers_mixOfValidAndInvalidIds_messageNamesOnlyInvalidId() {
        // Persist one real user so we have a valid ID.
        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }
        UserEntity validUser = persistUser(userRole);
        Long validId = validUser.getId();

        long invalidId = 888_999L;

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> deviceCommandService.manageDeviceUsers(deviceId, List.of(validId, invalidId)));

        String msg = ex.getMessage();
        assertTrue(msg.contains("888999"),
                "Exception message should contain the invalid ID '888999', but was: " + msg);
        assertTrue(msg.contains("invalid"),
                "Exception message should contain the word 'invalid', but was: " + msg);
        assertTrue(msg.endsWith(": 888999"),
                "Message should list only the invalid ID 888999 after the separator, but was: " + msg);
    }

    private UserEntity persistUser(RoleEntity role) {
        long seed = System.nanoTime();

        UserInfoEntity info = new UserInfoEntity();
        info.setFirstName("Resolve");
        info.setLastName("UsersTest");
        info.setIdentityCard("RUT-" + seed);
        info.setPhoneNumber1(String.format("09%08d", seed % 100_000_000L));
        info.setCurrentAddress("Hanoi");
        info.setPermanentAddress("Hanoi");
        info.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("resolve-users-test-" + seed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(info);
        user.setRoles(List.of(role));
        return userRepository.save(user);
    }
}
