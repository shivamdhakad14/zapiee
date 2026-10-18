package com.zapiee.authservice.service;

import com.zapiee.authservice.dto.request.LoginRequest;
import com.zapiee.authservice.dto.request.RegisterRequest;
import com.zapiee.authservice.dto.request.VerifyOtpRequest;
import com.zapiee.authservice.dto.response.AuthResponse;
import com.zapiee.authservice.dto.response.AuthTokens;
import com.zapiee.authservice.dto.response.UserInfo;
import com.zapiee.authservice.entity.Role;
import com.zapiee.authservice.entity.User;
import com.zapiee.authservice.enums.AccountStatus;
import com.zapiee.authservice.exception.BusinessRuleViolationException;
import com.zapiee.authservice.exception.DuplicateResourceException;
import com.zapiee.authservice.exception.EmailNotVerifiedException;
import com.zapiee.authservice.exception.ResourceNotFoundException;
import com.zapiee.authservice.mapper.UserMapper;
import com.zapiee.authservice.repository.RefreshTokenRepository;
import com.zapiee.authservice.repository.RoleRepository;
import com.zapiee.authservice.repository.UserRepository;
import com.zapiee.authservice.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountLockedException;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtService;
    private final RateLimitService rateLimitService;
    private final OtpService otpService;
    private final UserMapper userMapper;
    private final RefreshTokenService refreshTokenService;

    @Value("${security.account-lockout.max-attempts}")
    private Integer maxLoginAttempts;

    @Value("${security.account-lockout.lockout-duration-minutes}")
    private Integer lockoutDuration;

    @Transactional
    public UserInfo register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        rateLimitService.checkRegistrationRateLimit(request.getEmail());

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessRuleViolationException("Passwords do not match", HttpStatus.INTERNAL_SERVER_ERROR.toString());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Phone Number already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new BusinessRuleViolationException("Default role not found",
                        HttpStatus.INTERNAL_SERVER_ERROR.toString()));

        user.addRole(userRole);

        User save = userRepository.save(user);
        otpService.generateAndSendOtp(save.getEmail(), "EMAIL_VERIFICATION");

        return userMapper.mapToDto(save);
    }

    @Transactional
    public AuthTokens verifyOtp(VerifyOtpRequest request) {

        log.info("Verifying email with OTP: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessRuleViolationException(
                    "Email is already verified",
                    "ALREADY_VERIFIED"
            );
        }

        otpService.verifyOtp(
                user.getEmail(), request.getOtp(), "EMAIL_VERIFICATION"
        );

        user.setEmailVerified(true);
        user.setAccountStatus(AccountStatus.ACTIVE);
        User save = userRepository.save(user);

        log.info("Email verified successfully: {}", user.getEmail());
        return issueTokensAndPersist(save, save.getEmail());
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        log.info("Login attempt for: {}", request.getPhoneNumberOrEmail());
        String identifier = request.getPhoneNumberOrEmail();

        rateLimitService.checkLoginRateLimit(request.getPhoneNumberOrEmail());

        User user = userRepository.findByPhoneNumberOrEmail(identifier, identifier)
                .orElseThrow(() -> new BadCredentialsException("Invalid email/phone or password"));

        validateLoginCriteria(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid username or password");
        }

        return issueTokensAndPersist(user, identifier);
    }

    private AuthTokens issueTokensAndPersist(User user, String requestIdentifier) {
        user.resetFailedLoginAttempts();
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        refreshTokenService.saveRefreshToken(user, refreshToken, requestIdentifier);

        rateLimitService.resetLoginRateLimit(requestIdentifier);

        log.info("User logged in successfully: userId={}", user.getId());

        return AuthTokens.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private void validateLoginCriteria(User user){
        if (user.isAccountLocked()) {
            throw new LockedException("Account is temporarily locked due to multiple failed login attempts");
        }

        if (Boolean.FALSE.equals(user.getEmailVerified())
                || user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        switch (user.getAccountStatus()) {
            case SUSPENDED -> throw new DisabledException("Account has been suspended. Contact support.");
            case DEACTIVATED -> throw new DisabledException("This account has been deactivated");
            case LOCKED -> throw new LockedException("This account has been Locked");
        }
    }

    private void handleFailedLogin(User user) {
        user.incrementFailedLoginAttempts();

        if (user.getFailedLoginAttempts() >= maxLoginAttempts) {
            user.setAccountStatus(AccountStatus.LOCKED);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDuration));
            log.warn("Account locked for user: {}", user.getEmail());
        }

        userRepository.save(user);
    }

    public AuthResponse buildAuthResponse(User user, String accessToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .user(userMapper.mapToDto(user))
                .build();
    }

}
