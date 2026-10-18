package com.zapiee.orderservice.websocket;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains active WebSocket connections for the Order Service.
 *
 * <p>The manager maintains two independent connection groups:</p>
 *
 * <ul>
 *     <li>Customer connections grouped by restaurant + table number.</li>
 *     <li>Staff/Owner connections grouped by restaurant ID.</li>
 * </ul>
 *
 * <p>This class is intentionally responsible only for WebSocket
 * connection registration and lookup. Authentication, authorization,
 * session-token validation and order business logic are handled by
 * other components.</p>
 */
@Component
@Slf4j
@Tag(
        name = "WebSocket Session Manager",
        description = "Maintains active customer and staff WebSocket connections for real-time order updates."
)
public class WebSocketSessionManager {

    /*
     * ============================================================
     * CONSTANTS
     * ============================================================
     */

    private static final String KEY_SEPARATOR = "_";

    /*
     * ============================================================
     * ACTIVE CONNECTION REGISTRIES
     * ============================================================
     */

    /**
     * Customer WebSocket connections.
     *
     * Key:
     * restaurantId_tableNumber
     *
     * Example:
     *
     * 1_12 -> [customerSession1, customerSession2]
     */
    private final Map<String, Set<WebSocketSession>> tableSessions =
            new ConcurrentHashMap<>();

    /**
     * Staff/Owner WebSocket connections.
     *
     * Key:
     * restaurantId
     *
     * Example:
     *
     * 1 -> [staffSession1, ownerSession1]
     */
    private final Map<Long, Set<WebSocketSession>> staffSessions =
            new ConcurrentHashMap<>();


    /*
     * ============================================================
     * CUSTOMER SESSION MANAGEMENT
     * ============================================================
     */

    /**
     * Registers a customer WebSocket connection for a table.
     *
     * @param restaurantId restaurant identifier
     * @param tableNumber table number
     * @param session active WebSocket session
     */
    @Operation(
            summary = "Register customer WebSocket session",
            description = "Registers an active customer WebSocket connection against a restaurant table."
    )
    public void addTableSession(
            Long restaurantId,
            Long tableNumber,
            WebSocketSession session
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                tableNumber,
                "Table number"
        );

        validateSession(session);

        String key =
                buildKey(
                        restaurantId,
                        tableNumber
                );

        tableSessions
                .computeIfAbsent(
                        key,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(session);

        log.debug(
                "Customer WebSocket session registered | restaurantId={} | tableNumber={} | sessionId={}",
                restaurantId,
                tableNumber,
                session.getId()
        );
    }


    /**
     * Removes a customer WebSocket connection from a table.
     *
     * <p>If the table has no remaining connections, its registry
     * entry is also removed.</p>
     *
     * @param restaurantId restaurant identifier
     * @param tableNumber table number
     * @param session WebSocket session
     */
    @Operation(
            summary = "Remove customer WebSocket session",
            description = "Removes a customer WebSocket connection from a restaurant table."
    )
    public void removeTableSession(
            Long restaurantId,
            Long tableNumber,
            WebSocketSession session
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                tableNumber,
                "Table number"
        );

        validateSession(session);

        String key =
                buildKey(
                        restaurantId,
                        tableNumber
                );

        Set<WebSocketSession> sessions =
                tableSessions.get(key);

        if (sessions == null) {
            return;
        }

        sessions.remove(session);

        if (sessions.isEmpty()) {
            tableSessions.remove(
                    key,
                    sessions
            );
        }

        log.debug(
                "Customer WebSocket session removed | restaurantId={} | tableNumber={} | sessionId={}",
                restaurantId,
                tableNumber,
                session.getId()
        );
    }


    /**
     * Returns all currently registered customer sessions
     * for a restaurant table.
     *
     * <p>A defensive immutable snapshot is returned so callers
     * cannot directly modify the internal registry.</p>
     *
     * @param restaurantId restaurant identifier
     * @param tableNumber table number
     * @return active customer WebSocket sessions
     */
    @Operation(
            summary = "Get table WebSocket sessions",
            description = "Returns the currently registered customer WebSocket sessions for a restaurant table."
    )
    public Set<WebSocketSession> getTableSessions(
            Long restaurantId,
            Long tableNumber
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                tableNumber,
                "Table number"
        );

        String key =
                buildKey(
                        restaurantId,
                        tableNumber
                );

        Set<WebSocketSession> sessions =
                tableSessions.get(key);

        if (sessions == null ||
                sessions.isEmpty()) {

            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(
                Set.copyOf(sessions)
        );
    }


    /*
     * ============================================================
     * STAFF / OWNER SESSION MANAGEMENT
     * ============================================================
     */

    /**
     * Registers a staff/owner WebSocket connection.
     *
     * @param restaurantId restaurant identifier
     * @param session active WebSocket session
     */
    @Operation(
            summary = "Register staff WebSocket session",
            description = "Registers an active staff or owner WebSocket connection for a restaurant."
    )
    public void addStaffSession(
            Long restaurantId,
            WebSocketSession session
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validateSession(session);

        staffSessions
                .computeIfAbsent(
                        restaurantId,
                        ignored -> ConcurrentHashMap.newKeySet()
                )
                .add(session);

        log.debug(
                "Staff/Owner WebSocket session registered | restaurantId={} | sessionId={}",
                restaurantId,
                session.getId()
        );
    }


    /**
     * Removes a staff/owner WebSocket connection.
     *
     * <p>If the restaurant has no remaining connections,
     * its registry entry is removed.</p>
     *
     * @param restaurantId restaurant identifier
     * @param session WebSocket session
     */
    @Operation(
            summary = "Remove staff WebSocket session",
            description = "Removes a staff or owner WebSocket connection from a restaurant."
    )
    public void removeStaffSession(
            Long restaurantId,
            WebSocketSession session
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validateSession(session);

        Set<WebSocketSession> sessions =
                staffSessions.get(restaurantId);

        if (sessions == null) {
            return;
        }

        sessions.remove(session);

        if (sessions.isEmpty()) {
            staffSessions.remove(
                    restaurantId,
                    sessions
            );
        }

        log.debug(
                "Staff/Owner WebSocket session removed | restaurantId={} | sessionId={}",
                restaurantId,
                session.getId()
        );
    }


    /**
     * Returns all currently registered staff/owner sessions
     * for a restaurant.
     *
     * @param restaurantId restaurant identifier
     * @return active staff/owner WebSocket sessions
     */
    @Operation(
            summary = "Get staff WebSocket sessions",
            description = "Returns the currently registered staff and owner WebSocket sessions for a restaurant."
    )
    public Set<WebSocketSession> getStaffSessions(
            Long restaurantId
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        Set<WebSocketSession> sessions =
                staffSessions.get(restaurantId);

        if (sessions == null ||
                sessions.isEmpty()) {

            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(
                Set.copyOf(sessions)
        );
    }


    /*
     * ============================================================
     * CLEANUP
     * ============================================================
     */

    /**
     * Removes closed WebSocket sessions from both registries.
     *
     * <p>This method also removes empty restaurant/table buckets,
     * preventing unnecessary memory retention.</p>
     */
    @Operation(
            summary = "Cleanup closed WebSocket sessions",
            description = "Removes closed WebSocket sessions and empty registry buckets."
    )
    public void cleanupClosedSessions() {

        cleanupTableSessions();
        cleanupStaffSessions();

        log.debug(
                "Closed WebSocket session cleanup completed."
        );
    }


    /**
     * Removes closed customer sessions and empty table buckets.
     */
    private void cleanupTableSessions() {

        tableSessions.forEach(
                (key, sessions) -> {

                    sessions.removeIf(
                            session ->
                                    session == null ||
                                            !session.isOpen()
                    );

                    if (sessions.isEmpty()) {

                        tableSessions.remove(
                                key,
                                sessions
                        );
                    }
                }
        );
    }


    /**
     * Removes closed staff/owner sessions and empty restaurant buckets.
     */
    private void cleanupStaffSessions() {

        staffSessions.forEach(
                (restaurantId, sessions) -> {

                    sessions.removeIf(
                            session ->
                                    session == null ||
                                            !session.isOpen()
                    );

                    if (sessions.isEmpty()) {

                        staffSessions.remove(
                                restaurantId,
                                sessions
                        );
                    }
                }
        );
    }


    /*
     * ============================================================
     * OPTIONAL REGISTRY INFORMATION
     * ============================================================
     */

    /**
     * Returns whether at least one customer connection exists
     * for a restaurant table.
     */
    @Operation(
            summary = "Check table connection",
            description = "Checks whether a restaurant table currently has at least one customer WebSocket connection."
    )
    public boolean hasTableSessions(
            Long restaurantId,
            Long tableNumber
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                tableNumber,
                "Table number"
        );

        String key =
                buildKey(
                        restaurantId,
                        tableNumber
                );

        Set<WebSocketSession> sessions =
                tableSessions.get(key);

        return sessions != null &&
                sessions.stream()
                        .anyMatch(
                                session ->
                                        session != null &&
                                                session.isOpen()
                        );
    }


    /**
     * Returns whether at least one staff/owner connection exists
     * for a restaurant.
     */
    @Operation(
            summary = "Check staff connection",
            description = "Checks whether a restaurant currently has at least one staff or owner WebSocket connection."
    )
    public boolean hasStaffSessions(
            Long restaurantId
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        Set<WebSocketSession> sessions =
                staffSessions.get(restaurantId);

        return sessions != null &&
                sessions.stream()
                        .anyMatch(
                                session ->
                                        session != null &&
                                                session.isOpen()
                        );
    }


    /*
     * ============================================================
     * INTERNAL HELPERS
     * ============================================================
     */

    /**
     * Creates the internal table-session registry key.
     */
    private String buildKey(
            Long restaurantId,
            Long tableNumber
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                tableNumber,
                "Table number"
        );

        return restaurantId +
                KEY_SEPARATOR +
                tableNumber;
    }


    /**
     * Validates a WebSocket session.
     */
    private void validateSession(
            WebSocketSession session
    ) {

        if (session == null) {
            throw new IllegalArgumentException(
                    "WebSocket session cannot be null."
            );
        }

        if (!StringUtils.hasText(session.getId())) {
            throw new IllegalArgumentException(
                    "WebSocket session ID cannot be blank."
            );
        }
    }


    /**
     * Validates positive numeric identifiers.
     */
    private void validatePositiveId(
            Long value,
            String fieldName
    ) {

        if (value == null ||
                value <= 0) {

            throw new IllegalArgumentException(
                    fieldName +
                            " must be greater than zero."
            );
        }
    }
}
