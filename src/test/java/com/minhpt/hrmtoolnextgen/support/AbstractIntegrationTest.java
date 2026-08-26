package com.minhpt.hrmtoolnextgen.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

/**
 * Shared base for integration tests that need a {@link JavaMailSender} mock.
 *
 * <p>Extend this class instead of defining a per-test-class {@code MailTestConfig}
 * inner class. The mock bean is registered under {@link JavaMailSender}'s own type,
 * so it replaces any autoconfigured real sender without additional qualifiers.</p>
 *
 * <p>Classes that already define their own {@code @TestConfiguration} with additional
 * beans (e.g. {@code @MockBean} fields) can add {@code extends AbstractIntegrationTest}
 * to their existing inner config class.</p>
 */
@TestConfiguration
public abstract class AbstractIntegrationTest {

    @Bean
    JavaMailSender javaMailSender() {
        return mock(JavaMailSender.class);
    }
}
