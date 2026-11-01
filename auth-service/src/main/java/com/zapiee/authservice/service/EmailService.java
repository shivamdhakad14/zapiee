package com.zapiee.authservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendEmailVerificationOtp(String email, String otp, int expirationMinutes) {
        log.info("Sending email verification OTP to: {}", email);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("noreply@example.com");
            helper.setTo(email);
            helper.setSubject("Email Verification Code");

            Context context = new Context();
            context.setVariable("otp", otp);
            context.setVariable("expirationMinutes", expirationMinutes);
            context.setVariable("purpose", "Email Verification");

            String htmlContent = templateEngine.process("email.html", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email verification OTP sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send email verification OTP to: {}", email, e);
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
