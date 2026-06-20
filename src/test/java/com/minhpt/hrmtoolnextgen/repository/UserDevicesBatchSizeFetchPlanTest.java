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
import org.junit.jupiter.api.AfterEach;
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
 *   1. Persist USER_COUNT (10) users each owning 1 device.
 *   2. Clear the first-level cache and reset Hibernate Statistics.
 *   3. Load the users via findAllById.
 *   4. Snapshot Statistics.getPrepareStatementCount() before touching collections.
 *   5. Touch the lazy devices collection on every user.
 *   6. Assert the prepared-statement DELTA == 1 (one IN-batch, not 10 selects).
 *
 * WHY getPrepareStatementCount() DELTA rather than getCollectionFetchCount():
 *   Hibernate's batch loader for @BatchSize on the owning side of a @ManyToMany
 *   records SQL under the prepare-statement counter but not always under the
 *   collection-fetch counter. getPrepareStatementCount() is incremented for
 *   every JDBC statement regardless of loader path, making it the reliable
 *   discriminator.
 *
 * WHY THE ASSERTION DISCRIMINATES THE FIX:
 *   With @BatchSize(50) and 10 users all fitting in one batch: delta == 1.
 *   Without @BatchSize: Hibernate fires one SELECT per user => delta == 10.
 *   assertEquals(1, delta) therefore FAILS definitively when @BatchSize is
 *   removed (10 != 1, with no overlap between the two possible values).
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, UserDevicesBatchSizeFetchPlanTest.MailTestConfig.class})
@Transactional
class UserDevicesBatchSizeFetchPlanTest {

    static final int USER_COUNT = 10;

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
    private boolean priorStats;

    @BeforeEach
    void setUp() {
        savedUserIds.clear();

        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }

        // Create USER_COUNT devices — one per user so every user's devices
        // collection is non-empty (an empty collection would not trigger a
        // batch SELECT and would make the assertion trivially true).
        List<DeviceEntity> devices = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            DeviceEntity device = new DeviceEntity();
            long seed = System.nanoTime() + i;
            device.setName("Batch-Test-Device-" + seed);
            device.setSerialNumber("SN-BATCH-" + seed);
            device.setType(EDeviceType.LAPTOP);
            device.setStatus(EDeviceStatus.ACTIVE);
            devices.add(deviceRepository.save(device));
        }

        // Create USER_COUNT users, each owning exactly one device via the
        // users_devices join table (owning side is UserEntity.devices).
        for (int i = 0; i < USER_COUNT; i++) {
            UserEntity user = createUser(userRole, i);
            user.getDevices().add(devices.get(i));
            savedUserIds.add(userRepository.save(user).getId());
        }

        userRepository.flush();
        entityManager.clear();

        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        priorStats = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @AfterEach
    void tearDown() {
        statistics.clear();
        statistics.setStatisticsEnabled(priorStats);
    }

    @Test
    void loadingDevicesCollectionAcrossMultipleUsersShouldBeBatched() {
        // --- Load phase ---
        List<UserEntity> users = userRepository.findAllById(savedUserIds);

        assertEquals(USER_COUNT, users.size());

        // devices is lazy — must NOT be initialised yet.
        users.forEach(user -> assertFalse(Hibernate.isInitialized(user.getDevices()),
                "Expected devices collection to be lazy before touch"));

        // --- Snapshot prepared-statement count immediately before the touch loop ---
        // We use getPrepareStatementCount() delta rather than getCollectionFetchCount()
        // because Hibernate's batch loader for @BatchSize on the *owning* side of a
        // @ManyToMany uses a secondary-select path that Hibernate Statistics records
        // under the prepare-statement counter rather than the collection-fetch counter.
        // getPrepareStatementCount() is always incremented for every actual SQL
        // statement sent to the JDBC driver, so it is the most reliable discriminator.
        long stmtsBefore = statistics.getPrepareStatementCount();

        // --- Touch phase: access each user's devices collection ---
        // With @BatchSize(50): Hibernate groups all USER_COUNT (10) uninitialized
        // collections into ONE IN(...) batch query => delta == 1.
        // Without @BatchSize: Hibernate fires one SELECT per user => delta == 10.
        users.forEach(user -> assertFalse(user.getDevices().isEmpty(),
                "Expected every test user to have at least one device"));
        users.forEach(user -> assertTrue(Hibernate.isInitialized(user.getDevices()),
                "Expected devices collection to be initialized after touch"));

        long stmtsAfter = statistics.getPrepareStatementCount();
        long stmtsDelta = stmtsAfter - stmtsBefore;

        // EXACT assertion: with @BatchSize(50) and USER_COUNT (10) users, all
        // collections fit in a single IN-batch => exactly 1 extra statement.
        // Without @BatchSize this would be USER_COUNT (10) — the assertion fails
        // clearly because 10 > 1 and there is no overlap between the two values.
        assertEquals(1, stmtsDelta,
                "Expected exactly 1 batched SQL statement to load all " + USER_COUNT
                        + " users' devices collections (got delta=" + stmtsDelta
                        + ", stmtsBefore=" + stmtsBefore + ", stmtsAfter=" + stmtsAfter
                        + "). Without @BatchSize(50) this would be " + USER_COUNT + ".");
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
