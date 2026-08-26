package com.minhpt.hrmtoolnextgen.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine emailTemplateEngine;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${hrm.app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private void sendEmail(String to, String subject, String template, Map<String, Object> vars) {
        try {
            if (fromEmail == null || fromEmail.isEmpty()) {
                log.warn("Email not configured. Template '{}' for recipient: {}", template, to);
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            Context context = new Context();
            if (vars != null) {
                vars.forEach(context::setVariable);
            }

            String htmlContent = emailTemplateEngine.process(template, context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email (template: {}) to: {}", template, to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending email (template: {}) to: {}", template, to, e);
        }
    }

    @Async("emailTaskExecutor")
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Email not configured. Password reset email for {} skipped.", toEmail);
            return;
        }

        sendEmail(toEmail, "Password Reset Request", "password-reset", Map.of(
                "resetLink", resetLink,
                "resetToken", resetToken
        ));

        if (fromEmail != null && !fromEmail.isEmpty()) {
            log.info("Password reset email sent successfully to: {}", toEmail);
        }
    }

    @Async("emailTaskExecutor")
    public void sendWelcomeEmail(String toEmail, String userName, String password) {
        String loginUrl = frontendUrl + "/login";

        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Email not configured. Welcome email for {}", toEmail);
            return;
        }

        sendEmail(toEmail, "Welcome to HRM Tool - Your Account Credentials", "welcome-user", Map.of(
                "userName", userName != null ? userName : "User",
                "email", toEmail,
                "loginUrl", loginUrl
        ));

        if (fromEmail != null && !fromEmail.isEmpty()) {
            log.info("Welcome email sent successfully to: {}", toEmail);
        }
    }

    @Async("emailTaskExecutor")
    public void sendBirthdayEmail(String toEmail, String userName) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Email not configured. Birthday email for {} ({})", toEmail, userName);
            return;
        }

        sendEmail(toEmail, "Happy Birthday!", "birthday", Map.of(
                "userName", userName != null && !userName.isEmpty() ? userName : "Valued Employee",
                "frontendUrl", frontendUrl
        ));

        log.info("Birthday email sent successfully to: {} ({})", toEmail, userName);
    }

    @Async("emailTaskExecutor")
    public void sendApprovalNotificationEmail(String toEmail, String userName, String type, String status) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            log.warn("Email not configured. Approval notification for {}: {} -> {}", toEmail, type, status);
            return;
        }

        sendEmail(toEmail, "Your " + type + " has been " + status, "approval-notification", Map.of(
                "userName", userName != null && !userName.isEmpty() ? userName : "User",
                "type", type,
                "status", status,
                "frontendUrl", frontendUrl
        ));

        log.info("Approval notification email sent to: {} for {} -> {}", toEmail, type, status);
    }
}
