package com.minhpt.hrmtoolnextgen.config.actuator;

import static com.minhpt.hrmtoolnextgen.constant.RoleConstant.ADMIN;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitMetricsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @MockBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(false);
    }

    @Test
    @WithMockUser(authorities = ADMIN)
    void rejectedLoginShouldIncrementAndExposeRateLimitMetric() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "username": "blocked@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isTooManyRequests());

        mockMvc.perform(get("/actuator/metrics/hrm.rate_limit.violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hrm.rate_limit.violations"))
                .andExpect(jsonPath("$.measurements[0].value").isNumber())
                .andExpect(jsonPath("$.measurements[0].value").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1.0)))
                .andExpect(jsonPath("$.availableTags[?(@.tag=='method')].values[0]").value("login"))
                .andExpect(jsonPath("$.availableTags[?(@.tag=='key_prefix')].values[0]").value("ratelimit:login"));
    }
}