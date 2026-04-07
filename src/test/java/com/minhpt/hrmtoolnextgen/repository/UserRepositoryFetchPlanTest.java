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
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import com.minhpt.hrmtoolnextgen.HrmToolNextGenApplication;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(classes = {HrmToolNextGenApplication.class, UserRepositoryFetchPlanTest.MailTestConfig.class})
@Transactional
class UserRepositoryFetchPlanTest {

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
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<Long> savedUserIds = new ArrayList<>();

    private Statistics statistics;

    @SuppressWarnings("unused")
    @BeforeEach
    void setUp() {
        savedUserIds.clear();
        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }

        for (int index = 0; index < 3; index++) {
            savedUserIds.add(userRepository.save(createUser(userRole, index)).getId());
        }

        userRepository.flush();
        entityManager.clear();
        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
    }

    @Test
    void pageableFindAllShouldBatchLoadRolesWithoutCollectionFetchPagination() {
        Page<UserEntity> page = userRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));

        List<UserEntity> loadedUsers = page.getContent().stream()
                .filter(user -> savedUserIds.contains(user.getId()))
                .toList();

        assertEquals(savedUserIds.size(), loadedUsers.size());
        loadedUsers.forEach(user -> assertTrue(Hibernate.isInitialized(user.getUserInfo())));
        loadedUsers.forEach(user -> assertFalse(Hibernate.isInitialized(user.getRoles())));

        loadedUsers.forEach(user -> assertFalse(user.getRoles().isEmpty()));
        loadedUsers.forEach(user -> assertTrue(Hibernate.isInitialized(user.getRoles())));
        assertTrue(statistics.getPrepareStatementCount() <= 4);
    }

    @Test
    void specificationFindAllShouldBatchLoadRolesWithoutCollectionFetchPagination() {
        statistics.clear();
        Specification<UserEntity> spec = (root, query, cb) -> root.get("id").in(savedUserIds);

        Page<UserEntity> page = userRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));
        List<UserEntity> loadedUsers = page.getContent();

        assertEquals(savedUserIds.size(), loadedUsers.size());
        loadedUsers.forEach(user -> assertTrue(Hibernate.isInitialized(user.getUserInfo())));
        loadedUsers.forEach(user -> assertFalse(Hibernate.isInitialized(user.getRoles())));

        loadedUsers.forEach(user -> assertFalse(user.getRoles().isEmpty()));
        loadedUsers.forEach(user -> assertTrue(Hibernate.isInitialized(user.getRoles())));
        assertTrue(statistics.getPrepareStatementCount() <= 4);
    }

    private UserEntity createUser(RoleEntity userRole, int index) {
        long uniqueSeed = System.nanoTime() + index;

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setFirstName("Fetch" + index);
        userInfo.setLastName("Plan" + index);
        userInfo.setIdentityCard("ID-" + uniqueSeed);
        userInfo.setPhoneNumber1(String.format("09%08d", uniqueSeed % 100000000L));
        userInfo.setCurrentAddress("Hanoi");
        userInfo.setPermanentAddress("Hanoi");
        userInfo.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("fetch-plan-" + uniqueSeed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(userInfo);
        user.setRoles(List.of(userRole));
        return user;
    }
}