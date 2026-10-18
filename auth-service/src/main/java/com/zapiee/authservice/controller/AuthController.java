package com.zapiee.authservice.controller;

import com.zapiee.authservice.dto.request.LoginRequest;
import com.zapiee.authservice.dto.request.RefreshTokenRequest;
import com.zapiee.authservice.dto.request.RegisterRequest;
import com.zapiee.authservice.dto.request.VerifyOtpRequest;
import com.zapiee.authservice.dto.response.ApiResponse;
import com.zapiee.authservice.dto.response.AuthResponse;
import com.zapiee.authservice.dto.response.AuthTokens;
import com.zapiee.authservice.dto.response.UserInfo;
import com.zapiee.authservice.service.AuthService;
import com.zapiee.authservice.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserInfo>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());
        UserInfo user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(user, "User account created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {

        log.info("Login request received for email: {}", request.getPhoneNumberOrEmail());
        AuthTokens tokens = authService.login(request);
        setRefreshTokenCookie(httpResponse, tokens.getRefreshToken());

        AuthResponse body = authService.buildAuthResponse(tokens.getUser(), tokens.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success(body, "Login successful"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletResponse httpResponse) {

        AuthTokens tokens = authService.verifyOtp(request);
        setRefreshTokenCookie(httpResponse, tokens.getRefreshToken());

        AuthResponse body = authService.buildAuthResponse(tokens.getUser(), tokens.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success(body, "Email verified"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken,
            HttpServletResponse httpResponse) {

        AuthTokens authResponse = refreshTokenService.refreshToken(refreshToken);
        setRefreshTokenCookie(httpResponse, authResponse.getRefreshToken());

        AuthResponse body = authService.buildAuthResponse(authResponse.getUser(), authResponse.getAccessToken());
        return ResponseEntity.ok(ApiResponse.success(body, "Token refreshed"));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse httpResponse) {

        log.info("Logout request received");
        String accessToken = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7)
                : authHeader;

        refreshTokenService.logout(refreshToken, accessToken);
        clearRefreshTokenCookie(httpResponse);

        return ResponseEntity.ok(ApiResponse.noContent("Logout successful"));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}