package com.zapiee.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zapiee.orderservice.websocket.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Order WebSocket Push Service",
        description = "Handles real-time order update broadcasting to customer and staff/owner WebSocket clients."
)
public class WebSocketPushService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;



    @Operation(
            summary = "Broadcast message to table",
            description = "Sends a WebSocket message to all connected customers of a restaurant table."
    )
    public void broadcastToTable(Long restaurantId,
            Long tableNumber,
            Object message
    ) {

        if (!isPositiveId(restaurantId)) {

            log.warn(
                    "Table WebSocket broadcast skipped: invalid restaurantId={}",
                    restaurantId
            );

            return;
        }

        if (!isPositiveId(tableNumber)) {

            log.warn(
                    "Table WebSocket broadcast skipped: invalid tableNumber={}",
                    tableNumber
            );

            return;
        }

        if (message == null) {

            log.warn(
                    "Table WebSocket broadcast skipped: message is null | restaurantId={} | tableNumber={}",
                    restaurantId,
                    tableNumber
            );

            return;
        }

        String jsonMessage;

        try {

            jsonMessage =
                    serializeMessage(message);

        } catch (JsonProcessingException ex) {

            log.error(
                    "Unable to serialize table WebSocket message | restaurantId={} | tableNumber={}",
                    restaurantId,
                    tableNumber,
                    ex
            );

            return;
        }

        Set<WebSocketSession> sessions =
                sessionManager.getTableSessions(
                        restaurantId,
                        tableNumber
                );

        if (sessions.isEmpty()) {

            log.debug(
                    "No active customer WebSocket sessions found | restaurantId={} | tableNumber={}",
                    restaurantId,
                    tableNumber
            );

            return;
        }

        sendToSessions(
                sessions,
                jsonMessage,
                "TABLE",
                restaurantId,
                tableNumber
        );
    }


    // ============================================================
    // STAFF / OWNER BROADCAST
    // ============================================================

    /**
     * Broadcasts a message to all STAFF and OWNER clients
     * connected to a restaurant.
     *
     * <p>
     * Example:
     *
     * <pre>
     * Restaurant 1
     *     |
     *     +-- Staff A
     *     +-- Staff B
     *     +-- Owner
     * </pre>
     */
    @Operation(
            summary = "Broadcast message to restaurant staff",
            description = "Sends a WebSocket message to all connected staff and owner clients of a restaurant."
    )
    public void broadcastToStaff(
            Long restaurantId,
            Object message
    ) {

        if (!isPositiveId(restaurantId)) {

            log.warn(
                    "Staff WebSocket broadcast skipped: invalid restaurantId={}",
                    restaurantId
            );

            return;
        }

        if (message == null) {

            log.warn(
                    "Staff WebSocket broadcast skipped: message is null | restaurantId={}",
                    restaurantId
            );

            return;
        }

        String jsonMessage;

        try {

            jsonMessage =
                    serializeMessage(message);

        } catch (JsonProcessingException ex) {

            log.error(
                    "Unable to serialize staff WebSocket message | restaurantId={}",
                    restaurantId,
                    ex
            );

            return;
        }

        Set<WebSocketSession> sessions =
                sessionManager.getStaffSessions(
                        restaurantId
                );

        if (sessions.isEmpty()) {

            log.debug(
                    "No active staff/owner WebSocket sessions found | restaurantId={}",
                    restaurantId
            );

            return;
        }

        sendToSessions(
                sessions,
                jsonMessage,
                "STAFF",
                restaurantId,
                null
        );
    }


    // ============================================================
    // COMMON SEND LOGIC
    // ============================================================

    /**
     * Sends the already serialized JSON message to the supplied
     * WebSocket sessions.
     *
     * <p>
     * Each session is handled independently.
     *
     * <p>
     * If one client is disconnected or fails to receive the message,
     * other clients will still receive it.
     */
    private void sendToSessions(
            Set<WebSocketSession> sessions,
            String jsonMessage,
            String audience,
            Long restaurantId,
            Long tableNumber
    ) {

        if (sessions == null ||
                sessions.isEmpty()) {

            return;
        }

        TextMessage textMessage =
                new TextMessage(jsonMessage);

        for (WebSocketSession session : sessions) {

            if (session == null) {
                continue;
            }

            if (!session.isOpen()) {

                log.debug(
                        "Skipping closed WebSocket session | audience={} | sessionId={}",
                        audience,
                        session.getId()
                );

                continue;
            }

            sendMessage(
                    session,
                    textMessage,
                    audience,
                    restaurantId,
                    tableNumber
            );
        }
    }


    /**
     * Sends one message to one WebSocket session.
     */
    private void sendMessage(
            WebSocketSession session,
            TextMessage message,
            String audience,
            Long restaurantId,
            Long tableNumber
    ) {

        try {

            /*
             * A second open-state check prevents sending to a session
             * that was closed between the previous check and send.
             */
            if (!session.isOpen()) {
                return;
            }

            synchronized (session) {

                if (!session.isOpen()) {
                    return;
                }

                session.sendMessage(message);
            }

            log.debug(
                    "WebSocket message sent successfully | audience={} | sessionId={} | restaurantId={} | tableNumber={}",
                    audience,
                    session.getId(),
                    restaurantId,
                    tableNumber
            );

        } catch (Exception ex) {

            /*
             * Do not fail the entire broadcast because one client
             * disconnected or encountered an I/O error.
             */
            log.warn(
                    "WebSocket message delivery failed | audience={} | sessionId={} | restaurantId={} | tableNumber={}",
                    audience,
                    session.getId(),
                    restaurantId,
                    tableNumber,
                    ex
            );

            /*
             * Remove the failed session from the manager.
             *
             * This prevents repeatedly attempting to send messages
             * to a dead WebSocket connection.
             */
            if (tableNumber != null) {

                sessionManager.removeTableSession(
                        restaurantId,
                        tableNumber,
                        session
                );

            } else {

                sessionManager.removeStaffSession(
                        restaurantId,
                        session
                );
            }
        }
    }


    // ============================================================
    // JSON SERIALIZATION
    // ============================================================

    /**
     * Converts the outgoing WebSocket object into JSON.
     */
    private String serializeMessage(
            Object message
    ) throws JsonProcessingException {

        return objectMapper.writeValueAsString(
                message
        );
    }


    // ============================================================
    // VALIDATION
    // ============================================================

    /**
     * Validates positive database/business identifiers.
     */
    private boolean isPositiveId(
            Long value
    ) {

        return value != null &&
                value > 0;
    }
}
