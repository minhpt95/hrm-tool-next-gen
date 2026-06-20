package com.minhpt.hrmtoolnextgen.repository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;

/**
 * Tests for @Version optimistic locking on DeviceEntity and UserEntity.
 *
 * <p>The class is intentionally NOT @Transactional so that each save commits
 * to the shared H2 database, making the stale-version conflict visible across
 * separate persistence contexts (TEST 1). Rows are cleaned up in @AfterEach.
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, OptimisticLockingTest.MailTestConfig.class})
class OptimisticLockingTest {

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
    private DeviceRepository deviceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @PersistenceUnit
    private EntityManagerFactory entityManagerFactory;

    /** IDs of rows created during a test — deleted in @AfterEach. */
    private final List<Long> deviceIdsToDelete = new ArrayList<>();
    private final List<Long> userIdsToDelete = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            // Remove join-table rows first to satisfy FK constraints.
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users_devices WHERE user_id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users_roles WHERE user_id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            // Hard-delete users (bypass soft-delete so repeated runs stay clean).
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users WHERE id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            // Hard-delete devices.
            deviceIdsToDelete.forEach(did ->
                    entityManager.createNativeQuery(
                            "DELETE FROM devices WHERE id = :did")
                            .setParameter("did", did)
                            .executeUpdate());
            return null;
        });
        deviceIdsToDelete.clear();
        userIdsToDelete.clear();
    }

    // -------------------------------------------------------------------------
    // TEST 1 — Stale-version update on DeviceEntity triggers optimistic failure
    // -------------------------------------------------------------------------

    /**
     * Mechanism: two independent transactions via TransactionTemplate (no shared
     * Hibernate session, so the first-level cache cannot mask the conflict).
     *
     * <pre>
     * tx-A: persist device → version 0 committed.
     * tx-B: load fresh copy, rename it → version 0 → 1 committed.
     * tx-C: attempt to save the STALE copy (still at version 0) via
     *       deviceRepository.save() → Hibernate issues UPDATE … WHERE version = 0
     *       but DB already has version = 1 → 0 rows affected →
     *       ObjectOptimisticLockingFailureException.
     * </pre>
     *
     * Non-vacuousness: if {@code @Version} were removed from DeviceEntity,
     * Hibernate would not include a version predicate in the UPDATE, the stale
     * save would succeed silently, and assertThrows would fail.
     */
    @Test
    void staleVersionUpdateOnDevice_shouldThrowOptimisticLockingFailure() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // tx-A: persist the device; record its ID for cleanup.
        Long deviceId = tx.execute(status -> {
            DeviceEntity device = buildDevice(seed);
            DeviceEntity saved = deviceRepository.save(device);
            deviceRepository.flush();
            return saved.getId();
        });
        assert deviceId != null;
        deviceIdsToDelete.add(deviceId);

        // Load a STALE reference (version = 0) before tx-B increments it.
        // We load it here in a separate tx so the object is detached afterward.
        final DeviceEntity staleDevice = tx.execute(status ->
                deviceRepository.findById(deviceId).orElseThrow());
        assert staleDevice != null;

        // tx-B: load a fresh copy, mutate it, commit → DB version becomes 1.
        tx.execute(status -> {
            DeviceEntity fresh = deviceRepository.findById(deviceId).orElseThrow();
            fresh.setName("Updated by tx-B");
            deviceRepository.save(fresh);
            deviceRepository.flush();
            return null;
        });

        // tx-C: attempt to persist the STALE copy (version = 0) → must fail.
        // The stale entity is detached; save() calls merge(), Hibernate checks
        // the version column and throws ObjectOptimisticLockingFailureException.
        assertThrows(
                ObjectOptimisticLockingFailureException.class,
                () -> tx.execute(status -> {
                    staleDevice.setName("Conflicting update from stale copy");
                    deviceRepository.save(staleDevice);
                    deviceRepository.flush();
                    return null;
                }),
                "Expected ObjectOptimisticLockingFailureException when saving a stale DeviceEntity"
        );
    }

    // -------------------------------------------------------------------------
    // TEST 2 — Collection mutation on UserEntity.devices bumps the owner version
    // -------------------------------------------------------------------------

    /**
     * Proves that {@code @OptimisticLock(excluded = false)} on the {@code devices}
     * collection causes a join-table change to increment the owning UserEntity's
     * {@code @Version} counter.
     *
     * <pre>
     * tx-A: persist user (v0) + device, committed.
     * tx-B: load user, add device to user.getDevices(), save + flush → committed.
     *       Hibernate issues UPDATE users SET version = 1 WHERE id = ? AND version = 0.
     * Reload user in a new tx → assert version > v0.
     * </pre>
     *
     * Non-vacuousness: if {@code @OptimisticLock(excluded = false)} were absent
     * (or changed to {@code excluded = true}), Hibernate treats the collection
     * mutation as excluded from versioning, the UPDATE would not touch the version
     * column, and the reloaded version would remain at v0 — causing the assertion
     * {@code version > v0} to fail.
     */
    @Test
    void addingDeviceToUserCollection_shouldBumpUserVersion() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // tx-A: persist the user and the device in committed state.
        final long[] ids = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity user = buildUser(seed, role);
            UserEntity savedUser = userRepository.save(user);
            userRepository.flush();

            DeviceEntity device = buildDevice(seed);
            DeviceEntity savedDevice = deviceRepository.save(device);
            deviceRepository.flush();

            return new long[]{savedUser.getId(), savedDevice.getId()};
        });
        assert ids != null;
        long userId = ids[0];
        long deviceId = ids[1];
        userIdsToDelete.add(userId);
        deviceIdsToDelete.add(deviceId);

        // Capture v0 — the version after initial persist (typically 0).
        Long v0 = tx.execute(status ->
                userRepository.findById(userId).orElseThrow().getVersion());
        assert v0 != null;

        // tx-B: load fresh user, add the device, commit.
        tx.execute(status -> {
            // Use a plain EntityManager query to load user WITH devices initialised,
            // bypassing the EntityGraph on UserRepository.findById which doesn't
            // include the devices collection.
            UserEntity user = entityManager.find(UserEntity.class, userId);
            DeviceEntity device = entityManager.find(DeviceEntity.class, deviceId);
            user.getDevices().add(device);
            userRepository.save(user);
            userRepository.flush();
            return null;
        });

        // Reload in a fresh tx and assert the version was incremented.
        Long vAfter = tx.execute(status ->
                userRepository.findById(userId).orElseThrow().getVersion());
        assert vAfter != null;

        assertTrue(
                vAfter > v0,
                "UserEntity version should have incremented after mutating the devices collection. "
                        + "v0=" + v0 + ", vAfter=" + vAfter
        );
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DeviceEntity buildDevice(long seed) {
        DeviceEntity device = new DeviceEntity();
        device.setName("Test Device " + seed);
        device.setDescription("Created for optimistic locking test");
        device.setSerialNumber("SN-OPT-" + seed);
        device.setType(EDeviceType.LAPTOP);
        device.setStatus(EDeviceStatus.ACTIVE);
        return device;
    }

    private UserEntity buildUser(long seed, RoleEntity role) {
        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setFirstName("OptLock");
        userInfo.setLastName("Test");
        userInfo.setIdentityCard("OPT-ID-" + seed);
        userInfo.setPhoneNumber1(String.format("09%08d", seed % 100_000_000L));
        userInfo.setCurrentAddress("Hanoi");
        userInfo.setPermanentAddress("Hanoi");
        userInfo.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("opt-lock-" + seed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(userInfo);
        user.setRoles(List.of(role));
        return user;
    }

    private RoleEntity getOrCreateUserRole() {
        RoleEntity role = roleRepository.findByUserRole(EUserRole.USER);
        if (role == null) {
            role = new RoleEntity();
            role.setUserRole(EUserRole.USER);
            role = roleRepository.save(role);
            roleRepository.flush();
        }
        return role;
    }
}
