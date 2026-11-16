
package com.Zapiee.staff_service.controller;

import com.Zapiee.staff_service.dto.*;
import com.Zapiee.staff_service.service.StaffService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staffs")
@Validated
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService service;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<StaffResponseDto>> createStaff(@RequestHeader("X-Restaurant-Id") Long restaurantId,
           @Valid @RequestBody StaffRequestDto request) {

        log.info("Staff registration requested");

        StaffResponseDto response = service.createStaff(request, restaurantId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response,
                        "Staff account created successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithPin(
           @Valid @RequestBody LoginIdentifierRequestDto request) {
        log.info("Request body is null: {}", request == null);

        AuthResponse response = service.loginWithPin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authorization header");
        }

        String accessToken = authHeader.substring(7);
        String refreshToken = request == null
                ? null : request.getRefreshToken();

        service.logout(refreshToken, accessToken);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StaffResponseDto>> getStaffByName(
            @RequestParam @NotBlank String name) {
        StaffResponseDto staffByName = service.getStaffByName(name);
        return ResponseEntity.ok(ApiResponse.success(staffByName));
    }

    @GetMapping("/restaurant")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<StaffResponseDto>>> getAllByRestaurant(
            @RequestHeader("X-Restaurant-Id") Long restaurantId) {
        List<StaffResponseDto> staffByRestaurant = service.getStaffByRestaurant(restaurantId);
        return ResponseEntity.ok(ApiResponse.success(staffByRestaurant));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<MessageResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {

        return ResponseEntity.ok(service.requestPasswordReset(request));
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request) {

        return ResponseEntity.ok(service.resetPassword(request));
    }

    @PostMapping("/change-pin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request , Authentication authentication) {

        String email = authentication.getName();
        return ResponseEntity
                .ok(service.changePassword(request, email));
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<MessageResponse> deleteStaff(
            @PathVariable Long id) {

        service.deleteStaff(id);

        return ResponseEntity.ok(
                MessageResponse.success("Staff deleted successfully"));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<StaffResponseDto>> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffRequestDto request) {
        StaffResponseDto staffResponseDto = service.updateStaff(id, request);
        return ResponseEntity.ok(ApiResponse.success(staffResponseDto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = service.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(authResponse));
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(
                MessageResponse.success("Staff service is running"));
    }
}
