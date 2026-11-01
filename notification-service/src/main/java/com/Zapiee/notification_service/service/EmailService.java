package com.Zapiee.notification_service.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateEngineException;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String TEMPLATE_DIR = "email/";
    private static final Pattern SAFE_TEMPLATE_NAME = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String DEFAULT_SUBJECT = "OrderZap Notification";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.mail.from}")
    private String fromAddress;

    @Value("${notification.mail.from-name:OrderZap}")
    private String fromName;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Sends an HTML email rendered from templates/email/{templateName}.html.
     * plainText is optional but recommended (multipart/alternative).
     * The returned future completes exceptionally if delivery fails.
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendHtmlEmail(String to, String subject, String templateName,
                                                 Map<String, Object> variables, String plainText) {

        if (!emailEnabled) {
            log.info("[EMAIL DISABLED] subject='{}' recipient={}", subject, mask(to));
            return CompletableFuture.completedFuture(null);
        }
        if (!hasText(to)) {
            log.warn("Email skipped: empty recipient for subject '{}'", subject);
            return CompletableFuture.completedFuture(null);
        }
        if (templateName == null || !SAFE_TEMPLATE_NAME.matcher(templateName).matches()) {
            throw new IllegalArgumentException("Invalid template name: " + templateName);
        }

        try {
            Context context = new Context(Locale.ENGLISH);
            if (variables != null) {
                context.setVariables(variables);
            }
            String html = templateEngine.process(TEMPLATE_DIR + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to.trim());
            helper.setSubject(cleanSubject(subject));

            if (hasText(plainText)) {
                helper.setText(plainText, html);   // plain-text part + HTML part
            } else {
                helper.setText(html, true);
            }

            mailSender.send(message);
            log.info("HTML email sent. recipient={}, template={}", mask(to), templateName);
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException | MailException | UnsupportedEncodingException
                 | TemplateEngineException ex) {
            log.error("HTML email failed. recipient={}, template={}", mask(to), templateName, ex);
            throw new IllegalStateException("Email delivery failed for " + mask(to), ex);
        }
    }

    /** Plain-text only email (use for simple messages, not as a fallback). */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendSimpleEmail(String to, String subject, String body) {

        if (!emailEnabled || !hasText(to)) {
            return CompletableFuture.completedFuture(null);
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to.trim());
            helper.setSubject(cleanSubject(subject));
            helper.setText(body == null ? "" : body, false);

            mailSender.send(message);
            log.info("Plain email sent. recipient={}", mask(to));
            return CompletableFuture.completedFuture(null);

        } catch (MessagingException | MailException | UnsupportedEncodingException ex) {
            log.error("Plain email failed. recipient={}", mask(to), ex);
            throw new IllegalStateException("Email delivery failed for " + mask(to), ex);
        }
    }

    private String cleanSubject(String subject) {
        if (!hasText(subject)) return DEFAULT_SUBJECT;
        return subject.replaceAll("[\\r\\n]+", " ").trim();   // prevent header injection
    }

    private String mask(String email) {
        if (email == null) return "null";
        int at = email.indexOf('@');
        return at <= 1 ? "***" : email.charAt(0) + "***" + email.substring(at);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}