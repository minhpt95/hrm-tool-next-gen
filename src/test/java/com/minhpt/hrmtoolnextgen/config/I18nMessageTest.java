package com.minhpt.hrmtoolnextgen.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.minhpt.hrmtoolnextgen.component.MessageService;
import com.minhpt.hrmtoolnextgen.component.TokenBucketRateLimiter;
import com.minhpt.hrmtoolnextgen.dto.request.LoginRequest;
import com.minhpt.hrmtoolnextgen.exception.UnauthorizedException;
import com.minhpt.hrmtoolnextgen.service.auth.AuthService;

/**
 * Tests for i18n message resolution — R23.1 (vi), R23.2 (en / default), R23.3 (success + error).
 *
 * <p>Two complementary strategies:
 * <ul>
 *   <li>Direct: autowire MessageService, set LocaleContextHolder, assert decoded strings.
 *   <li>End-to-end: MockMvc with Accept-Language header drives AcceptHeaderLocaleResolver
 *       → LocaleContextHolder → MessageService in the request thread.
 * </ul>
 *
 * <p>Decoded expected values (from messages.properties / messages_vi.properties):
 * <pre>
 *   success             en: "Successfully"    vi: "Thanh cong" (decoded from escape sequences)
 *   auth.login.invalid  en: "Username or password invalid."
 *                       vi: "Ten dang nhap hoac mat khau khong hop le." (decoded)
 * </pre>
 *
 * <p>LocaleConfig note: no LocaleResolver bean is declared; Spring Boot's default
 * AcceptHeaderLocaleResolver is active. MessageService reads LocaleContextHolder.getLocale()
 * which is populated by the resolver on each request. Default (no header) resolves to the
 * JVM default locale; for determinism the direct tests set the locale explicitly.
 */
@SpringBootTest
@AutoConfigureMockMvc
class I18nMessageTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MessageService messageService;

    @MockBean
    private TokenBucketRateLimiter tokenBucketRateLimiter;

    // AuthService is @MockBean so login never touches DB/Redis.
    @MockBean
    private AuthService authService;

    @TestConfiguration
    static class MailTestConfig {
        @Bean
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    private Locale originalDefaultLocale;

    @BeforeEach
    void pinDefaultLocale() {
        // Pin JVM default to English so no-Accept-Language paths resolve deterministically
        // on any host, regardless of the OS/CI locale configuration.
        originalDefaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
    }

    @AfterEach
    void resetLocale() {
        // Prevent locale leaking into subsequent tests run in the same thread.
        LocaleContextHolder.resetLocaleContext();
        Locale.setDefault(originalDefaultLocale);
    }

    // -------------------------------------------------------------------------
    // Direct MessageService resolution — R23.1, R23.2
    // -------------------------------------------------------------------------

    /**
     * A known key resolved with Locale.ENGLISH must return the English value. _R23.2_
     */
    @Test
    void messageService_withEnglishLocale_resolvesEnglishValue() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String resolved = messageService.getMessage("success");

        assertEquals("Successfully", resolved);
    }

    /**
     * The same key resolved with the Vietnamese locale must return the decoded
     * Vietnamese value, not the unicode escape sequence stored in the .properties file. _R23.1_
     */
    @Test
    void messageService_withVietnameseLocale_resolvesVietnameseValue() {
        LocaleContextHolder.setLocale(new Locale("vi"));

        String resolved = messageService.getMessage("success");

        assertEquals("Thành công", resolved);
    }

    /**
     * When LocaleContextHolder holds no explicit locale (reset state) the MessageSource
     * falls back to the JVM default locale — pinned to {@link Locale#ENGLISH} in
     * {@code @BeforeEach} for determinism — and must NOT return the Vietnamese string. _R23.2_
     */
    @Test
    void messageService_withNoLocaleSet_doesNotReturnVietnamese() {
        // LocaleContextHolder was reset by @AfterEach; do not set any locale here.
        String resolved = messageService.getMessage("success");

        assertNotEquals("Thành công", resolved,
                "Default (no locale) must not resolve to the Vietnamese string");
    }

    /**
     * A success-side key localises correctly: "success" → Vietnamese. _R23.3_
     */
    @Test
    void messageService_successKey_localisesToVietnamese() {
        LocaleContextHolder.setLocale(new Locale("vi"));

        String resolved = messageService.getMessage("success");

        assertEquals("Thành công", resolved);
    }

    /**
     * An error-side key also localises correctly: "auth.login.invalid" → Vietnamese. _R23.3_
     */
    @Test
    void messageService_errorKey_localisesToVietnamese() {
        LocaleContextHolder.setLocale(new Locale("vi"));

        String resolved = messageService.getMessage("auth.login.invalid");

        assertEquals("Tên đăng nhập hoặc mật khẩu không hợp lệ.", resolved);
    }

    /**
     * Same error-side key with English locale. _R23.3_
     */
    @Test
    void messageService_errorKey_resolvesEnglishValue() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String resolved = messageService.getMessage("auth.login.invalid");

        assertEquals("Username or password invalid.", resolved);
    }

    // -------------------------------------------------------------------------
    // End-to-end body localisation (MockMvc) — R23.1, R23.2, R23.3
    // -------------------------------------------------------------------------

    /**
     * Strategy: POST /api/v1/auth/login with authService mocked to throw
     * UnauthorizedException with messageCode="auth.login.invalid".
     * CommonControllerAdvice.buildErrorResponse() calls messageService.getMessage(messageCode)
     * in the request thread, where AcceptHeaderLocaleResolver has already set
     * LocaleContextHolder from the Accept-Language header.
     *
     * Note: the existing AuthControllerIntegrationTest throws with a plain message string
     * (no messageCode), which bypasses messageService entirely — these tests are distinct:
     * they exercise the messageCode resolution path through buildErrorResponse.
     *
     * Accept-Language: vi → Vietnamese body. _R23.1, R23.3_
     */
    @Test
    void endToEnd_withViAcceptLanguage_responseBodyContainsVietnameseMessage() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("auth.login.invalid", new Object[]{}));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "vi")
                        .content("{\"username\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Tên đăng nhập hoặc mật khẩu không hợp lệ."));
    }

    /**
     * Same endpoint with Accept-Language: en → English body. _R23.2, R23.3_
     */
    @Test
    void endToEnd_withEnAcceptLanguage_responseBodyContainsEnglishMessage() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("auth.login.invalid", new Object[]{}));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content("{\"username\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Username or password invalid."));
    }

    /**
     * Explicit en-vs-vi contrast: the two responses carry DIFFERENT message strings,
     * proving locale is the sole driver of the body difference. _R23.1, R23.2_
     */
    @Test
    void endToEnd_enAndVi_responseBodyDiffers() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("auth.login.invalid", new Object[]{}));

        MvcResult enResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "en")
                        .content("{\"username\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult viResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "vi")
                        .content("{\"username\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String enBody = enResult.getResponse().getContentAsString();
        String viBody = viResult.getResponse().getContentAsString();

        assertNotEquals(enBody, viBody,
                "Accept-Language: en and Accept-Language: vi must produce different response bodies");
    }

    /**
     * No Accept-Language header → default locale → English body (not Vietnamese). _R23.2_
     */
    @Test
    void endToEnd_withNoAcceptLanguage_responseBodyIsEnglish() throws Exception {
        when(tokenBucketRateLimiter.tryConsume(anyString(), anyInt(), anyInt())).thenReturn(true);
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("auth.login.invalid", new Object[]{}));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user@example.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Username or password invalid."));
    }
}
