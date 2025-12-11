package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.entity.jpa.role.RoleEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserRole;
import com.vatek.hrmtoolnextgen.repository.jpa.RoleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class InitDataService {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        List<RoleEntity> roles = roleRepository.findAll();

        if (roles.isEmpty()) {
            RoleEntity adminRole = new RoleEntity();
            adminRole.setUserRole(EUserRole.ADMIN);
            adminRole.setCreatedBy("ADMIN");
            RoleEntity userRole = new RoleEntity();
            userRole.setUserRole(EUserRole.USER);
            userRole.setCreatedBy("ADMIN");
            RoleEntity itRole = new RoleEntity();
            itRole.setUserRole(EUserRole.IT_ADMIN);
            itRole.setCreatedBy("ADMIN");
            RoleEntity pmRole = new RoleEntity();
            pmRole.setUserRole(EUserRole.PROJECT_MANAGER);
            pmRole.setCreatedBy("ADMIN");

            roles.add(adminRole);
            roles.add(userRole);
            roles.add(itRole);
            roles.add(pmRole);
            roleRepository.saveAll(roles);
        }
    }
}
