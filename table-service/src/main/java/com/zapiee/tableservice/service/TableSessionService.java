package com.zapiee.tableservice.service;

import com.zapiee.tableservice.dto.response.TableResolveResponse;
import com.zapiee.tableservice.dto.response.TableSessionResponse;
import com.zapiee.tableservice.enums.SessionStatus;
import com.zapiee.tableservice.exception.BadRequestException;
import com.zapiee.tableservice.exception.SessionNotFoundException;
import com.zapiee.tableservice.model.TableSession;
import com.zapiee.tableservice.repository.TableSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableSessionService {

    private final TableSessionRepository tableSessionRepository;

    @Transactional
    public TableResolveResponse createSession(Long restaurantId, Long tableId, Long tableNumber) {

        validateIds(restaurantId, tableId);

        LocalDateTime now = LocalDateTime.now();

        TableSession session = tableSessionRepository
                        .findActiveSessionForUpdate(restaurantId, tableId, SessionStatus.ACTIVE)
                        .orElse(null);

        if (session != null) {

            if (isExpired(session, now)) {

                session.setStatus(SessionStatus.EXPIRED);
                tableSessionRepository.save(session);

                log.info("Existing table session expired. restaurantId={}, tableId={}, sessionId={}",
                        restaurantId, tableId, session.getId());

            } else {
                log.info("Existing table session returned. restaurantId={}, tableId={}, sessionId={}",
                        restaurantId, tableId, session.getId());

                return mapToResponse(session);
            }
        }

        TableSession newSession = TableSession.builder()
                .restaurantId(restaurantId)
                .tableId(tableId)
                .tableNumber(tableNumber)
                .sessionToken(generateSessionToken())
                .status(SessionStatus.ACTIVE)
                .build();

        TableSession savedSession = tableSessionRepository.save(newSession);

        log.info("New table session created. restaurantId={}, tableId={}, tableNumber={}, sessionId={}",
                restaurantId, tableId, tableNumber, savedSession.getId());

        return mapToResponse(savedSession);
    }


    @Transactional
    public void markOrderCompleted(Long restaurantId, Long tableId) {

        validateIds(restaurantId, tableId);

        TableSession session = tableSessionRepository
                        .findActiveSessionForUpdate(restaurantId, tableId, SessionStatus.ACTIVE)
                        .orElseThrow(() -> new SessionNotFoundException("Active table session not found."));

        LocalDateTime completedAt = LocalDateTime.now();

        LocalDateTime expiresAt = completedAt.plusMinutes(2);

        session.setOrderCompletedAt(completedAt);
        session.setExpiresAt(expiresAt);

        tableSessionRepository.save(session);

        log.info("Order completion registered for table session. restaurantId={}, tableId={}, sessionId={}, expiresAt={}",
                restaurantId, tableId, session.getId(), expiresAt);
    }


    @Transactional(readOnly = true)
    public TableSessionResponse getSession(String sessionToken) {

        validateSessionToken(sessionToken);

        String normalizedToken = sessionToken.trim();

        TableSession session = tableSessionRepository
                .findBySessionToken(normalizedToken)
                .orElseThrow(() -> new SessionNotFoundException("Table session not found."));

        LocalDateTime now = LocalDateTime.now();

        if (session.getStatus() == SessionStatus.EXPIRED) {
            throw new SessionNotFoundException("Table session has expired.");
        }

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new BadRequestException("Table session is not active.");
        }

        if (isExpired(session, now)) {
            throw new SessionNotFoundException("Table session has expired.");
        }
        validateSessionData(session);

        log.debug("Table session resolved successfully. sessionId={}, restaurantId={}, tableId={}",
                session.getId(), session.getRestaurantId(), session.getTableId());

        return mapToSessionResponse(session);
    }

    private TableSessionResponse mapToSessionResponse(TableSession session) {
        return TableSessionResponse.builder()
                .sessionId(session.getId())
                .restaurantId(session.getRestaurantId())
                .tableId(session.getTableId())
                .tableNumber(session.getTableNumber())
                .sessionToken(session.getSessionToken())
                .sessionStatus(session.getStatus())
                .expiresAt(session.getExpiresAt())
                .build();
    }


    private void validateSessionToken(String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new BadRequestException("Session token is required.");
        }

        if (sessionToken.trim().length() > 100) {
            throw new BadRequestException("Invalid session token.");
        }
    }


    private void validateSessionData(TableSession session) {

        if (session.getId() == null) {
            throw new BadRequestException("Session ID is missing.");
        }

        if (session.getRestaurantId() == null || session.getRestaurantId() <= 0) {
            throw new BadRequestException("Invalid restaurant ID in table session.");
        }

        if (session.getTableId() == null || session.getTableId() <= 0) {
            throw new BadRequestException("Invalid table ID in table session.");
        }

        if (session.getSessionToken() == null || session.getSessionToken().isBlank()) {
            throw new BadRequestException("Session token is missing.");
        }

        if (session.getStatus() == null) {
            throw new BadRequestException("Table session status is missing.");
        }
    }


    @Transactional
    public void expireSession(String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new BadRequestException(
                    "Session token is required."
            );
        }

        TableSession session = tableSessionRepository
                        .findBySessionToken(sessionToken)
                        .orElseThrow(() -> new SessionNotFoundException("Table session not found."));

        if (session.getStatus() == SessionStatus.EXPIRED) {
            return;
        }

        session.setStatus(SessionStatus.EXPIRED);
        tableSessionRepository.save(session);

        log.info("Table session expired. sessionId={}, tableId={}", session.getId(), session.getTableId());
    }

    private boolean isExpired(TableSession session, LocalDateTime now) {
        return session.getExpiresAt() != null
                && !now.isBefore(session.getExpiresAt());
    }

    private String generateSessionToken() {
        return UUID.randomUUID().toString();
    }

    private void validateIds(Long restaurantId, Long tableId) {

        if (restaurantId == null || restaurantId <= 0) {
            throw new BadRequestException("Valid restaurant ID is required.");
        }

        if (tableId == null || tableId <= 0) {
            throw new BadRequestException("Valid table ID is required.");
        }
    }

    private TableResolveResponse mapToResponse(TableSession session) {
        return TableResolveResponse.builder()
                .restaurantId(session.getRestaurantId())
                .tableId(session.getTableId())
                .sessionToken(session.getSessionToken())
                .sessionExpiresAt(session.getExpiresAt())
                .build();
    }

}