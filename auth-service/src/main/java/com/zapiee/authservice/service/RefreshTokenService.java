package com.zapiee.authservice.service;

import com.zapiee.authservice.dto.request.RefreshTokenRequest;
import com.zapiee.authservice.dto.response.AuthResponse;
import com.zapiee.authservice.dto.response.AuthTokens;
import com.zapiee.authservice.entity.RefreshToken;
import com.zapiee.authservice.entity.User;
import com.zapiee.authservice.exception.BusinessRuleViolationException;
import com.zapiee.authservice.exception.ResourceNotFoundException;
import com.zapiee.authservice.mapper.UserMapper;
import com.zapiee.authservice.repository.RefreshTokenRepository;
import com.zapiee.authservice.util.JwtUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthTokens refreshToken(String refreshTokenValue) {
        log.info("Refreshing access token");

        if (!jwtService.validateToken(refreshTokenValue, jwtService.extractUsername(refreshTokenValue))) {
            throw new BusinessRuleViolationException("Invalid refresh token", "INVALID_TOKEN");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found"));

        if (!refreshToken.isValid()) {
            throw new BusinessRuleViolationException("Refresh token is expired or revoked", "INVALID_TOKEN");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);

        return AuthTokens.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .user(user)
                .build();
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        log.info("Logging out user");

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void saveRefreshToken(User user, String token, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getAccessTokenExpirationInSeconds()))
                .ipAddress(ipAddress)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}
