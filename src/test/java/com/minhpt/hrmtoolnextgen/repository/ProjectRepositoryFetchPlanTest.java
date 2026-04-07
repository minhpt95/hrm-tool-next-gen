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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import com.minhpt.hrmtoolnextgen.HrmToolNextGenApplication;
import com.minhpt.hrmtoolnextgen.entity.jpa.project.ProjectEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserEntity;
import com.minhpt.hrmtoolnextgen.entity.jpa.user.UserInfoEntity;
import com.minhpt.hrmtoolnextgen.enumeration.EProjectStatus;
import com.minhpt.hrmtoolnextgen.enumeration.EUserRole;
import com.minhpt.hrmtoolnextgen.repository.jpa.ProjectRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.RoleRepository;
import com.minhpt.hrmtoolnextgen.repository.jpa.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest(classes = {HrmToolNextGenApplication.class, ProjectRepositoryFetchPlanTest.MailTestConfig.class})
@Transactional
class ProjectRepositoryFetchPlanTest {

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
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final List<Long> savedProjectIds = new ArrayList<>();

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        savedProjectIds.clear();
        RoleEntity userRole = roleRepository.findByUserRole(EUserRole.USER);
        if (userRole == null) {
            userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole = roleRepository.save(userRole);
        }

        UserEntity manager = userRepository.save(createUser(userRole, "manager"));
        List<UserEntity> members = List.of(
                userRepository.save(createUser(userRole, "member-a")),
                userRepository.save(createUser(userRole, "member-b")),
                userRepository.save(createUser(userRole, "member-c"))
        );

        for (int index = 0; index < 3; index++) {
            ProjectEntity project = new ProjectEntity();
            project.setName("Fetch Plan Project " + index + "-" + System.nanoTime());
            project.setClientName("Client " + index);
            project.setDescription("Project fetch plan test " + index);
            project.setProjectStatus(EProjectStatus.RUNNING);
            project.setStartTime(LocalDate.of(2026, 1, 1).plusDays(index));
            project.setEndTime(LocalDate.of(2026, 12, 31).minusDays(index));
            project.setProjectManager(manager);
            members.forEach(project::addMemberToProject);
            savedProjectIds.add(projectRepository.save(project).getId());
        }

        projectRepository.flush();
        entityManager.clear();
        statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();
    }

    @Test
    void pageableFindAllShouldBatchLoadMembersWithoutCollectionFetchPagination() {
        Page<ProjectEntity> page = projectRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));

        List<ProjectEntity> loadedProjects = page.getContent().stream()
                .filter(project -> savedProjectIds.contains(project.getId()))
                .toList();

        assertEquals(savedProjectIds.size(), loadedProjects.size());
        loadedProjects.forEach(project -> assertTrue(Hibernate.isInitialized(project.getProjectManager())));
        loadedProjects.forEach(project -> assertFalse(project.getMembers().isEmpty()));
        loadedProjects.forEach(project -> assertTrue(Hibernate.isInitialized(project.getMembers())));
        assertTrue(statistics.getPrepareStatementCount() <= 4);
    }

    @Test
    void specificationFindAllShouldBatchLoadMembersWithoutCollectionFetchPagination() {
        statistics.clear();
        Specification<ProjectEntity> spec = (root, query, cb) -> root.get("id").in(savedProjectIds);

        Page<ProjectEntity> page = projectRepository.findAll(spec, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));
        List<ProjectEntity> loadedProjects = page.getContent();

        assertEquals(savedProjectIds.size(), loadedProjects.size());
        loadedProjects.forEach(project -> assertTrue(Hibernate.isInitialized(project.getProjectManager())));
        loadedProjects.forEach(project -> assertFalse(project.getMembers().isEmpty()));
        loadedProjects.forEach(project -> assertTrue(Hibernate.isInitialized(project.getMembers())));
        assertTrue(statistics.getPrepareStatementCount() <= 4);
    }

    private UserEntity createUser(RoleEntity userRole, String suffix) {
        long uniqueSeed = System.nanoTime();

        UserInfoEntity userInfo = new UserInfoEntity();
        userInfo.setFirstName("Fetch");
        userInfo.setLastName("Project" + suffix);
        userInfo.setIdentityCard("PID-" + suffix + "-" + uniqueSeed);
        userInfo.setPhoneNumber1(String.format("09%08d", uniqueSeed % 100000000L));
        userInfo.setCurrentAddress("Hanoi");
        userInfo.setPermanentAddress("Hanoi");
        userInfo.setOnboardDate(LocalDate.of(2026, 1, 1));

        UserEntity user = new UserEntity();
        user.setEmail("project-fetch-" + suffix + "-" + uniqueSeed + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setUserInfo(userInfo);
        user.setRoles(List.of(userRole));
        return user;
    }
}