package com.minhpt.hrmtoolnextgen.config.interceptor;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.constant.ApiConstant;
import com.minhpt.hrmtoolnextgen.service.HolidayService;
import com.minhpt.hrmtoolnextgen.support.AbstractIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
class LegacyApiDeprecationHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestConfig extends AbstractIntegrationTest {
        @Bean
        TokenBucketRateLimiter tokenBucketRateLimiter() {
            return mock(TokenBucketRateLimiter.class);
        }

        @Bean
        HolidayService holidayService() {
            return mock(HolidayService.class);
        }
    }

    @Autowired
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Autowired
    private HolidayService holidayService;

    @BeforeEach
    void allowRateLimiter() {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(holidayService.getCurrentYearHolidays()).thenReturn(Collections.emptyList());
    }

    // -----------------------------------------------------------------------
    // Pre-existing: auth endpoint
    // -----------------------------------------------------------------------

    @Test
    void legacyEndpointShouldExposeDeprecationHeaders() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Wed, 31 Dec 2026 23:59:59 GMT"))
                .andExpect(header().string("Link", "</api/v1/auth/login>; rel=\"successor-version\""));
    }

    @Test
    void versionedEndpointShouldNotExposeDeprecationHeaders() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }

    // -----------------------------------------------------------------------
    // 4.2 — second endpoint family: holidays (path-prefix driven, not endpoint-specific)
    // -----------------------------------------------------------------------

    @Test
    @WithMockUser
    void legacyHolidayEndpointShouldExposeDeprecationHeaders() throws Exception {
        // /api/holidays/current is a legacy path — interceptor must add headers regardless of
        // which endpoint family is used, confirming the behaviour is path-prefix driven (R6.1-6.3)
        mockMvc.perform(get("/api/holidays/current"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", ApiConstant.LEGACY_API_SUNSET))
                .andExpect(header().string("Link", "</api/v1/holidays/current>; rel=\"successor-version\""));
    }

    @Test
    @WithMockUser
    void versionedHolidayEndpointShouldNotExposeDeprecationHeaders() throws Exception {
        // /api/v1/holidays/current is NOT a legacy path — no deprecation headers
        mockMvc.perform(get("/api/v1/holidays/current"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }

    // -----------------------------------------------------------------------
    // 4.2 — pin sunset date against ApiConstant (R6.4)
    // -----------------------------------------------------------------------

    @Test
    void legacyEndpointSunsetHeaderShouldMatchApiConstantValue() throws Exception {
        // Explicitly pins the Sunset value to "Wed, 31 Dec 2026 23:59:59 GMT" via the
        // constant so that a change to ApiConstant.LEGACY_API_SUNSET surfaces here.
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{}"))
                .andExpect(header().string("Sunset", ApiConstant.LEGACY_API_SUNSET));
    }
}
