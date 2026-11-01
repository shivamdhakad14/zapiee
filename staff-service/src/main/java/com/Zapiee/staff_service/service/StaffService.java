package com.Zapiee.staff_service.service;

import com.Zapiee.staff_service.client.RestaurantClient;
import com.Zapiee.staff_service.dto.*;
import com.Zapiee.staff_service.enums.NotificationChannel;
import com.Zapiee.staff_service.enums.NotificationStatus;
import com.Zapiee.staff_service.enums.NotificationType;
import com.Zapiee.staff_service.event.NotificationEvent;
import com.Zapiee.staff_service.exception.*;
import com.Zapiee.staff_service.model.RefreshToken;
import com.Zapiee.staff_service.model.Staff;
import com.Zapiee.staff_service.model.StaffRole;
import com.Zapiee.staff_service.repository.RefreshTokenRepository;
import com.Zapiee.staff_service.repository.RoleRepository;
import com.Zapiee.staff_service.repository.StaffRepository;
import com.Zapiee.staff_service.util.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaffService {

    @Value("${security.account-lockout.max-attempts}")
    private Integer maxLoginAttempts;

    @Value("${security.account-lockout.lockout-duration-minutes}")
    private Integer lockoutDuration;

    private final StaffRepository staffRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RestaurantClient restaurantClient;
    private final JwtUtil jwtService;

//    @Autowired
//    private KafkaProducerService kafkaProducerService;



    @Transactional
    public AuthResponse loginWithPin(LoginIdentifierRequestDto request) {

        if (request == null
                || request.getEmailOrMobile() == null
                || request.getEmailOrMobile().isBlank()
                || request.getPin() == null
                || request.getPin().isBlank()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String identifier = request.getEmailOrMobile().trim();
        log.info("Staff login attempt received");

        Staff staff = staffRepository
                .findByMobileOrEmail(identifier, identifier)
                .orElseThrow(() ->
                        new org.springframework.security.authentication
                                .BadCredentialsException("Invalid credentials"));

        if (staff.isCurrentlyLocked()) {
            throw new AccountLockedException(
                    "Account is temporarily locked");
        }

        if (!Boolean.TRUE.equals(staff.getAccountEnabled())) {
            throw new AccountDisabledException("Account is disabled");
        }

        boolean validPin = passwordEncoder.matches(
                String.valueOf(request.getPin()),
                staff.getPin());

        if (!validPin) {
            handleFailedLogin(staff);

            throw new org.springframework.security.authentication
                    .BadCredentialsException("Invalid credentials");
        }

        staff.resetFailedLoginAttempts();
        staff.setLastLogin(LocalDateTime.now());
        staffRepository.save(staff);

        String accessToken = jwtService.generateAccessToken(staff);
        String refreshToken = jwtService.generateRefreshToken(staff);

        saveRefreshToken(staff, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .restaurantId(staff.getRestaurantId())
                .staffInfo(buildStaffInfo(staff))
                .build();
    }


    @Transactional
    public MessageResponse logout(String refreshToken, String accessToken) {
        log.info("Logging out user");

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });

        return MessageResponse.success("Staff Logged out successfully");
    }



    @Transactional
    public AuthResponse refreshToken(
            RefreshTokenRequest refreshTokenRequest) {

        String token = refreshTokenRequest.getRefreshToken();

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Refresh token is required");
        }

        RefreshToken storedToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid refresh token"));

        if (!storedToken.isValid()) {
            throw new InvalidTokenException(
                    "Refresh token expired or revoked");
        }

        Staff staff = storedToken.getStaff();

        if (staff == null
                || !Boolean.TRUE.equals(staff.getAccountEnabled())
                || staff.isCurrentlyLocked()) {
            throw new InvalidTokenException(
                    "Staff account is unavailable");
        }

        // Implement this check in JwtUtil. A refresh JWT must not
        // be accepted as an access JWT, or vice versa.
//        if (!jwtService.isRefreshToken(token)
//                || !jwtService.validateToken(
//                token, jwtService.extractUsername(token))) {
//            throw new InvalidTokenException("Invalid refresh token");
//        }
        String email = jwtService.extractUsername(token);
        String tokenUserId = jwtService.extractUserId(token);

        if (!jwtService.isRefreshToken(token)
                || email == null
                || email.isBlank()
                || tokenUserId == null
                || !tokenUserId.equals(String.valueOf(staff.getId()))
                || !email.equalsIgnoreCase(staff.getEmail())
                || jwtService.isTokenExpired(token)) {

            throw new InvalidTokenException("Invalid refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(staff);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationInSeconds())
                .restaurantId(staff.getRestaurantId())
                .staffInfo(buildStaffInfo(staff))
                .build();
    }




    private void saveRefreshToken(Staff staff, String token) {

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .staff(staff)
                .expiresAt(
                        LocalDateTime.now().plusSeconds(
                                jwtService.getRefreshTokenExpirationInSeconds()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }

    public Long getValidatedRestaurantId(Long restaurantId) {

        ApiResponse<RestaurantResponse> response =
                restaurantClient.getRestaurantById(restaurantId);

        if (response == null
                || !response.isSuccess()
                || response.getData() == null) {

            throw new IllegalArgumentException(
                    "Restaurant not found or restaurant response is invalid."
            );
        }

        return response.getData().getId();
    }

    @Transactional
    public StaffResponseDto createStaff(StaffRequestDto request) {

        if (staffRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (staffRepository.existsByMobile(request.getMobile())) {
            throw new IllegalArgumentException("Mobile already registered");
        }

        if (request.getPin() == null
                || !request.getPin().equals(request.getConfirmPin())) {
            throw new IllegalArgumentException("PINs do not match");
        }

        Long restaurantId = getValidatedRestaurantId(request.getRestaurantId());

        StaffRole role = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Default staff role is not configured"));

        LocalDateTime now = LocalDateTime.now();

        Staff staff = Staff.builder()
                .name(request.getName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .shift(request.getShift())
                .status(request.getStatus())
                .pin(passwordEncoder.encode(request.getPin()))
                .salary(request.getSalary())
                .workingStaffRole(request.getWorkingStaffRole())
                .restaurantId(restaurantId)
                .roles(Set.of(role))
                .createdAt(now)
                .build();

       Staff savedStaff =  staffRepository.save(staff);

        log.info("Staff account created successfully");

//            NotificationEvent event = NotificationEvent.builder()
//                    .userId(staff.getId())
//                    .referenceId(UUID.randomUUID().toString())
//                    .email(restaurant.getBusinessEmail())
//                    .title("New Staff Created")
//                    .message("Welcome " + staff.getName() + " to the restaurant team.")
//                    .type(NotificationType.STAFF_CREATED)
//                    .status(NotificationStatus.PENDING)
//                    .channel(NotificationChannel.EMAIL)
//                    .payload(Map.of(
//                            "name", staff.getName(),
//                            "workingRole", staff.getWorkingStaffRole()
//                    ))
//                    .build();
//
//            kafkaProducerService.sendNotification(event);

        StaffResponseDto response = new StaffResponseDto();
        response.setId(savedStaff.getId());
        response.setName(savedStaff.getName());
        response.setMobile(savedStaff.getMobile());
        response.setSalary(savedStaff.getSalary());
        response.setWorkingStaffRole(savedStaff.getWorkingStaffRole());
        response.setStatus(savedStaff.getStatus().name());
        response.setShift(savedStaff.getShift().name());
        response.setRestaurantId(savedStaff.getRestaurantId());
        response.setCreatedAt(savedStaff.getCreatedAt());

        return response;

    }

    @Transactional
    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());

        Staff staff = staffRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        staff.setPinResetToken(resetToken);
        staffRepository.save(staff);

        return MessageResponse.success("Password reset email sent");
    }

    @Transactional
    public MessageResponse resetPassword(PasswordResetConfirmRequest request) {
        log.info("Resetting password");

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Passwords do not match");
        }

        Staff staff = staffRepository.findByPinResetToken(request.getToken())
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        staff.setPin(passwordEncoder.encode(request.getNewPassword()));
        staff.setPinResetToken(null);
        staff.setLastPinChange(LocalDateTime.now());
        staffRepository.save(staff);

        log.info("Password reset successfully for user: {}", staff.getEmail());
        return MessageResponse.success("Password reset successfully");
    }

    @Transactional
    public MessageResponse changePassword(
            ChangePasswordRequest request,
            String email) {

        log.info("Password change requested");

        Staff staff = staffRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        // Verify the current password against the stored hash.
        if (!passwordEncoder.matches(
                request.getCurrentPin(),
                staff.getPin())) {

            throw new BadCredentialsException(
                    "Current password is incorrect");
        }
        // Ensure the new password and confirmation match.
        if (!request.getNewPin()
                .equals(request.getConfirmPin())) {
            throw new PasswordMismatchException(
                    "New password and confirmation do not match");
        }

        // Prevent changing to the same password.
        if (passwordEncoder.matches(
                request.getNewPin(),
                staff.getPin())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password");
        }

        // Store the encoded password, never plain text.
        staff.setPin(passwordEncoder.encode(request.getNewPin()));
        staff.setPinResetToken(null);
        staff.setLastPinChange(LocalDateTime.now());

        staffRepository.save(staff);

        log.info("Password changed successfully");

        return MessageResponse.success(
                "Password changed successfully");
    }


    @Transactional
    public StaffResponseDto updateStaff(Long id, StaffRequestDto dto) {

        Staff staff = staffRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Staff not found"));

        staffRepository.findByEmail(dto.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Email already registered");
                });

        staffRepository.findByMobile(dto.getMobile())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Mobile already registered");
                });

        staff.setName(dto.getName());
        staff.setEmail(dto.getEmail());
        staff.setMobile(dto.getMobile());
        staff.setSalary(dto.getSalary());
        staff.setStatus(dto.getStatus());
        staff.setShift(dto.getShift());
        staff.setWorkingStaffRole(dto.getWorkingStaffRole());
        staff.setRestaurantId(dto.getRestaurantId());
        staff.setUpdatedAt(LocalDateTime.now());

        // Never update the PIN as a side effect of profile editing.

        return toResponse(staffRepository.save(staff));
    }



    public StaffResponseDto getStaffById(Long id){
        Staff staff = staffRepository.findById(id)
                .orElseThrow( () -> new RuntimeException("Staff not found"));

        return toResponse(staff);
    }

    public List<StaffResponseDto> getStaffByRestaurant(Long RestaurantId){
        return staffRepository.findByRestaurantId(RestaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteStaff(Long id){
        Staff staff = staffRepository.findById(id)
                        .orElseThrow(() ->
                                new EntityNotFoundException("Staff not found"));
        log.info("Staff delete attempt for: {}", staff.getId());

        staffRepository.delete(staff);

//        kafkaProducerService.sendNotification(
//                new NotificationEvent(
//                        staff.getId(),
//                        UUID.randomUUID().toString(),
//                        staff.getEmail(),
//                        "Staff Deleted",
//                        "Staff " + staff.getName() + " is successfully deleted.",
//                        NotificationType.STAFF_DELETED,
//                        NotificationStatus.PENDING,
//                        NotificationChannel.EMAIL
//                )
//        );
    }

    private void handleFailedLogin(Staff staff) {
        staff.incrementFailedLoginAttempts();

        if (staff.getFailedLoginAttempts() >= maxLoginAttempts) {
            staff.setAccountNonLocked(false);
            staff.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDuration));
            log.warn("Account locked for user: {}", staff.getName());
        }

        staffRepository.save(staff);
    }

    //MAPPER
    public Staff mapToEntity(StaffRequestDto dto){

        StaffRole role = roleRepository.findByName(dto.getName())
                .orElseThrow(() -> new RuntimeException("Role not found"));

        Staff staff = new Staff();

        staff.setName(dto.getName());
        staff.setEmail(dto.getEmail());
        staff.setMobile(dto.getMobile());
        staff.setSalary(dto.getSalary());
        staff.setPin(passwordEncoder.encode(dto.getName()));
        staff.setRoles(Set.of(role));
        staff.setStatus(dto.getStatus());
        staff.setShift(dto.getShift());
        staff.setRestaurantId(dto.getRestaurantId());

        return staff;
    }

    public StaffResponseDto toResponse(Staff staff) {

         StaffResponseDto dto = new StaffResponseDto();
         dto.setId(staff.getId());
        dto.setName(staff.getName());
        dto.setMobile(staff.getMobile());
        dto.setSalary(staff.getSalary());
        dto.setShift(String.valueOf(staff.getShift()));
        dto.setStatus(String.valueOf(staff.getStatus()));
        dto.setRestaurantId(staff.getRestaurantId());
        dto.setCreatedAt(staff.getCreatedAt());
        dto.setWorkingStaffRole(staff.getWorkingStaffRole());

        //extract role from Set<StaffRole>
//        if (staff.getRole() != null && !staff.getRole().isEmpty()){
//            StaffRole role = staff.getRole().iterator().next();
//            dto.setWorkingRole(role.getName());
//        }

        return dto;
    }


    public StaffInfo buildStaffInfo(Staff staff) {

        return StaffInfo.builder()
                .id(staff.getId())
                .email(staff.getEmail())
                .name(staff.getName())
                .createdAt(staff.getCreatedAt())
                .restaurantId(staff.getRestaurantId())
                .roles(staff.getRoles().stream()
                        .map(StaffRole::getName)
                        .collect(Collectors.toSet()))
                .build();
    }


    public StaffResponseDto getStaffByName(String name) {
        Staff staff = staffRepository.findStaffByName(name)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(staff);
    }

}
