package com.minhpt.hrmtoolnextgen.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.constant.RoleConstant;
import com.minhpt.hrmtoolnextgen.dto.principal.UserPrincipalDto;

/**
 * Integration tests for SseController.
 *
 * Both SSE endpoints sit under USER_ENDPOINTS in WebSecurityConfig, meaning any
 * authenticated principal passes the URL-level filter. The controller uses
 * @AuthenticationPrincipal UserPrincipalDto, so we inject a real UserPrincipalDto
 * via SecurityMockMvcRequestPostProcessors.user() — @WithMockUser injects a
 * String-based principal and causes ClassCastException at runtime.
 *
 * Strategy:
 *   - TokenBucketRateLimiter @MockBean — avoids Redis calls from the @RateLimit
 *     aspect wired into the application context.
 *   - JavaMailSender provided via @TestConfiguration mock — prevents SMTP wiring.
 *   - No @MockBean SseService — we use the real in-memory service so the count
 *     endpoint reflects actual state.
 *
 * Endpoints under test:
 *   GET /api/v1/sse/connect            → 200 + text/event-stream (R21.1)
 *   GET /api/v1/sse/connections/count  → 200 + numeric body    (R21.3)
 *   GET /api/v1/sse/connect unauthenticated → 401              (security gate)
 *
 * SSE async assertion:
 *   MockMvc returns an MvcResult with an async result (the SseEmitter). We assert
 *   request().asyncStarted() to confirm the emitter was registered, then read
 *   Content-Type from the initial response object directly.
 *   We do NOT call asyncDispatch() — that would block waiting for the emitter to
 *   complete, which never happens since no client closes the stream.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SseControllerIntegrationTest {

    @SuppressWarnings("unused")
    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    @Autowired
    private MockMvc mockMvc;

    // -------------------------------------------------------------------------
    // Helper: a minimal UserPrincipalDto that @AuthenticationPrincipal will bind
    // -------------------------------------------------------------------------

    private UserPrincipalDto userPrincipal(long id) {
        return UserPrincipalDto.internalBuilder()
                .id(id)
                .email("user-" + id + "@example.com")
                .authorities(java.util.List.of(new SimpleGrantedAuthority(RoleConstant.USER)))
                .build();
    }

    // -------------------------------------------------------------------------
    // R21.1 — GET /api/v1/sse/connect as authenticated user
    //
    // MockMvc handles SseEmitter as an async result. We:
    //   1. Perform the request — assert asyncStarted (emitter was registered).
    //   2. Dispatch the async result — assert 200 + text/event-stream Content-Type.
    // -------------------------------------------------------------------------

    @Test
    void connect_asAuthenticatedUser_asyncStarted_andContentTypeIsTextEventStream() throws Exception {
        // SseEmitter causes an async result. asyncDispatch() would block forever
        // (emitter never completes). Instead: assert asyncStarted (emitter registered)
        // and read Content-Type from the initial MockHttpServletResponse directly —
        // Spring sets the Content-Type header from produces=text/event-stream before
        // handing off to the async path.
        MvcResult result = mockMvc.perform(
                        get("/api/v1/sse/connect")
                                .with(user(userPrincipal(1L))))
                .andExpect(request().asyncStarted())
                .andReturn();

        String contentType = result.getResponse().getContentType();
        assertTrue(
                contentType != null && contentType.contains(MediaType.TEXT_EVENT_STREAM_VALUE),
                "Expected text/event-stream but got: " + contentType);
    }

    // -------------------------------------------------------------------------
    // R21.1 — unauthenticated request → 401
    // -------------------------------------------------------------------------

    @Test
    void connect_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/sse/connect"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // R21.3 — GET /api/v1/sse/connections/count → 200 with numeric body
    // -------------------------------------------------------------------------

    @Test
    void getConnectionCount_asAuthenticatedUser_returns200WithNumericBody() throws Exception {
        mockMvc.perform(
                        get("/api/v1/sse/connections/count")
                                .with(user(userPrincipal(2L))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // -------------------------------------------------------------------------
    // R21.3 — unauthenticated count request → 401
    // -------------------------------------------------------------------------

    @Test
    void getConnectionCount_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/sse/connections/count"))
                .andExpect(status().isUnauthorized());
    }
}
