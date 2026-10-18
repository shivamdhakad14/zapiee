package com.zapiee.orderservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zapiee.orderservice.client.TableSessionClient;
import com.zapiee.orderservice.config.WebSocketAuthInterceptor;
import com.zapiee.orderservice.dto.request.AddItemRequest;
import com.zapiee.orderservice.dto.request.RemoveItemRequest;
import com.zapiee.orderservice.dto.request.UpdateOrderStatusRequest;
import com.zapiee.orderservice.dto.request.WsMessage;
import com.zapiee.orderservice.dto.response.OrderConnectionResponse;
import com.zapiee.orderservice.dto.response.OrderListResponse;
import com.zapiee.orderservice.dto.response.OrderSnapshotResponse;
import com.zapiee.orderservice.dto.response.TableSessionResponse;
import com.zapiee.orderservice.dto.response.WebSocketErrorResponse;
import com.zapiee.orderservice.enums.MessageType;
import com.zapiee.orderservice.enums.Role;
import com.zapiee.orderservice.enums.SessionStatus;
import com.zapiee.orderservice.exception.ErrorCode;
import com.zapiee.orderservice.exception.OrderServiceException;
import com.zapiee.orderservice.exception.RestaurantAccessDeniedException;
import com.zapiee.orderservice.exception.SessionClosedException;
import com.zapiee.orderservice.exception.SessionExpiredException;
import com.zapiee.orderservice.exception.SessionNotFoundException;
import com.zapiee.orderservice.exception.TableMismatchException;
import com.zapiee.orderservice.service.OrderService;
import com.zapiee.orderservice.websocket.WebSocketSessionManager;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.zapiee.orderservice.config.WebSocketAuthInterceptor.ATTRIBUTE_SESSION;

@Component
@RequiredArgsConstructor
@Slf4j
@Schema(name = "Order WebSocket Handler",
        description = "Handles customer, staff and owner WebSocket connections and order operations."
)
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTRIBUTE_ROLE = "role";
    private static final String ATTRIBUTE_RESTAURANT_ID = "restaurantId";
    private static final String ATTRIBUTE_SESSION_TOKEN = "sessionToken";

    private static final String ATTRIBUTE_TABLE_ID = "tableId";
    private static final String ATTRIBUTE_TABLE_NUMBER = "tableNumber";
    private static final String ATTRIBUTE_SESSION_ID = "sessionId";

    private final WebSocketSessionManager manager;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final TableSessionClient tableSessionClient;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String traceId = generateTraceId();

        try {
            if (session == null) {
                log.warn("WebSocket connection rejected | traceId={}", traceId);
                return;
            }

            Role role = normalizeRole(session.getAttributes()
                                    .get(ATTRIBUTE_ROLE));

            if (role == null) {
                log.warn("WebSocket connection rejected: missing/invalid role | sessionId={} | traceId={}",
                        session.getId(), traceId);

                sendErrorAndClose(session, "Invalid WebSocket role.",
                        ErrorCode.INVALID_ROLE, traceId);
                return;
            }

            session.getAttributes().put(ATTRIBUTE_ROLE, role);
            log.info("WebSocket connected | sessionId={} | role={} | traceId={}",
                    session.getId(), role, traceId);

            switch (role) {
                case CUSTOMER ->
                        handleCustomerConnection(
                                session,
                                traceId
                        );

                case STAFF, OWNER ->
                        handleStaffOwnerConnection(
                                session,
                                role,
                                traceId
                        );
            }

        } catch (OrderServiceException ex) {

            log.warn("WebSocket connection business error | sessionId={} | errorCode={} | traceId={}",
                    safeSessionId(session), ex.getErrorCode().getCode(), traceId, ex);

            sendErrorAndClose(session, ex.getMessage(), ex.getErrorCode(), traceId);

        } catch (Exception ex) {

            log.error("Unexpected WebSocket connection error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex);

            sendErrorAndClose(session, "Unable to establish WebSocket connection.",
                    ErrorCode.INTERNAL_SERVER_ERROR, traceId);
        }
    }


    private void handleCustomerConnection(WebSocketSession session, String traceId) throws IOException {

        TableSessionResponse tableSession = getStoredTableSession(session);

        String sessionToken = getRequiredStringAttribute(session,
                        ATTRIBUTE_SESSION_TOKEN, ErrorCode.WEBSOCKET_SESSION_INVALID);

        log.info("Customer WebSocket authenticated | " +
                        "restaurantId={} | tableId={} | tableNumber={} | " +
                        "sessionId={} | sessionToken={} | wsSessionId={} | traceId={}",
                tableSession.getRestaurantId(), tableSession.getTableId(),
                tableSession.getTableNumber(), tableSession.getSessionId(),
                maskToken(sessionToken), session.getId(), traceId);

        session.getAttributes().put(ATTRIBUTE_RESTAURANT_ID, tableSession.getRestaurantId());

        session.getAttributes().put("tableId", tableSession.getTableId());
        session.getAttributes().put("tableNumber", tableSession.getTableNumber());
        session.getAttributes().put("sessionId", tableSession.getSessionId());

        manager.addTableSession(tableSession.getRestaurantId(),
                tableSession.getTableNumber(), session);

        sendCustomerSnapshot(session, tableSession, traceId);
    }

    private void handleStaffOwnerConnection(WebSocketSession session, Role role, String traceId) throws IOException {

        validateStaffOwnerRole(role);

        Long restaurantId = getRequiredLongAttribute(session,
                        ATTRIBUTE_RESTAURANT_ID, ErrorCode.RESTAURANT_ACCESS_DENIED);

        if (restaurantId <= 0) {
            throw new RestaurantAccessDeniedException("Invalid restaurant ID.");
        }

        log.info("{} WebSocket connected | restaurantId={} | sessionId={} | traceId={}",
                role, restaurantId, session.getId(), traceId);

        manager.addStaffSession(restaurantId, session);

        List<OrderListResponse> runningOrders = orderService.getRunningOrders(restaurantId);

        send(session, new WsMessage<>(MessageType.STAFF_ORDER_SNAPSHOT.name(), runningOrders));
    }

    private TableSessionResponse getStoredTableSession(WebSocketSession session) {

        if (session == null) {
            throw new OrderServiceException("WebSocket session is null.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID);
        }

        Object restaurantIdValue = session.getAttributes().get("restaurantId");
        Object tableIdValue = session.getAttributes().get("tableId");
        Object tableNumberValue = session.getAttributes().get("tableNumber");
        Object sessionIdValue = session.getAttributes().get("sessionId");
        Object sessionTokenValue = session.getAttributes().get("sessionToken");

        if (restaurantIdValue == null || tableIdValue == null || tableNumberValue == null ||
                sessionIdValue == null || sessionTokenValue == null) {
            throw new OrderServiceException("Required table session attributes are missing from WebSocket session.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID);
        }

        try {
            Long restaurantId = Long.valueOf(restaurantIdValue.toString());
            Long tableId = Long.valueOf(tableIdValue.toString());
            Long tableNumber = Long.valueOf(tableNumberValue.toString());
            Long sessionId = Long.valueOf(sessionIdValue.toString());

            String sessionToken = sessionTokenValue.toString();

            if (restaurantId <= 0 || tableId <= 0 || tableNumber <= 0 ||
                    sessionId <= 0 || sessionToken.isBlank()) {
                throw new OrderServiceException("Invalid table session attributes.",
                        ErrorCode.WEBSOCKET_SESSION_INVALID);
            }

            return TableSessionResponse.builder()
                    .restaurantId(restaurantId)
                    .tableId(tableId)
                    .tableNumber(tableNumber)
                    .sessionId(sessionId)
                    .sessionToken(sessionToken)
                    .sessionStatus(SessionStatus.ACTIVE)
                    .build();

        } catch (NumberFormatException ex) {
            throw new OrderServiceException("Invalid numeric table session attribute.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID, ex);
        }
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        String traceId = generateTraceId();

        try {
            validateIncomingMessage(session, message);
            Role role = normalizeRole(session.getAttributes().get(ATTRIBUTE_ROLE));

            if (role == null) {
                throw new OrderServiceException("WebSocket role is missing or invalid.",
                        ErrorCode.WEBSOCKET_SESSION_INVALID);
            }

            WsMessage<?> wsMessage = parseWebSocketMessage(message.getPayload());
            MessageType messageType = parseMessageType(wsMessage.getType());

            validateMessageAllowedForRole(role, messageType);

            switch (role) {

                case CUSTOMER -> handleCustomerMessage(
                        session, messageType, wsMessage, traceId);

                case STAFF, OWNER -> handleStaffOwnerMessage(
                        session, role, messageType, wsMessage, traceId);
            }

        } catch (OrderServiceException ex) {

            log.warn("WebSocket business error | sessionId={} | errorCode={} | traceId={}",
                    safeSessionId(session), ex.getErrorCode().getCode(), traceId, ex);

            sendError(session, ex.getMessage(), ex.getErrorCode(), traceId);

        } catch (JsonProcessingException ex) {

            log.warn("Invalid WebSocket JSON | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex);

            sendError(session, "Invalid WebSocket message format.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID, traceId);

        } catch (Exception ex) {

            log.error("Unexpected WebSocket message error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex);

            sendError(session, "Unable to process WebSocket message.",
                    ErrorCode.INTERNAL_SERVER_ERROR, traceId);
        }
    }


    private void handleCustomerMessage(WebSocketSession session, MessageType messageType,
                                       WsMessage<?> wsMessage, String traceId) {

        TableSessionResponse tableSession =
                getValidatedStoredTableSession(session, traceId);
        String sessionToken = tableSession.getSessionToken();

        switch (messageType) {

            case ADD_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request);
                orderService.placeFirstOrder(sessionToken, request);
            }

            case NEW_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request);
                orderService.addNewItems(sessionToken, request);
            }

            case REMOVE_ITEM -> {
                RemoveItemRequest request = convertPayload(
                        wsMessage.getPayload(), RemoveItemRequest.class);

                validateRemoveItemRequest(request);
                orderService.removeItem(sessionToken, request);
            }

            default -> throw new OrderServiceException(
                    "Message type '" + messageType +
                            "' is not allowed for CUSTOMER.", ErrorCode.FORBIDDEN);
        }

        log.info("Customer WebSocket operation completed | restaurantId={} | tableId={} | sessionId={} | messageType={} | traceId={}",
                tableSession.getRestaurantId(), tableSession.getTableId(), tableSession.getSessionId(),
                messageType, traceId);
    }


    private void handleStaffOwnerMessage(WebSocketSession session, Role role,
                                         MessageType messageType, WsMessage<?> wsMessage, String traceId) {

        validateStaffOwnerRole(role);

        Long restaurantId = getRequiredLongAttribute(session,
                ATTRIBUTE_RESTAURANT_ID, ErrorCode.RESTAURANT_ACCESS_DENIED);

        switch (messageType) {

            case UPDATE_STATUS -> {
                UpdateOrderStatusRequest request = convertPayload(
                        wsMessage.getPayload(), UpdateOrderStatusRequest.class);

                validateUpdateOrderStatusRequest(request);
                orderService.updateStatusByOrderId(restaurantId, request.getId(), request.getStatus());
            }

            case ADD_ITEM, NEW_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request);
                orderService.addItemsByRestaurant(restaurantId, request, messageType);
            }

            case REMOVE_ITEM -> {
                RemoveItemRequest request = convertPayload(
                        wsMessage.getPayload(), RemoveItemRequest.class);

                validateRemoveItemRequest(request);
                orderService.removeItemByRestaurant(restaurantId, request);
            }

            default -> throw new OrderServiceException(
                    "Message type '" + messageType +
                            "' is not allowed for " +
                            role + ".", ErrorCode.FORBIDDEN);
        }

        log.info("{} WebSocket operation completed | restaurantId={} | messageType={} | traceId={}",
                role, restaurantId, messageType, traceId);
    }


    private TableSessionResponse resolveTableSession(String sessionToken, String traceId) {

        try {

            TableSessionResponse response = tableSessionClient.getSession(sessionToken);

            if (response == null) {
                throw new SessionNotFoundException("Table session was not found.");
            }

            return response;

        } catch (OrderServiceException ex) {

            throw ex;

        } catch (Exception ex) {

            log.error("Unable to resolve table session | sessionToken={} | traceId={}",
                    maskToken(sessionToken), traceId, ex);

            throw new OrderServiceException("Unable to validate table session.",
                    ErrorCode.TABLE_SERVICE_UNAVAILABLE, ex);
        }
    }


    private void validateTableSession(TableSessionResponse response, String requestedToken) {

        if (response == null) {
            throw new SessionNotFoundException("Table session was not found.");
        }

        if (response.getSessionId() == null || response.getSessionId() <= 0) {
            throw new SessionNotFoundException("Table session ID is missing or invalid.");
        }

        if (response.getRestaurantId() == null || response.getRestaurantId() <= 0) {
            throw new TableMismatchException("Invalid restaurant information in table session.");
        }

        if (response.getTableId() == null || response.getTableId() <= 0) {
            throw new TableMismatchException("Invalid table information in table session.");
        }

        if (response.getTableNumber() == null || response.getTableNumber() <= 0) {
            throw new TableMismatchException("Invalid table number in table session.");
        }

        if (response.getSessionToken() == null || response.getSessionToken().isBlank()) {
            throw new SessionNotFoundException("Table session token is missing.");
        }

        if (!response.getSessionToken().equals(requestedToken)) {
            throw new TableMismatchException("Table session token mismatch.");
        }

        if (response.getSessionStatus() == null) {
            throw new SessionNotFoundException("Table session status is missing.");
        }

        if (response.getSessionStatus() == SessionStatus.EXPIRED) {
            throw new SessionExpiredException("Table session has expired.");
        }

        if (response.getSessionStatus() == SessionStatus.CLOSED) {
            throw new SessionClosedException("Table session is closed.");
        }

        if (response.getSessionStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotFoundException("Table session is not active.");
        }

//        if (response.getExpiresAt() != null && !response.getExpiresAt().isAfter(LocalDateTime.now())) {
//            throw new SessionExpiredException("Table session has expired.");
//        }
    }


    private TableSessionResponse getValidatedStoredTableSession(WebSocketSession session,
                                                                String traceId) {

        String sessionToken = getRequiredStringAttribute(session,
                ATTRIBUTE_SESSION_TOKEN, ErrorCode.WEBSOCKET_SESSION_INVALID);

        TableSessionResponse tableSession = resolveTableSession(sessionToken, traceId);
        validateTableSession(tableSession, sessionToken);

        return tableSession;
    }


    private void validateIncomingMessage(WebSocketSession session, TextMessage message) {

        if (session == null || !session.isOpen()) {
            throw new OrderServiceException("WebSocket session is closed.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID);
        }

        if (message == null || message.getPayload() == null ||
                message.getPayload().isBlank()) {
            throw new OrderServiceException("WebSocket message cannot be empty.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        if (message.getPayload().length() > 100_000) {
            throw new OrderServiceException("WebSocket message is too large.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }
    }


    private WsMessage<?> parseWebSocketMessage(String payload) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(payload);

        if (root == null || !root.isObject()) {
            throw new OrderServiceException("WebSocket message must be a JSON object.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        JsonNode typeNode = root.get("type");

        if (typeNode == null || typeNode.isNull() ||
                typeNode.asText().isBlank()) {
            throw new OrderServiceException("WebSocket message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        JsonNode payloadNode = root.get("payload");

        if (payloadNode == null || payloadNode.isNull()) {
            throw new OrderServiceException("WebSocket message payload is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        return objectMapper.treeToValue(root, WsMessage.class);
    }


    private <T> T convertPayload(Object payload, Class<T> targetClass) {

        if (payload == null) {
            throw new OrderServiceException("WebSocket payload cannot be null.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        try {

            T result = objectMapper.convertValue(payload, targetClass);

            if (result == null) {
                throw new OrderServiceException("Invalid WebSocket payload.",
                        ErrorCode.WEBSOCKET_MESSAGE_INVALID);
            }

            return result;

        } catch (IllegalArgumentException ex) {
            throw new OrderServiceException("Invalid WebSocket payload.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID, ex);
        }
    }


    private MessageType parseMessageType(String type) {

        if (type == null || type.isBlank()) {
            throw new OrderServiceException("Message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        try {

            return MessageType.valueOf(type.trim().toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException ex) {
            throw new OrderServiceException(
                    "Unsupported WebSocket message type: " + type,
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }
    }


    private void validateMessageAllowedForRole(Role role, MessageType messageType) {

        if (role == null) {
            throw new OrderServiceException("WebSocket role is required.",
                    ErrorCode.INVALID_ROLE
            );
        }

        if (messageType == null) {
            throw new OrderServiceException("Message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID);
        }

        switch (role) {

            case CUSTOMER -> {
                if (messageType == MessageType.UPDATE_STATUS ||
                        messageType == MessageType.STAFF_ORDER_SNAPSHOT) {
                    throw new OrderServiceException(
                            "This message type is not allowed for customers.",
                            ErrorCode.FORBIDDEN);
                }
            }

            case STAFF, OWNER -> {
                if (messageType == MessageType.ORDER_SNAPSHOT) {
                    throw new OrderServiceException(
                            "This message type is not allowed for Staff/Owner.",
                            ErrorCode.FORBIDDEN);
                }
            }
        }
    }


    private void validateStaffOwnerRole(Role role) {

        if (role != Role.STAFF && role != Role.OWNER) {
            throw new OrderServiceException("Only STAFF or OWNER can use restaurant-level WebSocket.",
                    ErrorCode.INVALID_ROLE);
        }
    }


    private Role normalizeRole(Object roleAttribute) {

        if (roleAttribute == null) {
            return null;
        }

        if (roleAttribute instanceof Role role) {
            return role;
        }

        String value = roleAttribute.toString().trim();

        if (value.isBlank()) {
            return null;
        }

        try {

            return Role.valueOf(value.toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid WebSocket role: {}", value);
            return null;
        }
    }


    private void validateAddItemRequest(AddItemRequest request) {

        if (request == null || request.getItems() == null ||
                request.getItems().isEmpty()) {
            throw new OrderServiceException("At least one order item is required.",
                    ErrorCode.INVALID_ORDER_ITEM);
        }

        if (request.getItems().size() > 100) {
            throw new OrderServiceException("A maximum of 100 items can be submitted in one request.",
                    ErrorCode.INVALID_ORDER_ITEM);
        }

        request.getItems().forEach(item -> {

            if (item == null) {
                throw new OrderServiceException("Order item cannot be null.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getItemId() == null || item.getItemId() <= 0) {
                throw new OrderServiceException("Order item ID must be greater than zero.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderServiceException("Order item quantity must be greater than zero.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getQuantity() > 100) {
                throw new OrderServiceException("Order item quantity cannot exceed 100.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getPrice() == null || item.getPrice().signum() < 0) {
                throw new OrderServiceException("Order item price cannot be negative.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getName() == null || item.getName().isBlank()) {
                throw new OrderServiceException("Order item name is required.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getCurrency() == null || item.getCurrency().isBlank()) {
                throw new OrderServiceException("Order item currency is required.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }
        });
    }


    private void validateRemoveItemRequest(RemoveItemRequest request) {

        if (request == null) {
            throw new OrderServiceException("Remove item request cannot be null.",
                    ErrorCode.INVALID_ORDER_ITEM);
        }

        if (request.getId() == null || request.getId() <= 0) {
            throw new OrderServiceException("Order ID must be greater than zero.",
                    ErrorCode.ORDER_NOT_FOUND);
        }

        if (request.getItemId() == null || request.getItemId() <= 0) {
            throw new OrderServiceException("Order item ID must be greater than zero.",
                    ErrorCode.ORDER_ITEM_NOT_FOUND);
        }
    }


    private void validateUpdateOrderStatusRequest(UpdateOrderStatusRequest request) {

        if (request == null) {
            throw new OrderServiceException("Update status request cannot be null.",
                    ErrorCode.INVALID_ORDER_STATUS);
        }

        if (request.getId() == null || request.getId() <= 0) {
            throw new OrderServiceException("Order ID must be greater than zero.",
                    ErrorCode.ORDER_NOT_FOUND);
        }

        if (request.getStatus() == null) {
            throw new OrderServiceException("Order status is required.",
                    ErrorCode.INVALID_ORDER_STATUS);
        }
    }


    private String getRequiredStringAttribute(WebSocketSession session,
                                              String attributeName, ErrorCode errorCode) {

        Object value = session.getAttributes().get(attributeName);

        if (value == null || value.toString().isBlank()) {
            throw new OrderServiceException("Required WebSocket attribute '" +
                            attributeName + "' is missing.", errorCode);
        }

        return value.toString().trim();
    }


    private Long getRequiredLongAttribute(WebSocketSession session,
                                          String attributeName, ErrorCode errorCode) {

        Object value = session.getAttributes().get(attributeName);

        if (value == null) {
            throw new OrderServiceException("Required WebSocket attribute '" +
                            attributeName + "' is missing.", errorCode);
        }

        try {

            Long result = Long.valueOf(value.toString());

            if (result <= 0) {
                throw new NumberFormatException();
            }
            return result;

        } catch (NumberFormatException ex) {
            throw new OrderServiceException("Invalid value for WebSocket attribute '" +
                            attributeName + "'.", errorCode);
        }
    }


    private Long getOptionalLongAttribute(WebSocketSession session, String attributeName) {

        Object value = session.getAttributes().get(attributeName);

        if (value == null) {
            return null;
        }

        try {
            return Long.valueOf(value.toString());

        } catch (NumberFormatException ex) {
            return null;
        }
    }


    private void validateSessionToken(String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new SessionNotFoundException("Session token is required.");
        }

        if (sessionToken.length() > 500) {
            throw new SessionNotFoundException("Invalid session token.");
        }
    }


    private void sendCustomerSnapshot(WebSocketSession session,
            TableSessionResponse tableSession, String traceId) throws IOException {

        OrderSnapshotResponse snapshot = orderService.checkRunningOrder(
                        tableSession.getRestaurantId(),
                        tableSession.getTableNumber());

        if (snapshot != null) {
            send(session, new WsMessage<>(
                    MessageType.ORDER_SNAPSHOT.name(), snapshot));
            return;
        }

        OrderConnectionResponse connection =
                OrderConnectionResponse.builder()
                        .restaurantId(tableSession.getRestaurantId())
                        .tableNumber(tableSession.getTableNumber())
                        .tableId(tableSession.getTableId())
                        .sessionToken(tableSession.getSessionToken())
                        .role(Role.CUSTOMER)
                        .build();

        send(session, new WsMessage<>(MessageType.ORDER_SNAPSHOT.name(), connection));

        log.debug("No running order for customer connection | " +
                        "restaurantId={} | tableNumber={} | traceId={}",
                tableSession.getRestaurantId(), tableSession.getTableNumber(), traceId);
    }


    private void send(WebSocketSession session, Object message) throws IOException {

        if (session == null || !session.isOpen()) {
            log.debug("Cannot send WebSocket message because connection is closed.");
            return;
        }

        String json = objectMapper.writeValueAsString(message);

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }


    private void sendError(WebSocketSession session, String message,
                           ErrorCode errorCode, String traceId) {

        try {

            if (session == null || !session.isOpen()) {
                return;
            }

            WebSocketErrorResponse response = WebSocketErrorResponse.builder()
                            .success(false)
                            .message(message != null ? message
                                            : "WebSocket operation failed.")
                            .errorCode(errorCode != null ? errorCode.getCode()
                                            : ErrorCode.INTERNAL_SERVER_ERROR.getCode())
                            .traceId(traceId)
                            .timestamp(LocalDateTime.now())
                            .build();

            send(session, response);

        } catch (Exception ex) {
            log.error("Unable to send WebSocket error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex);
        }
    }


    private void sendErrorAndClose(WebSocketSession session, String message,
                                   ErrorCode errorCode, String traceId) {
        sendError(session, message, errorCode, traceId);
        closeSafely(session, CloseStatus.POLICY_VIOLATION);
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {

        Role role = normalizeRole(session.getAttributes().get(ATTRIBUTE_ROLE));

        try {

            log.info("WebSocket connection closed | sessionId={} | role={} | status={}",
                    safeSessionId(session), role, status);

            if (role == Role.CUSTOMER) {

                Long restaurantId = getOptionalLongAttribute(session, ATTRIBUTE_RESTAURANT_ID);
                Long tableNumber = getOptionalLongAttribute(session, ATTRIBUTE_TABLE_NUMBER);

                if (restaurantId != null && tableNumber != null) {
                    manager.removeTableSession(restaurantId, tableNumber, session);
                }

            } else if (role == Role.STAFF || role == Role.OWNER) {

                Long restaurantId = getOptionalLongAttribute(session, ATTRIBUTE_RESTAURANT_ID);

                if (restaurantId != null) {
                    manager.removeStaffSession(restaurantId, session);
                }
            }

        } catch (Exception ex) {
            log.error("Error while cleaning WebSocket connection | sessionId={}",
                    safeSessionId(session), ex);
        }
    }

    private void closeSafely(WebSocketSession session, CloseStatus status) {

        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(status);
        } catch (IOException ex) {
            log.warn("Unable to close WebSocket session | sessionId={}",
                    safeSessionId(session), ex);
        }
    }


    private String safeSessionId(WebSocketSession session) {
        return session != null ? session.getId() : "unknown";
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    private String maskToken(String token) {

        if (token == null || token.length() <= 8) {
            return "********";
        }

        return token.substring(0, 4)
                + "****"
                + token.substring(
                token.length() - 4
        );
    }
}

