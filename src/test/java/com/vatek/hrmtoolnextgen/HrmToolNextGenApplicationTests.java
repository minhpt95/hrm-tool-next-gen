package com.vatek.hrmtoolnextgen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.Mockito.mock;

@SpringBootTest
class HrmToolNextGenApplicationTests {

    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @Test
    void contextLoads() {
    }

}
