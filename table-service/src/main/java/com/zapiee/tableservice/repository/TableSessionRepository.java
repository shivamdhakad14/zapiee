package com.zapiee.tableservice.repository;

import com.zapiee.tableservice.enums.SessionStatus;
import com.zapiee.tableservice.model.TableSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findByRestaurantIdAndTableIdAndStatus(Long restaurantId, Long tableId, SessionStatus status);

    Optional<TableSession> findBySessionToken(String sessionToken);

    @Query("""
            SELECT s
            FROM TableSession s
            WHERE s.status = :status
              AND s.expiresAt IS NOT NULL
              AND s.expiresAt <= :now
            """)
    java.util.List<TableSession> findExpiredSessions(@Param("status") SessionStatus status, @Param("now") LocalDateTime now);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM TableSession s
            WHERE s.restaurantId = :restaurantId
              AND s.tableId = :tableId
              AND s.status = :status
            """)
    Optional<TableSession> findActiveSessionForUpdate(@Param("restaurantId") Long restaurantId, @Param("tableId") Long tableId, @Param("status") SessionStatus status);


}