package com.zapiee.tableservice.model;

import com.zapiee.tableservice.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "table_sessions",
        indexes = {
                @Index(name = "idx_session_token", columnList = "session_token"),
                @Index(name = "idx_session_restaurant_table", columnList = "restaurant_id, table_id"),
                @Index(name = "idx_session_status", columnList = "status"),
                @Index(name = "idx_session_expires_at", columnList = "expires_at")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_session_token", columnNames = "session_token")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "table_id", nullable = false)
    private Long tableId;

    @Column(nullable = false)
    private Long tableNumber;

    @Column(name = "session_token", nullable = false, unique = true, length = 128)
    private String sessionToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "order_completed_at")
    private LocalDateTime orderCompletedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = SessionStatus.ACTIVE;
        }
    }
}