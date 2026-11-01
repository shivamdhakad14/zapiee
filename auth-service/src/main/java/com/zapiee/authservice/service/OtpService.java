package com.zapiee.authservice.service;

import com.zapiee.authservice.exception.BusinessRuleViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;

    @Value("${otp.length:6}")
    private Integer otpLength;

    @Value("${otp.expiration-minutes:10}")
    private Integer otpExpirationMinutes;

    @Value("${otp.max-attempts:3}")
    private Integer maxVerificationAttempts;

    private static final String OTP_PREFIX = "otp:email:";
    private static final String OTP_ATTEMPTS_PREFIX = "otp:attempts:";
    private static final SecureRandom random = new SecureRandom();

    public void generateAndSendOtp(String email, String purpose) {
        log.info("Generating OTP for email: {}", email);

        rateLimitService.checkEmailVerificationRateLimit(email);
        String otp = generateOtp();

        String key = getOtpKey(email, purpose);
        redisTemplate.opsForValue().set(
                key,
                otp,
                Duration.ofMinutes(otpExpirationMinutes)
        );

        String attemptsKey = getAttemptsKey(email, purpose);
        redisTemplate.delete(attemptsKey);

        sendOtpEmail(email, otp, purpose);

        log.info("OTP generated and sent to: {}", email);
    }

    public boolean verifyOtp(String email, String otp, String purpose) {
        log.info("Verifying OTP for email: {}", email);

        String key = getOtpKey(email, purpose);
        String attemptsKey = getAttemptsKey(email, purpose);

        String storedOtp = redisTemplate.opsForValue().get(key);
        if (storedOtp == null) {
            log.warn("OTP expired or not found for email: {}", email);
            throw new BusinessRuleViolationException("OTP has expired or doesn't exist. " +
                    "Please request a new one.", "OTP_EXPIRED");
        }

        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= maxVerificationAttempts) {
            log.warn("Maximum OTP verification attempts exceeded for email: {}", email);
            redisTemplate.delete(key);
            redisTemplate.delete(attemptsKey);
            throw new BusinessRuleViolationException("Maximum verification attempts exceeded." +
                    " Please request a new OTP.", "OTP_ATTEMPTS_EXCEEDED");
        }

        if (!otp.equals(storedOtp)) {
            if (attempts == 0) {
                redisTemplate.opsForValue().set(attemptsKey, "1", Duration.ofMinutes(otpExpirationMinutes));
            } else {
                redisTemplate.opsForValue().increment(attemptsKey);
            }

            log.warn("Invalid OTP provided for email: {}", email);
            throw new BusinessRuleViolationException(
                    String.format("Invalid OTP. %d attempts remaining.", maxVerificationAttempts - attempts - 1), "OTP_INVALID"
            );
        }

        redisTemplate.delete(key);
        redisTemplate.delete(attemptsKey);

        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    public void resendOtp(String email, String purpose) {
        log.info("Resending OTP for email: {}", email);

        rateLimitService.checkEmailVerificationRateLimit(email);

        String key = getOtpKey(email, purpose);
        String existingOtp = redisTemplate.opsForValue().get(key);

        if (existingOtp != null) {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl != null && ttl > (otpExpirationMinutes * 60L / 2)) {
                sendOtpEmail(email, existingOtp, purpose);
                log.info("Resent existing OTP to: {}", email);
                return;
            }
        }
        generateAndSendOtp(email, purpose);
    }

    private String generateOtp() {
        int otp = random.nextInt((int) Math.pow(10, otpLength));
        return String.format("%0" + otpLength + "d", otp);
    }

    private void sendOtpEmail(String email, String otp, String purpose) {
        if(purpose.equalsIgnoreCase("EMAIL_VERIFICATION")){
            emailService.sendEmailVerificationOtp(email, otp, otpExpirationMinutes);
        }
    }

    private String getOtpKey(String email, String purpose) {
        return OTP_PREFIX + purpose + ":" + email;
    }

    private String getAttemptsKey(String email, String purpose) {
        return OTP_ATTEMPTS_PREFIX + purpose + ":" + email;
    }

    public boolean isOtpValid(String email, String purpose) {
        String key = getOtpKey(email, purpose);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

}
