package com.minhpt.hrmtoolnextgen.service.device;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.minhpt.hrmtoolnextgen.dto.device.DeviceUserDto;
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
 * Service-level integration test that verifies two things at the real call site
 * of DeviceCommandService.manageDeviceUsers:
 *
 * (a) CORRECTNESS: after calling manageDeviceUsers the returned list matches the
 *     requested target user set exactly.
 *
 * (b) BATCH N+1 PROOF: when several users are REMOVED from a device their
 *     individual UserEntity.devices collections must be lazily initialised (so
 *     the removeIf call can execute). Without @BatchSize(50) each removal fires
 *     its own SELECT — N queries for N removed users. With @BatchSize(50) all N
 *     collections are loaded in a single IN(...) batch query.
 *
 * WHY THE ARITHMETIC PROVES THE FIX:
 *   We assign INITIAL_ASSIGNED_COUNT (8) users to the device, then call
 *   manageDeviceUsers with a target list that keeps only KEEP_COUNT (3) of them.
 *   That forces REMOVE_COUNT (5) removals, each touching the removed user's
 *   devices collection. Statistics are cleared right before the call so only
 *   that call's statements count.
 *
 *   Total statements with @BatchSize(50): ~11
 *     (findByIdWithUsers + resolveUsers findAllById + role/userInfo batches
 *      + 1 devices IN-batch for all 5 removed users + saveAll)
 *   Total statements without @BatchSize: ~15
 *     (same, but the 1 devices IN-batch becomes REMOVE_COUNT=5 individual SELECTs,
 *      adding 4 extra statements)
 *
 *   Bound of 13: above the batched total (~11), below the unbatched total (~15).
 *   The gap is 4 statements. Removing @BatchSize yields ~15 > 13 => FAIL.
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, DeviceCommandServiceManageUsersBatchTest.MailTestConfig.class})
@Transactional
class DeviceCommandServiceManageUsersBatchTest {

    static final int INITIAL_ASSIGNED_COUNT = 8;
    static final int KEEP_COUNT = 3;
    static final int REMOVE_COUNT = INITIAL_ASSIGNED_COUNT - KEEP_COUNT; // 5

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
    private UserRepository userRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long deviceId;
    private List<Long> allUserIds;
    private Statistics statistics;
    private boolean priorStats;

    @BeforeEach
    void setUp() {
        allUserIds = new ArrayList<>();

        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }

        // Persist the device under test.
        DeviceEntity device = new DeviceEntity();
        long deviceSeed = System.nanoTime();
        device.setName("Manage-Users-Test-Device-" + deviceSeed);
        device.setSerialNumber("SN-MUT-" + deviceSeed);
        device.setType(EDeviceType.LAPTOP);
        device.setStatus(EDeviceStatus.ACTIVE);
        device = deviceRepository.save(device);
        deviceId = device.getId();

        // Persist INITIAL_ASSIGNED_COUNT users. Assign them all to the device
        // through the OWNING side (UserEntity.devices) so the join table rows
        // are present before the act.
        for (int i = 0; i < INITIAL_ASSIGNED_COUNT; i++) {
            UserEntity user = createUser(userRole, i);
            user.getDevices().add(device);
            allUserIds.add(userRepository.save(user).getId());
        }

        userRepository.flush();
        // Clear the persistence context so nothing is cached — the service
        // must reload everything from the database, making Statistics meaningful.
        entityManager.clear();

        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        priorStats = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        // Do NOT clear statistics here; clear right before the act so the
        // snapshot covers only manageDeviceUsers.
    }

    @AfterEach
    void tearDown() {
        statistics.clear();
        statistics.setStatisticsEnabled(priorStats);
    }

    @Test
    void manageDeviceUsersShouldRemoveUsersBatchedAndReturnCorrectAssignment() {
        // --- Arrange: choose which users to keep (first KEEP_COUNT) ---
        // The remaining REMOVE_COUNT users will be removed by manageDeviceUsers.
        List<Long> targetUserIds = allUserIds.subList(0, KEEP_COUNT);
        Set<Long> targetIdSet = Set.copyOf(targetUserIds);

        // --- Snapshot statistics immediately before the call ---
        // Only queries emitted inside manageDeviceUsers will be counted.
        statistics.clear();

        // --- Act ---
        List<DeviceUserDto> result = deviceCommandService.manageDeviceUsers(deviceId, targetUserIds);

        // --- Assert (a): correctness ---
        // The returned list must contain exactly the KEEP_COUNT requested users.
        assertEquals(KEEP_COUNT, result.size(),
                "Expected manageDeviceUsers to return exactly " + KEEP_COUNT + " users");

        Set<Long> returnedIds = result.stream()
                .map(DeviceUserDto::getId)
                .collect(Collectors.toSet());
        assertEquals(targetIdSet, returnedIds,
                "Returned user IDs must match the requested target set");

        // --- Assert (b): batch proof via prepared-statement count delta ---
        // We measure the total SQL statements emitted by the entire manageDeviceUsers
        // call. With @BatchSize(50) the REMOVE_COUNT (5) removed users' devices
        // collections are all loaded in ONE batched IN(...) query. Without @BatchSize
        // each of the 5 removed users triggers its own SELECT (+4 extra statements).
        //
        // Observed statement budget with @BatchSize(50) in place (measured):
        //   ~11 statements total across findByIdWithUsers, resolveUsers (findAllById),
        //   role/userInfo batches for all touched users, ONE devices IN-batch for
        //   the REMOVE_COUNT=5 removed users, and saveAll.
        //
        // Without @BatchSize the single devices IN-batch becomes REMOVE_COUNT (5)
        // individual SELECTs, adding +4 extra statements => ~15 total.
        //
        // Bound of 13: above the observed batched total (11) but well below the
        // unbatched total (~15). The gap is 4 statements (one per extra device SELECT
        // beyond the batched one), so the assertion FAILS clearly when @BatchSize is
        // removed (15 > 13).
        long stmtsDelta = statistics.getPrepareStatementCount();
        assertTrue(stmtsDelta <= 13,
                "Expected at most 13 total SQL statements inside manageDeviceUsers "
                        + "(with @BatchSize(50) the " + REMOVE_COUNT + " removed users' "
                        + "devices collections load in 1 batch, not " + REMOVE_COUNT
                        + " individual SELECTs), but got " + stmtsDelta + ". "
                        + "Without @BatchSize this would be ~" + (stmtsDelta + (REMOVE_COUNT - 1)) + ".");
    }

    private UserEntity createUser(RoleEntity userRole, int index) {
        long uniqueSeed = System.nanoTime() + index;

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setFirstName("ManageUser" + index);
        userInfo.setLastName("BatchTest" + index);
        userInfo.setIdentityCard("MUID-" + uniqueSeed);
        userInfo.setPhoneNumber1(String.format("09%08d", uniqueSeed % 100_000_000L));
        userInfo.setCurrentAddress("Hanoi");
        userInfo.setPermanentAddress("Hanoi");
        userInfo.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("manage-users-batch-" + uniqueSeed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(userInfo);
        user.setRoles(List.of(userRole));
        return user;
    }
}
