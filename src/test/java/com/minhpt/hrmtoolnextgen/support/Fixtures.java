package com.minhpt.hrmtoolnextgen.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.minhpt.hrmtoolnextgen.entity.jpa.dayoff.DayOffEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.device.DeviceEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.timesheet.TimesheetEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EDayOffStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EDeviceType;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetStatus;
import com.minhpt.hrmtoolnextgen.enumeration.ETimesheetType;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;

/**
 * Static factory helpers that produce valid, in-memory (non-persisted) entity instances.
 *
 * <p>The {@code seed} parameter keeps unique-constrained fields (email, identityCard,
 * phoneNumber1, serialNumber) collision-free across calls within a single test run.
 * Mirror of the per-test helpers in OptimisticLockingTest, extracted for reuse.
 */
public final class Fixtures {

    private Fixtures() {}

    // -------------------------------------------------------------------------
    // Role
    // -------------------------------------------------------------------------

    public static RoleEntity buildRole(EUserRole userRole) {
        RoleEntity role = new RoleEntity();
        role.setUserRole(userRole);
        return role;
    }

    /** Convenience: USER role (most commonly needed). */
    public static RoleEntity buildUserRole() {
        return buildRole(EUserRole.USER);
    }

    // -------------------------------------------------------------------------
    // UserInfo + User
    // -------------------------------------------------------------------------

    public static UserInfoEntity buildUserInfo(long seed) {
        UserInfoEntity info = new UserInfoEntity();
        info.setFirstName("First" + seed);
        info.setLastName("Last" + seed);
        info.setIdentityCard("ID-" + seed);
        info.setPhoneNumber1("09" + String.format("%09d", Math.floorMod(seed, 1_000_000_000L)));
        info.setCurrentAddress("Hanoi");
        info.setPermanentAddress("Hanoi");
        info.setOnboardDate(LocalDate.of(2026, 1, 1));
        return info;
    }

    /**
     * Builds a UserEntity with a minimal set of required fields.
     *
     * <p>{@code @Version version} is intentionally left null; Hibernate populates it on first persist.
     *
     * @param seed used to generate unique email / identity-card / phone
     * @param roles one or more roles to assign; pass {@link #buildUserRole()} when unsure
     */
    public static UserEntity buildUser(long seed, List<RoleEntity> roles) {
        UserEntity user = new UserEntity();
        user.setEmail("user-" + seed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(buildUserInfo(seed));
        user.setRoles(roles);
        return user;
    }

    /** Convenience overload: single role. */
    public static UserEntity buildUser(long seed, RoleEntity role) {
        return buildUser(seed, List.of(role));
    }

    /** Convenience overload: USER role created inline (no persistence needed). */
    public static UserEntity buildUser(long seed) {
        return buildUser(seed, buildUserRole());
    }

    // -------------------------------------------------------------------------
    // Device
    // -------------------------------------------------------------------------

    /** {@code @Version version} is intentionally left null; Hibernate populates it on first persist. */
    public static DeviceEntity buildDevice(long seed) {
        DeviceEntity device = new DeviceEntity();
        device.setName("Device-" + seed);
        device.setDescription("Test device " + seed);
        device.setSerialNumber("SN-" + seed);
        device.setType(EDeviceType.LAPTOP);
        device.setStatus(EDeviceStatus.ACTIVE);
        return device;
    }

    public static DeviceEntity buildDevice(long seed, EDeviceType type, EDeviceStatus status) {
        DeviceEntity device = buildDevice(seed);
        device.setType(type);
        device.setStatus(status);
        return device;
    }

    // -------------------------------------------------------------------------
    // Project
    // -------------------------------------------------------------------------

    /**
     * Builds a ProjectEntity. The {@code manager} may be {@code null} for tests
     * that don't exercise manager-related logic.
     */
    public static ProjectEntity buildProject(long seed, UserEntity manager) {
        ProjectEntity project = new ProjectEntity();
        project.setName("Project-" + seed);
        project.setClientName("Client-" + seed);
        project.setDescription("Test project " + seed);
        project.setProjectStatus(EProjectStatus.RUNNING);
        project.setStartTime(LocalDate.of(2026, 1, 1));
        project.setEndTime(LocalDate.of(2026, 12, 31));
        project.setProjectManager(manager);
        return project;
    }

    /** Convenience: no manager assigned. */
    public static ProjectEntity buildProject(long seed) {
        return buildProject(seed, null);
    }

    // -------------------------------------------------------------------------
    // Timesheet
    // -------------------------------------------------------------------------

    /**
     * Builds a TimesheetEntity linked to the given user and project.
     * Either may be {@code null} for isolated unit tests.
     */
    public static TimesheetEntity buildTimesheet(long seed, UserEntity user, ProjectEntity project) {
        TimesheetEntity ts = new TimesheetEntity();
        ts.setTitle("Timesheet-" + seed);
        ts.setDescription("Test timesheet " + seed);
        ts.setWorkingHours(LocalTime.of(8, 0));
        ts.setType(ETimesheetType.NORMAL);
        ts.setWorkingDay(LocalDate.of(2026, 1, (int) (seed % 28) + 1));
        ts.setStatus(ETimesheetStatus.PENDING);
        ts.setUserEntity(user);
        ts.setProjectEntity(project);
        return ts;
    }

    /** Convenience: no user or project linked. */
    public static TimesheetEntity buildTimesheet(long seed) {
        return buildTimesheet(seed, null, null);
    }

    // -------------------------------------------------------------------------
    // DayOff
    // -------------------------------------------------------------------------

    /**
     * Builds a DayOffEntity. {@code requestedBy} may be {@code null} for isolated tests.
     */
    public static DayOffEntity buildDayOff(long seed, UserEntity requestedBy) {
        DayOffEntity dayOff = new DayOffEntity();
        dayOff.setTitle("DayOff-" + seed);
        dayOff.setReason("Test reason " + seed);
        dayOff.setStartTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        dayOff.setEndTime(LocalDateTime.of(2026, 6, 1, 18, 0));
        dayOff.setStatus(EDayOffStatus.PENDING);
        dayOff.setRequestedAt(LocalDateTime.of(2026, 5, 28, 10, 0));
        dayOff.setRequestedBy(requestedBy);
        return dayOff;
    }

    /** Convenience: no owner linked. */
    public static DayOffEntity buildDayOff(long seed) {
        return buildDayOff(seed, null);
    }
}
