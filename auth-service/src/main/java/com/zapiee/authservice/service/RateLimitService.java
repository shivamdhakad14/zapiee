package com.zapiee.authservice.service;

import com.zapiee.authservice.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${rate-limit.login.max-attempts:5}")
    private Integer loginMaxAttempts;

    @Value("${rate-limit.login.duration-seconds:300}")
    private Integer loginDurationSeconds;

    @Value("${rate-limit.registration.max-attempts:3}")
    private Integer registrationMaxAttempts;

    @Value("${rate-limit.registration.duration-seconds:3600}")
    private Integer registrationDurationSeconds;

    @Value("${rate-limit.password-reset.max-attempts:3}")
    private Integer passwordResetMaxAttempts;

    @Value("${rate-limit.password-reset.duration-seconds:3600}")
    private Integer passwordResetDurationSeconds;

    @Value("${rate-limit.email-verification.max-attempts:5}")
    private Integer emailVerificationMaxAttempts;

    @Value("${rate-limit.email-verification.duration-seconds:3600}")
    private Integer emailVerificationDurationSeconds;

    private static final String LOGIN_RATE_LIMIT_PREFIX = "rate_limit:login:";
    private static final String REGISTRATION_RATE_LIMIT_PREFIX = "rate_limit:registration:";
    private static final String PASSWORD_RESET_RATE_LIMIT_PREFIX = "rate_limit:password_reset:";
    private static final String EMAIL_VERIFICATION_RATE_LIMIT_PREFIX = "rate_limit:email_verification:";
    private static final String API_RATE_LIMIT_PREFIX = "rate_limit:api:";

    public void checkLoginRateLimit(String identifier) {
        checkRateLimit(
                LOGIN_RATE_LIMIT_PREFIX + identifier,
                loginMaxAttempts,
                loginDurationSeconds,
                "Too many login attempts. Please try again later."
        );
    }

    public void checkRegistrationRateLimit(String identifier) {
        checkRateLimit(
                REGISTRATION_RATE_LIMIT_PREFIX + identifier,
                registrationMaxAttempts,
                registrationDurationSeconds,
                "Too many registration attempts. Please try again later."
        );
    }

    public void checkEmailVerificationRateLimit(String email) {
        checkRateLimit(
                EMAIL_VERIFICATION_RATE_LIMIT_PREFIX + email,
                emailVerificationMaxAttempts,
                emailVerificationDurationSeconds,
                "Too many verification email requests. Please try again later."
        );
    }

    public void resetLoginRateLimit(String identifier) {
        resetRateLimit(LOGIN_RATE_LIMIT_PREFIX + identifier);
    }


    public void resetRateLimit(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (deleted) {
                log.debug("Rate limit reset for key: {}", key);
            }
        } catch (Exception e) {
            log.error("Error resetting rate limit for key: {}", key, e);
        }
    }

    private void checkRateLimit(String key, int maxAttempts, int durationSeconds, String errorMessage) {
        try {
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;

            log.debug("Rate limit check: key={}, current={}, max={}", key, currentCount, maxAttempts);

            if (currentCount >= maxAttempts) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                log.warn("Rate limit exceeded for key: {}, TTL: {}s", key, ttl);

                throw new RateLimitExceededException(
                        errorMessage + " Try again in " + ttl + " seconds."
                );
            }

            if (currentCount == 0) {
                redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(durationSeconds));
            } else {
                redisTemplate.opsForValue().increment(key);
            }

            log.debug("Rate limit incremented: key={}, new count={}", key, currentCount + 1);

        } catch (RateLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error checking rate limit for key: {}", key, e);
        }
    }
}
