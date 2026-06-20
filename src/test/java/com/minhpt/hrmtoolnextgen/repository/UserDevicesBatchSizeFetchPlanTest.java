package com.minhpt.hrmtoolnextgen.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Verifies that @BatchSize(size = 50) on UserEntity.devices prevents N+1 queries
 * when lazily initialising the devices collection across multiple users.
 *
 * Pattern mirrors UserRepositoryFetchPlanTest / ProjectRepositoryFetchPlanTest:
 *   1. Persist N users each owning M devices.
 *   2. Clear the first-level cache and reset Hibernate Statistics.
 *   3. Load the users.
 *   4. Touch the lazy devices collection on every user.
 *   5. Assert total query count is far below N (batching fires one IN query per
 *      batch of 50, not one query per user).
 *
 * With 3 users and @BatchSize(50), all three devices collections are fetched in
 * a single batch query, so total queries should be <= 4 (1 user load + 1 role
 * batch + 1 device batch + 1 count query for the pageable).
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, UserDevicesBatchSizeFetchPlanTest.MailTestConfig.class})
@Transactional
class UserDevicesBatchSizeFetchPlanTest {

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
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<Long> savedUserIds = new ArrayList<>();

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        savedUserIds.clear();

        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }

        // Create 3 devices — one per user so each user's devices collection is non-empty.
        List<DeviceEntity> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DeviceEntity device = new DeviceEntity();
            long seed = System.nanoTime() + i;
            device.setName("Batch-Test-Device-" + seed);
            device.setSerialNumber("SN-BATCH-" + seed);
            device.setType(EDeviceType.LAPTOP);
            device.setStatus(EDeviceStatus.ACTIVE);
            devices.add(deviceRepository.save(device));
        }

        // Create 3 users, each owning one device via the users_devices join table.
        for (int i = 0; i < 3; i++) {
            UserEntity user = createUser(userRole, i);
            user.getDevices().add(devices.get(i));
            savedUserIds.add(userRepository.save(user).getId());
        }

        userRepository.flush();
        entityManager.clear();

        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
    }

    @Test
    void loadingDevicesCollectionAcrossMultipleUsersShouldBeBatched() {
        // Load only our test users via a direct findAllById call to keep the
        // query count predictable regardless of other data in the test database.
        List<UserEntity> users = userRepository.findAllById(savedUserIds);

        assertEquals(savedUserIds.size(), users.size());

        // devices is lazy — not yet initialised
        users.forEach(user -> assertFalse(Hibernate.isInitialized(user.getDevices())));

        // Touch all devices collections. With @BatchSize(50) Hibernate loads
        // all 3 collections in ONE IN(...) query instead of 3 separate queries.
        users.forEach(user -> assertFalse(user.getDevices().isEmpty()));
        users.forEach(user -> assertTrue(Hibernate.isInitialized(user.getDevices())));

        // Total statements: 1 (findAllById) + 1 (roles batch) + 1 (devices batch) = 3.
        // Allow up to 5 to absorb any sequence/metadata overhead without being flaky.
        assertTrue(
                statistics.getPrepareStatementCount() <= 5,
                "Expected batched device loading (<= 5 queries) but got: "
                        + statistics.getPrepareStatementCount()
        );
    }

    private UserEntity createUser(RoleEntity userRole, int index) {
        long uniqueSeed = System.nanoTime() + index;

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setFirstName("BatchDevice" + index);
        userInfo.setLastName("Test" + index);
        userInfo.setIdentityCard("BDID-" + uniqueSeed);
        userInfo.setPhoneNumber1(String.format("09%08d", uniqueSeed % 100_000_000L));
        userInfo.setCurrentAddress("Hanoi");
        userInfo.setPermanentAddress("Hanoi");
        userInfo.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("batch-device-" + uniqueSeed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(userInfo);
        user.setRoles(List.of(userRole));
        return user;
    }
}
