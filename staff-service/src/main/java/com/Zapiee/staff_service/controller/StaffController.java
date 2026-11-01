
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
    public ResponseEntity<ApiResponse<StaffResponseDto>> createStaff(
           @Valid @RequestBody StaffRequestDto request) {

        log.info("Staff registration requested");

        StaffResponseDto response = service.createStaff(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(
                        "Staff account created successfully", response));
    }

//    @PostMapping("/login")
//    public ResponseEntity<AuthResponse> loginWithPin(
//            @Valid @RequestBody LoginIdentifierRequestDto request) {
//
//        return ResponseEntity.ok(service.loginWithPin(request));
//    }

    @PostMapping("/login")
    public ResponseEntity<?> loginWithPin(
           @Valid @RequestBody LoginIdentifierRequestDto request) {

        log.info("Login endpoint reached");
        log.info("Request body is null: {}", request == null);

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        AuthResponse response = service.loginWithPin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody(required = false) RefreshTokenRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid authorization header");
        }

        String accessToken = authHeader.substring(7);
        String refreshToken = request == null
                ? null : request.getRefreshToken();

        return ResponseEntity.ok(
                service.logout(refreshToken, accessToken));
    }

    @GetMapping
    public ResponseEntity<StaffResponseDto> getStaffByName(
            @RequestParam @NotBlank String name) {

        return ResponseEntity.ok(service.getStaffByName(name));
    }

    @GetMapping("/restaurant/{restaurantId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<List<StaffResponseDto>> getAllByRestaurant(
            @PathVariable Long restaurantId) {

        return ResponseEntity.ok(
                service.getStaffByRestaurant(restaurantId));
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
    public ResponseEntity<StaffResponseDto> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffRequestDto request) {

        return ResponseEntity.ok(service.updateStaff(id, request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        return ResponseEntity.ok(service.refreshToken(request));
    }

    @GetMapping("/health")
    public ResponseEntity<MessageResponse> health() {
        return ResponseEntity.ok(
                MessageResponse.success("Staff service is running"));
    }
}
