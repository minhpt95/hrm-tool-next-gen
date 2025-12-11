package com.vatek.hrmtoolnextgen.config;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@Log4j2
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Implementation of AuditorAware to get the current user ID from Spring Security context
     * Returns the user ID as String to match the database column type (VARCHAR)
     */
    private static class SpringSecurityAuditorAware implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    log.debug("No authentication found, returning SYSTEM as default auditor");
                    return Optional.of("SYSTEM");
                }

                Object principal = authentication.getPrincipal();

                if (principal instanceof UserPrincipalDto) {
                    UserPrincipalDto userPrincipal = (UserPrincipalDto) principal;
                    Long userId = userPrincipal.getId();
                    if (userId != null) {
                        String userIdString = String.valueOf(userId);
                        log.debug("Current auditor: {}", userIdString);
                        return Optional.of(userIdString);
                    }
                }

                log.debug("Principal is not UserPrincipalDto or has no ID, returning SYSTEM as default auditor");
                return Optional.of("SYSTEM");
            } catch (Exception e) {
                log.warn("Error getting current auditor: {}, using SYSTEM as default", e.getMessage());
                return Optional.of("SYSTEM");
            }
        }
    }
}

