package com.vatek.hrmtoolnextgen.config;

import com.vatek.hrmtoolnextgen.dto.principal.UserPrincipalDto;
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
    public AuditorAware<Long> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Implementation of AuditorAware to get the current user ID from Spring Security context
     * Returns the user ID as Long to match the database column type (BIGINT)
     */
    private static class SpringSecurityAuditorAware implements AuditorAware<Long> {

        @Override
        public Optional<Long> getCurrentAuditor() {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !authentication.isAuthenticated()) {
                    log.debug("No authentication found, returning 0 (system) as auditor");
                    return Optional.of(0L);
                }

                Object principal = authentication.getPrincipal();

                if (principal instanceof UserPrincipalDto userPrincipal) {
                    Long userId = userPrincipal.getId();
                    if (userId != null) {
                        log.debug("Current auditor: {}", userId);
                        return Optional.of(userId);
                    }
                }

                log.debug("Principal is not UserPrincipalDto or has no ID, returning 0 (system) as auditor");
                return Optional.of(0L);
            } catch (Exception e) {
                log.warn("Error getting current auditor: {}, using 0 (system) as default", e.getMessage());
                return Optional.of(0L);
            }
        }
    }
}

