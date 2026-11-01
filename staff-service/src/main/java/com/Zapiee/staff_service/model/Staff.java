package com.Zapiee.staff_service.model;

import com.Zapiee.staff_service.enums.StaffShift;
import com.Zapiee.staff_service.enums.StaffStatus;
import com.Zapiee.staff_service.enums.WorkingStaffRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@Table(
        name = "staff_members",
        indexes = {
                @Index(name = "idx_staff_restaurant_id", columnList = "restaurant_id"),
                @Index(name = "idx_staff_email", columnList = "email"),
                @Index(name = "idx_staff_mobile", columnList = "mobile")
        }
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 15, unique = true) // widened from 10 — some numbers with country code exceed 10 digits
    private String mobile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "staff_role",
            joinColumns = @JoinColumn(name = "staff_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<StaffRole> roles = new java.util.HashSet<>();

    @OneToMany(mappedBy = "staff",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private Set<RefreshToken> refreshTokens = new java.util.HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StaffStatus status;

    @Enumerated(EnumType.STRING)
    private StaffShift shift;

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(nullable = false, length = 100)
    private String pin;

    @Column(name = "working_hours")
    private String workingHours;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkingStaffRole workingStaffRole;

    // Stores a SHA-256 hash of the reset token, never the raw token.
    @Column(name = "pin_reset_token", length = 128)
    private String pinResetToken;

    @Column(name = "pin_reset_token_expiry")
    private LocalDateTime pinResetTokenExpiry;

    @Column(name = "last_pin_change")
    private LocalDateTime lastPinChange;

    @Column(name = "account_non_locked")
    @Builder.Default
    private Boolean accountNonLocked = true;

    @Column(name = "account_enabled")
    @Builder.Default
    private Boolean accountEnabled = true;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Optimistic locking — protects concurrent updates to failedLoginAttempts / status
    // from a POS terminal + an admin panel hitting the same row at once.
    @Version
    private Long version;

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public boolean isCurrentlyLocked() {
        if (Boolean.FALSE.equals(accountNonLocked) && lockedUntil == null) {
            return true; // manually locked, no expiry
        }
        return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
    }

    /** Call explicitly from the service when you want to clear an expired lock. */
    public void unlockIfExpired() {
        if (lockedUntil != null && LocalDateTime.now().isAfter(lockedUntil)) {
            this.accountNonLocked = true;
            this.lockedUntil = null;
            this.failedLoginAttempts = 0;
        }
    }
}