package com.minhpt.hrmtoolnextgen.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Unit tests for EmailService — plain Mockito, no Spring context, no live SMTP.
 *
 * R22.1 sendPasswordResetEmail — renders "password-reset" template and dispatches via mailSender.
 * R22.1 sendWelcomeEmail      — renders "welcome-user" template and dispatches.
 * R22.1 sendBirthdayEmail     — renders "birthday" template and dispatches.
 * Swallow-vs-rethrow: all three methods catch MessagingException (and Exception) and
 * log without rethrowing — verified by assertDoesNotThrow on a throwing mailSender.send.
 * Early-return on empty fromEmail: when fromEmail is blank, mailSender is never called.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private SpringTemplateEngine emailTemplateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // Inject non-empty fromEmail so the "email not configured" guard does not
        // short-circuit; frontendUrl can remain at its default but set it explicitly.
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:3000");

        // Real MimeMessage so MimeMessageHelper can set properties without NPE.
        // lenient: the blankFromEmail tests early-return before createMimeMessage is reached.
        mimeMessage = new MimeMessage((Session) null);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // -------------------------------------------------------------------------
    // R22.1 sendPasswordResetEmail — happy path
    // -------------------------------------------------------------------------

    @Test
    void sendPasswordResetEmail_rendersPasswordResetTemplateAndSends() {
        when(emailTemplateEngine.process(eq("password-reset"), any(Context.class)))
                .thenReturn("<html>reset</html>");

        emailService.sendPasswordResetEmail("user@example.com", "tok-abc123");

        verify(emailTemplateEngine).process(eq("password-reset"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    // -------------------------------------------------------------------------
    // R22.1 sendWelcomeEmail — happy path
    // -------------------------------------------------------------------------

    @Test
    void sendWelcomeEmail_rendersWelcomeUserTemplateAndSends() {
        when(emailTemplateEngine.process(eq("welcome-user"), any(Context.class)))
                .thenReturn("<html>welcome</html>");

        emailService.sendWelcomeEmail("user@example.com", "Alice", "p@ssw0rd");

        verify(emailTemplateEngine).process(eq("welcome-user"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    // -------------------------------------------------------------------------
    // R22.1 sendBirthdayEmail — happy path
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmail_rendersBirthdayTemplateAndSends() {
        when(emailTemplateEngine.process(eq("birthday"), any(Context.class)))
                .thenReturn("<html>happy birthday</html>");

        emailService.sendBirthdayEmail("user@example.com", "Bob");

        verify(emailTemplateEngine).process(eq("birthday"), any(Context.class));
        verify(mailSender).send(mimeMessage);
    }

    // -------------------------------------------------------------------------
    // Swallow-vs-rethrow: send failure must NOT propagate (all three methods)
    // -------------------------------------------------------------------------

    @Test
    void sendPasswordResetEmail_sendFailure_doesNotPropagateException() {
        when(emailTemplateEngine.process(eq("password-reset"), any(Context.class)))
                .thenReturn("<html>reset</html>");
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendPasswordResetEmail("user@example.com", "tok-fail"));
    }

    @Test
    void sendWelcomeEmail_sendFailure_doesNotPropagateException() {
        when(emailTemplateEngine.process(eq("welcome-user"), any(Context.class)))
                .thenReturn("<html>welcome</html>");
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendWelcomeEmail("user@example.com", "Alice", "p@ss"));
    }

    @Test
    void sendBirthdayEmail_sendFailure_doesNotPropagateException() {
        when(emailTemplateEngine.process(eq("birthday"), any(Context.class)))
                .thenReturn("<html>birthday</html>");
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                emailService.sendBirthdayEmail("user@example.com", "Bob"));
    }

    // -------------------------------------------------------------------------
    // Early-return when fromEmail is blank — mailSender must not be touched
    // -------------------------------------------------------------------------

    @Test
    void sendPasswordResetEmail_blankFromEmail_doesNotCallMailSender() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        emailService.sendPasswordResetEmail("user@example.com", "tok-abc");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendWelcomeEmail_blankFromEmail_doesNotCallMailSender() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        emailService.sendWelcomeEmail("user@example.com", "Alice", "pass");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendBirthdayEmail_blankFromEmail_doesNotCallMailSender() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "");

        emailService.sendBirthdayEmail("user@example.com", "Bob");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // -------------------------------------------------------------------------
    // sendBirthdayEmail — null/empty userName falls back to "Valued Employee"
    // (template context variable is set, not tested at template level, but
    //  the call must still complete and dispatch)
    // -------------------------------------------------------------------------

    @Test
    void sendBirthdayEmail_nullUserName_completesWithoutException() {
        when(emailTemplateEngine.process(eq("birthday"), any(Context.class)))
                .thenReturn("<html>birthday</html>");

        assertDoesNotThrow(() -> emailService.sendBirthdayEmail("user@example.com", null));
        verify(mailSender).send(mimeMessage);
    }
}
