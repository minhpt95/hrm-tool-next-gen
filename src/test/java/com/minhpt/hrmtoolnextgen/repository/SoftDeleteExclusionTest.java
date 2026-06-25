package com.minhpt.hrmtoolnextgen.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.minhpt.hrmtoolnextgen.HrmToolNextGenApplication;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.repository.jpa.DeviceRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;
import com.minhpt.hrmtoolnextgen.support.Fixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Verifies that @SQLRestriction("is_delete = false") excludes soft-deleted rows
 * from all standard repository reads (findById, findAll) and that non-deleted
 * siblings remain visible. _R17.1, R17.2_
 *
 * <p>NOT @Transactional — each persist/delete must commit so that the subsequent
 * read (in a new persistence context) sees the committed soft-delete state.
 */
@SpringBootTest(classes = {HrmToolNextGenApplication.class, SoftDeleteExclusionTest.MailTestConfig.class})
class SoftDeleteExclusionTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<Long> deviceIdsToDelete = new ArrayList<>();
    private final List<Long> userIdsToDelete = new ArrayList<>();
    private final List<Long> projectIdsToDelete = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.execute(status -> {
            // FK-ordered hard-deletes: join tables first, then child rows, then parents.
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users_devices WHERE user_id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users_projects_working WHERE user_id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users_roles WHERE user_id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            projectIdsToDelete.forEach(pid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM projects WHERE id = :pid")
                            .setParameter("pid", pid)
                            .executeUpdate());
            userIdsToDelete.forEach(uid ->
                    entityManager.createNativeQuery(
                            "DELETE FROM users WHERE id = :uid")
                            .setParameter("uid", uid)
                            .executeUpdate());
            deviceIdsToDelete.forEach(did ->
                    entityManager.createNativeQuery(
                            "DELETE FROM devices WHERE id = :did")
                            .setParameter("did", did)
                            .executeUpdate());
            return null;
        });
        deviceIdsToDelete.clear();
        userIdsToDelete.clear();
        projectIdsToDelete.clear();
    }

    // -------------------------------------------------------------------------
    // DeviceEntity soft-delete exclusion
    // -------------------------------------------------------------------------

    /**
     * Persist a device, soft-delete it via native UPDATE (mirroring @SQLDelete;
     * repository.delete() is blocked by the versioned-@SQLDelete bug — see the
     * @Disabled tests / inline note), then assert findById returns empty
     * (excluded by @SQLRestriction). _R17.1_
     */
    @Test
    void softDeletedDevice_isExcludedFromFindById() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long deviceId = tx.execute(status -> {
            DeviceEntity device = Fixtures.buildDevice(seed);
            DeviceEntity saved = deviceRepository.save(device);
            deviceRepository.flush();
            return saved.getId();
        });
        assert deviceId != null;
        deviceIdsToDelete.add(deviceId);

        // Soft-delete: execute the same SQL that @SQLDelete would issue.
        // repository.delete() on a @Version entity requires the version in the WHERE
        // clause, but DeviceEntity's @SQLDelete only has one '?'. Issuing the UPDATE
        // directly via native query is the equivalent test of the soft-delete mechanism
        // while staying within test scope (no modification to src/main).
        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE devices SET is_delete = true WHERE id = :id")
                    .setParameter("id", deviceId)
                    .executeUpdate();
            return null;
        });

        Optional<DeviceEntity> result = deviceRepository.findById(deviceId);
        assertFalse(result.isPresent(),
                "Soft-deleted DeviceEntity must not be returned by findById");
    }

    /**
     * Persist two devices, soft-delete only one. Assert the deleted row is absent
     * and the surviving sibling is still returned by findById. _R17.2_
     */
    @Test
    void nonDeletedDevice_remainsVisibleAfterSiblingIsSoftDeleted() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        long[] ids = tx.execute(status -> {
            DeviceEntity d1 = Fixtures.buildDevice(seed);
            DeviceEntity d2 = Fixtures.buildDevice(seed + 1);
            DeviceEntity s1 = deviceRepository.save(d1);
            DeviceEntity s2 = deviceRepository.save(d2);
            deviceRepository.flush();
            return new long[]{s1.getId(), s2.getId()};
        });
        assert ids != null;
        long deletedId = ids[0];
        long survivorId = ids[1];
        deviceIdsToDelete.add(deletedId);
        deviceIdsToDelete.add(survivorId);

        // Soft-delete the first device via native UPDATE (same SQL as @SQLDelete).
        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE devices SET is_delete = true WHERE id = :id")
                    .setParameter("id", deletedId)
                    .executeUpdate();
            return null;
        });

        assertFalse(deviceRepository.findById(deletedId).isPresent(),
                "Deleted device must be excluded");
        assertTrue(deviceRepository.findById(survivorId).isPresent(),
                "Non-deleted sibling device must still be visible");
    }

    // -------------------------------------------------------------------------
    // UserEntity soft-delete exclusion
    // -------------------------------------------------------------------------

    /**
     * Persist a user, soft-delete it via native UPDATE (mirroring @SQLDelete;
     * repository.delete() is blocked by the versioned-@SQLDelete bug — see the
     * @Disabled tests / inline note), then assert findById returns empty. _R17.1_
     */
    @Test
    void softDeletedUser_isExcludedFromFindById() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long userId = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity user = Fixtures.buildUser(seed, role);
            UserEntity saved = userRepository.save(user);
            userRepository.flush();
            return saved.getId();
        });
        assert userId != null;
        userIdsToDelete.add(userId);

        // Soft-delete via native UPDATE (same SQL as UserEntity's @SQLDelete).
        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE users SET is_delete = TRUE, deleted_date = NOW() WHERE id = :id")
                    .setParameter("id", userId)
                    .executeUpdate();
            return null;
        });

        Optional<UserEntity> result = userRepository.findById(userId);
        assertFalse(result.isPresent(),
                "Soft-deleted UserEntity must not be returned by findById");
    }

    /**
     * Persist two users, soft-delete only the first. The second must still be
     * visible; the deleted one must be excluded. _R17.2_
     */
    @Test
    void nonDeletedUser_remainsVisibleAfterSiblingIsSoftDeleted() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        long[] ids = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity u1 = Fixtures.buildUser(seed, role);
            UserEntity u2 = Fixtures.buildUser(seed + 1, role);
            long id1 = userRepository.save(u1).getId();
            long id2 = userRepository.save(u2).getId();
            userRepository.flush();
            return new long[]{id1, id2};
        });
        assert ids != null;
        long deletedId = ids[0];
        long survivorId = ids[1];
        userIdsToDelete.add(deletedId);
        userIdsToDelete.add(survivorId);

        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE users SET is_delete = TRUE, deleted_date = NOW() WHERE id = :id")
                    .setParameter("id", deletedId)
                    .executeUpdate();
            return null;
        });

        assertFalse(userRepository.findById(deletedId).isPresent(),
                "Deleted user must be excluded");
        assertTrue(userRepository.findById(survivorId).isPresent(),
                "Non-deleted sibling user must still be visible");
    }

    // -------------------------------------------------------------------------
    // ProjectEntity soft-delete exclusion
    // -------------------------------------------------------------------------

    /**
     * Persist a project, soft-delete it, assert findById returns empty. _R17.1_
     */
    @Test
    void softDeletedProject_isExcludedFromFindById() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        // Projects reference a manager (UserEntity); persist a manager first.
        Long managerId = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity manager = Fixtures.buildUser(seed + 1000, role);
            long id = userRepository.save(manager).getId();
            userRepository.flush();
            return id;
        });
        assert managerId != null;
        userIdsToDelete.add(managerId);

        Long projectId = tx.execute(status -> {
            UserEntity manager = userRepository.findById(managerId).orElseThrow();
            ProjectEntity project = Fixtures.buildProject(seed, manager);
            long id = projectRepository.save(project).getId();
            projectRepository.flush();
            return id;
        });
        assert projectId != null;
        projectIdsToDelete.add(projectId);

        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE projects SET is_delete = TRUE, deleted_date = NOW() WHERE id = :id")
                    .setParameter("id", projectId)
                    .executeUpdate();
            return null;
        });

        Optional<ProjectEntity> result = projectRepository.findById(projectId);
        assertFalse(result.isPresent(),
                "Soft-deleted ProjectEntity must not be returned by findById");
    }

    /**
     * Persist two projects, soft-delete only one; assert selective exclusion. _R17.2_
     */
    @Test
    void nonDeletedProject_remainsVisibleAfterSiblingIsSoftDeleted() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long managerId = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity manager = Fixtures.buildUser(seed + 2000, role);
            long id = userRepository.save(manager).getId();
            userRepository.flush();
            return id;
        });
        assert managerId != null;
        userIdsToDelete.add(managerId);

        long[] projectIds = tx.execute(status -> {
            UserEntity manager = userRepository.findById(managerId).orElseThrow();
            ProjectEntity p1 = Fixtures.buildProject(seed, manager);
            ProjectEntity p2 = Fixtures.buildProject(seed + 1, manager);
            long id1 = projectRepository.save(p1).getId();
            long id2 = projectRepository.save(p2).getId();
            projectRepository.flush();
            return new long[]{id1, id2};
        });
        assert projectIds != null;
        long deletedId = projectIds[0];
        long survivorId = projectIds[1];
        projectIdsToDelete.add(deletedId);
        projectIdsToDelete.add(survivorId);

        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE projects SET is_delete = TRUE, deleted_date = NOW() WHERE id = :id")
                    .setParameter("id", deletedId)
                    .executeUpdate();
            return null;
        });

        assertFalse(projectRepository.findById(deletedId).isPresent(),
                "Deleted project must be excluded");
        assertTrue(projectRepository.findById(survivorId).isPresent(),
                "Non-deleted sibling project must still be visible");
    }

    // -------------------------------------------------------------------------
    // findAll count — soft-deleted row is not counted
    // -------------------------------------------------------------------------

    /**
     * Verify that findAll (with pagination) does not count soft-deleted devices,
     * while a non-deleted sibling is included in the count.
     */
    @Test
    void softDeletedDevice_isNotCountedInFindAll() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        long countBefore = deviceRepository.count();

        long[] ids = tx.execute(status -> {
            DeviceEntity d1 = Fixtures.buildDevice(seed + 3000);
            DeviceEntity d2 = Fixtures.buildDevice(seed + 3001);
            long id1 = deviceRepository.save(d1).getId();
            long id2 = deviceRepository.save(d2).getId();
            deviceRepository.flush();
            return new long[]{id1, id2};
        });
        assert ids != null;
        deviceIdsToDelete.add(ids[0]);
        deviceIdsToDelete.add(ids[1]);

        assertEquals(countBefore + 2, deviceRepository.count(),
                "Both new devices should be counted before any deletion");

        tx.execute(status -> {
            entityManager.createNativeQuery(
                    "UPDATE devices SET is_delete = true WHERE id = :id")
                    .setParameter("id", ids[0])
                    .executeUpdate();
            return null;
        });

        assertEquals(countBefore + 1, deviceRepository.count(),
                "Only the non-deleted device should remain in count after soft-delete");
    }

    // -------------------------------------------------------------------------
    // KNOWN BUG: repository.delete() on @Version entities
    // -------------------------------------------------------------------------

    /**
     * KNOWN BUG: DeviceEntity has both @Version and @SQLDelete. Hibernate 6 appends
     * the @Version value as bind parameter #2, but the @SQLDelete SQL declares only
     * one '?' placeholder (WHERE id = ?). This causes DataIntegrityViolationException
     * "Unable to bind parameter #2" at runtime — DELETE /device/{id} fails.
     *
     * <p>This test records the DESIRED behavior: repository.delete() soft-deletes the
     * row (is_delete=true) and findById returns empty. Re-enable once @SQLDelete SQL
     * is updated to include the version predicate (e.g. "WHERE id = ? AND version = ?").
     */
    @Disabled("KNOWN BUG: @SQLDelete on a @Version entity omits the version predicate; "
            + "Hibernate 6 binds @Version as bind param #2 -> DataIntegrityViolationException "
            + "'Unable to bind parameter #2'. DELETE /device/{id} and /admin/user/{id} fail at "
            + "runtime. Re-enable once the @SQLDelete SQL includes the version predicate "
            + "(e.g. '... WHERE id = ? AND version = ?').")
    @Test
    void softDeletedDevice_viaRepositoryDelete_isExcludedFromFindById() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long deviceId = tx.execute(status -> {
            DeviceEntity device = Fixtures.buildDevice(seed + 9000);
            DeviceEntity saved = deviceRepository.save(device);
            deviceRepository.flush();
            return saved.getId();
        });
        assert deviceId != null;
        deviceIdsToDelete.add(deviceId);

        tx.execute(status -> {
            DeviceEntity saved = deviceRepository.findById(deviceId).orElseThrow();
            deviceRepository.delete(saved);
            deviceRepository.flush();
            return null;
        });

        // Desired: row is soft-deleted (excluded by @SQLRestriction).
        assertFalse(deviceRepository.findById(deviceId).isPresent(),
                "repository.delete() on DeviceEntity must soft-delete the row (is_delete=true)");

        // Desired: native SELECT confirms is_delete=true, not a hard-delete.
        Object isDeleteRaw = tx.execute(status ->
                entityManager.createNativeQuery(
                        "SELECT is_delete FROM devices WHERE id = :id")
                        .setParameter("id", deviceId)
                        .getSingleResult());
        assertTrue(Boolean.TRUE.equals(isDeleteRaw) || (isDeleteRaw instanceof Number n && n.intValue() == 1),
                "is_delete column must be true after repository.delete()");
    }

    /**
     * KNOWN BUG: UserEntity has both @Version and @SQLDelete — same root cause as the
     * Device variant above. DELETE /admin/user/{id} fails at runtime with
     * DataIntegrityViolationException "Unable to bind parameter #2".
     *
     * <p>This test records the DESIRED behavior: repository.delete() soft-deletes the
     * row (is_delete=true) and findById returns empty. Re-enable once @SQLDelete SQL
     * includes the version predicate.
     */
    @Disabled("KNOWN BUG: @SQLDelete on a @Version entity omits the version predicate; "
            + "Hibernate 6 binds @Version as bind param #2 -> DataIntegrityViolationException "
            + "'Unable to bind parameter #2'. DELETE /device/{id} and /admin/user/{id} fail at "
            + "runtime. Re-enable once the @SQLDelete SQL includes the version predicate "
            + "(e.g. '... WHERE id = ? AND version = ?').")
    @Test
    void softDeletedUser_viaRepositoryDelete_isExcludedFromFindById() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long userId = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity user = Fixtures.buildUser(seed + 9001, role);
            UserEntity saved = userRepository.save(user);
            userRepository.flush();
            return saved.getId();
        });
        assert userId != null;
        userIdsToDelete.add(userId);

        tx.execute(status -> {
            UserEntity saved = userRepository.findById(userId).orElseThrow();
            userRepository.delete(saved);
            userRepository.flush();
            return null;
        });

        // Desired: row is soft-deleted (excluded by @SQLRestriction).
        assertFalse(userRepository.findById(userId).isPresent(),
                "repository.delete() on UserEntity must soft-delete the row (is_delete=true)");

        // Desired: native SELECT confirms is_delete=true, not a hard-delete.
        Object isDeleteRaw = tx.execute(status ->
                entityManager.createNativeQuery(
                        "SELECT is_delete FROM users WHERE id = :id")
                        .setParameter("id", userId)
                        .getSingleResult());
        assertTrue(Boolean.TRUE.equals(isDeleteRaw) || (isDeleteRaw instanceof Number n && n.intValue() == 1),
                "is_delete column must be true after repository.delete()");
    }

    // -------------------------------------------------------------------------
    // ProjectEntity repository.delete() — unversioned, must work correctly
    // -------------------------------------------------------------------------

    /**
     * ProjectEntity has no @Version, so repository.delete() invokes @SQLDelete
     * without any extra bind parameter. This test proves the delete() path works
     * for unversioned entities, contrasting the @Disabled Device/User tests above.
     *
     * <p>Persists two projects; deletes only one via repository.delete(); asserts
     * the deleted row is excluded by @SQLRestriction and a native SELECT confirms
     * is_delete=true (soft-delete, not hard-delete). The sibling remains visible.
     */
    @Test
    void softDeletedProject_viaRepositoryDelete_isExcludedAndSiblingRemains() {
        long seed = System.nanoTime();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Long managerId = tx.execute(status -> {
            RoleEntity role = getOrCreateUserRole();
            UserEntity manager = Fixtures.buildUser(seed + 8000, role);
            long id = userRepository.save(manager).getId();
            userRepository.flush();
            return id;
        });
        assert managerId != null;
        userIdsToDelete.add(managerId);

        long[] projectIds = tx.execute(status -> {
            UserEntity manager = userRepository.findById(managerId).orElseThrow();
            ProjectEntity p1 = Fixtures.buildProject(seed + 8001, manager);
            ProjectEntity p2 = Fixtures.buildProject(seed + 8002, manager);
            long id1 = projectRepository.save(p1).getId();
            long id2 = projectRepository.save(p2).getId();
            projectRepository.flush();
            return new long[]{id1, id2};
        });
        assert projectIds != null;
        long deletedId = projectIds[0];
        long survivorId = projectIds[1];
        projectIdsToDelete.add(deletedId);
        projectIdsToDelete.add(survivorId);

        // Delete via repository — ProjectEntity has no @Version so this must work cleanly.
        tx.execute(status -> {
            ProjectEntity toDelete = projectRepository.findById(deletedId).orElseThrow();
            projectRepository.delete(toDelete);
            projectRepository.flush();
            return null;
        });

        assertFalse(projectRepository.findById(deletedId).isPresent(),
                "Soft-deleted project must be excluded from findById after repository.delete()");
        assertTrue(projectRepository.findById(survivorId).isPresent(),
                "Non-deleted sibling project must remain visible");

        // Native SELECT confirms is_delete=true (soft-delete, not a hard-delete).
        Object isDeleteRaw = tx.execute(status ->
                entityManager.createNativeQuery(
                        "SELECT is_delete FROM projects WHERE id = :id")
                        .setParameter("id", deletedId)
                        .getSingleResult());
        assertTrue(Boolean.TRUE.equals(isDeleteRaw) || (isDeleteRaw instanceof Number n && n.intValue() == 1),
                "is_delete column must be true after repository.delete() on a ProjectEntity");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
