package com.vatek.hrmtoolnextgen.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            // Check if email is configured
            if (fromEmail == null || fromEmail.isEmpty()) {
                log.warn("Email not configured. Password reset token for {}: {}", toEmail, resetToken);
                log.warn("Reset link: {}/reset-password?token={}", frontendUrl, resetToken);
                return; // Don't throw exception, just log for development
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Password Reset Request");

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            // Prepare template variables
            Context context = new Context();
            context.setVariable("resetLink", resetLink);
            context.setVariable("resetToken", resetToken);

            // Process the Thymeleaf template
            String htmlContent = emailTemplateEngine.process("password-reset", context);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);

            log.info("Password reset email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}. Token: {}", toEmail, resetToken, e);
            // In development, log the token instead of failing
            log.warn("Reset link: {}/reset-password?token={}", frontendUrl, resetToken);
            // Don't throw exception to allow development without email server
            // In production, you might want to throw: throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending password reset email to: {}. Token: {}", toEmail, resetToken, e);
            log.warn("Reset link: {}/reset-password?token={}", frontendUrl, resetToken);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName, String password) {
        try {
            // Check if email is configured
            if (fromEmail == null || fromEmail.isEmpty()) {
                log.warn("Email not configured. Welcome email for {} - Email: {}, Password: {}", toEmail, toEmail, password);
                return; // Don't throw exception, just log for development
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Welcome to HRM Tool - Your Account Credentials");

            String loginUrl = frontendUrl + "/login";

            // Prepare template variables
            Context context = new Context();
            context.setVariable("userName", userName != null ? userName : "User");
            context.setVariable("email", toEmail);
            context.setVariable("password", password);
            context.setVariable("loginUrl", loginUrl);

            // Process the Thymeleaf template
            String htmlContent = emailTemplateEngine.process("welcome-user", context);
            helper.setText(htmlContent, true); // true = isHtml

            mailSender.send(message);

            log.info("Welcome email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}. Password: {}", toEmail, password, e);
            // Don't throw exception to allow development without email server
            // In production, you might want to throw: throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to: {}. Password: {}", toEmail, password, e);
        }
    }
}

