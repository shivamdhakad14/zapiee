
package com.zapiee.orderservice.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zapiee.orderservice.client.TableSessionClient;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTRIBUTE_ROLE = "role";
    private static final String ATTRIBUTE_RESTAURANT_ID = "restaurantId";
    private static final String ATTRIBUTE_SESSION_TOKEN = "sessionToken";
    private static final String ATTRIBUTE_TABLE_ID = "tableId";
    private static final String ATTRIBUTE_TABLE_NUMBER = "tableNumber";
    private static final String ATTRIBUTE_SESSION_ID = "sessionId";

    private static final int MAX_MESSAGE_LENGTH = 100_000;
    private static final int MAX_ITEMS_PER_REQUEST = 50;
    private static final int MAX_ITEM_QUANTITY = 1000;
    private static final int MAX_SESSION_TOKEN_LENGTH = 500;
    private static final int MAX_ITEM_NAME_LENGTH = 255;
    private static final int MAX_CURRENCY_LENGTH = 3;

    private final WebSocketSessionManager manager;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;
    private final TableSessionClient tableSessionClient;


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        String traceId = generateTraceId();

        try {
            if (session == null || !session.isOpen()) {
                log.warn("WebSocket connection rejected | traceId={}", traceId);
                return;
            }

            Role role = normalizeRole(
                    session.getAttributes().get(ATTRIBUTE_ROLE));

            if (role == null) {
                sendErrorAndClose(
                        session,
                        "Invalid or missing WebSocket role.",
                        ErrorCode.INVALID_ROLE,
                        traceId
                );
                return;
            }

            session.getAttributes().put(ATTRIBUTE_ROLE, role);

            switch (role) {
                case CUSTOMER ->
                        handleCustomerConnection(session, traceId);

                case STAFF, OWNER ->
                        handleStaffOwnerConnection(session, role, traceId);
            }

            log.info(
                    "WebSocket connection established | sessionId={} | role={} | traceId={}",
                    safeSessionId(session), role, traceId
            );

        } catch (OrderServiceException ex) {

            log.warn(
                    "WebSocket connection rejected | sessionId={} | errorCode={} | traceId={}",
                    safeSessionId(session),
                    ex.getErrorCode().getCode(),
                    traceId
            );

            sendErrorAndClose(
                    session, ex.getMessage(), ex.getErrorCode(), traceId
            );

        } catch (Exception ex) {

            log.error(
                    "Unexpected WebSocket connection error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex
            );

            sendErrorAndClose(
                    session,
                    "Unable to establish WebSocket connection.",
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    traceId
            );
        }
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        if (session == null) {
            return;
        }

        Role role = normalizeRole(
                session.getAttributes().get(ATTRIBUTE_ROLE));

        try {
            log.info(
                    "WebSocket connection closed | sessionId={} | role={} | status={}",
                    safeSessionId(session), role, status
            );

            if (role == Role.CUSTOMER) {

                Long restaurantId = getOptionalLongAttribute(
                        session, ATTRIBUTE_RESTAURANT_ID);

                Long tableNumber = getOptionalLongAttribute(
                        session, ATTRIBUTE_TABLE_NUMBER);

                if (restaurantId != null && tableNumber != null) {
                    manager.removeTableSession(
                            restaurantId, tableNumber, session);
                }

            } else if (role == Role.STAFF || role == Role.OWNER) {

                Long restaurantId = getOptionalLongAttribute(
                        session, ATTRIBUTE_RESTAURANT_ID);

                if (restaurantId != null) {
                    manager.removeStaffSession(restaurantId, session);
                }
            }

        } catch (Exception ex) {
            log.error(
                    "WebSocket cleanup failed | sessionId={}",
                    safeSessionId(session), ex
            );
        }
    }

    private void handleCustomerConnection(
            WebSocketSession session,
            String traceId
    ) throws IOException {

        TableSessionResponse storedSession =
                getStoredTableSession(session);

        String token = storedSession.getSessionToken();

        validateSessionToken(token);

        TableSessionResponse validatedSession =
                resolveTableSession(token, traceId);

        validateTableSession(validatedSession, token);

        // Verify that the connection context matches Table Service.
        validateStoredContextMatches(
                session, validatedSession);

        session.getAttributes().put(
                ATTRIBUTE_RESTAURANT_ID,
                validatedSession.getRestaurantId());

        session.getAttributes().put(
                ATTRIBUTE_TABLE_ID,
                validatedSession.getTableId());

        session.getAttributes().put(
                ATTRIBUTE_TABLE_NUMBER,
                validatedSession.getTableNumber());

        session.getAttributes().put(
                ATTRIBUTE_SESSION_ID,
                validatedSession.getSessionId());

        manager.addTableSession(
                validatedSession.getRestaurantId(),
                validatedSession.getTableNumber(),
                session
        );

        log.info(
                "Customer WebSocket authenticated | restaurantId={} | tableId={} "
                        + "| tableNumber={} | sessionId={} | wsSessionId={} | traceId={}",
                validatedSession.getRestaurantId(),
                validatedSession.getTableId(),
                validatedSession.getTableNumber(),
                validatedSession.getSessionId(),
                safeSessionId(session),
                traceId
        );

        sendCustomerSnapshot(session, validatedSession, traceId);
    }

    private void handleStaffOwnerConnection(
            WebSocketSession session,
            Role role,
            String traceId
    ) throws IOException {

        validateStaffOwnerRole(role);

        Long restaurantId = getRequiredLongAttribute(
                session,
                ATTRIBUTE_RESTAURANT_ID,
                ErrorCode.RESTAURANT_ACCESS_DENIED
        );

        manager.addStaffSession(restaurantId, session);

        List<OrderListResponse> runningOrders =
                orderService.getRunningOrders(restaurantId);

        send(
                session,
                new WsMessage<>(
                        MessageType.STAFF_ORDER_SNAPSHOT.name(),
                        runningOrders
                )
        );

        log.info(
                "{} WebSocket authenticated | restaurantId={} | sessionId={} | traceId={}",
                role, restaurantId, safeSessionId(session), traceId
        );
    }

    // ================================================================
    // INCOMING MESSAGES
    // ================================================================

    @Override
    protected void handleTextMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        String traceId = generateTraceId();

        try {
            validateIncomingMessage(session, message);

            Role role = normalizeRole(
                    session.getAttributes().get(ATTRIBUTE_ROLE));

            if (role == null) {
                throw new OrderServiceException(
                        "WebSocket role is missing or invalid.",
                        ErrorCode.INVALID_ROLE
                );
            }

            WsMessage<?> wsMessage =
                    parseWebSocketMessage(message.getPayload());

            MessageType messageType =
                    parseMessageType(wsMessage.getType());

            validateMessageAllowedForRole(role, messageType);

            switch (role) {
                case CUSTOMER ->
                        handleCustomerMessage(
                                session, messageType, wsMessage, traceId);

                case STAFF, OWNER ->
                        handleStaffOwnerMessage(
                                session, role, messageType, wsMessage, traceId);
            }

        } catch (OrderServiceException ex) {

            log.warn(
                    "WebSocket business error | sessionId={} | errorCode={} | traceId={}",
                    safeSessionId(session),
                    ex.getErrorCode().getCode(),
                    traceId
            );

            sendError(
                    session, ex.getMessage(), ex.getErrorCode(), traceId);

        } catch (JsonProcessingException ex) {

            log.warn(
                    "Malformed WebSocket JSON | sessionId={} | traceId={}",
                    safeSessionId(session), traceId
            );

            sendError(
                    session,
                    "Invalid WebSocket message format.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID,
                    traceId
            );

        } catch (IllegalArgumentException ex) {

            log.warn(
                    "WebSocket validation error | sessionId={} | traceId={} | reason={}",
                    safeSessionId(session), traceId, ex.getMessage()
            );

            sendError(
                    session,
                    safeMessage(ex, "Invalid request."),
                    ErrorCode.INVALID_ORDER_ITEM,
                    traceId
            );

        } catch (IllegalStateException ex) {

            log.warn(
                    "WebSocket business rule violation | sessionId={} | traceId={} | reason={}",
                    safeSessionId(session), traceId, ex.getMessage()
            );

            sendError(
                    session,
                    safeMessage(ex, "Order operation is not allowed."),
                    ErrorCode.INVALID_ORDER_STATUS,
                    traceId
            );

        } catch (Exception ex) {

            log.error(
                    "Unexpected WebSocket message error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex
            );

            sendError(
                    session,
                    "Unable to process WebSocket message.",
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    traceId
            );
        }
    }

    private void handleCustomerMessage(
            WebSocketSession session,
            MessageType messageType,
            WsMessage<?> wsMessage,
            String traceId
    ) {

        TableSessionResponse tableSession =
                getValidatedStoredTableSession(session, traceId);

        String sessionToken = tableSession.getSessionToken();

        switch (messageType) {

            case ADD_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request, false);

                orderService.placeFirstOrder(sessionToken, request);
            }

            case NEW_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request, false);

                orderService.addNewItems(sessionToken, request);
            }

            case REMOVE_ITEM -> {
                RemoveItemRequest request = convertPayload(
                        wsMessage.getPayload(), RemoveItemRequest.class);

                validateRemoveItemRequest(request);

                orderService.removeItem(sessionToken, request);
            }

            default -> throw new OrderServiceException(
                    "Message type '" + messageType
                            + "' is not allowed for CUSTOMER.",
                    ErrorCode.FORBIDDEN
            );
        }

        log.info(
                "Customer WebSocket operation completed | restaurantId={} "
                        + "| tableId={} | messageType={} | traceId={}",
                tableSession.getRestaurantId(),
                tableSession.getTableId(),
                messageType,
                traceId
        );
    }

    private void handleStaffOwnerMessage(
            WebSocketSession session,
            Role role,
            MessageType messageType,
            WsMessage<?> wsMessage,
            String traceId
    ) {

        validateStaffOwnerRole(role);

        Long restaurantId = getRequiredLongAttribute(
                session,
                ATTRIBUTE_RESTAURANT_ID,
                ErrorCode.RESTAURANT_ACCESS_DENIED
        );

        switch (messageType) {

            case UPDATE_STATUS -> {
                UpdateOrderStatusRequest request = convertPayload(
                        wsMessage.getPayload(),
                        UpdateOrderStatusRequest.class
                );

                validateUpdateOrderStatusRequest(request);

                orderService.updateStatusByOrderId(
                        restaurantId,
                        request.getOrderId(),
                        request.getStatus()
                );
            }

            case ADD_ITEM, NEW_ITEM -> {
                AddItemRequest request = convertPayload(
                        wsMessage.getPayload(), AddItemRequest.class);

                validateAddItemRequest(request, true);

                if (request.getOrderId() == null ) {

                    throw new OrderServiceException(
                            "Order ID must be greater than zero.",
                            ErrorCode.ORDER_NOT_FOUND
                    );
                }

                orderService.addItemsByRestaurant(
                        restaurantId,
                        request.getOrderId(),
                        request,
                        messageType
                );
            }

            case REMOVE_ITEM -> {
                RemoveItemRequest request = convertPayload(
                        wsMessage.getPayload(), RemoveItemRequest.class);

                validateRemoveItemRequest(request);

                orderService.removeItemByRestaurant(
                        restaurantId, request);
            }

            default -> throw new OrderServiceException(
                    "Message type '" + messageType
                            + "' is not allowed for " + role + ".",
                    ErrorCode.FORBIDDEN
            );
        }

        log.info(
                "{} WebSocket operation completed | restaurantId={} "
                        + "| messageType={} | traceId={}",
                role, restaurantId, messageType, traceId
        );
    }


    private TableSessionResponse getStoredTableSession(
            WebSocketSession session
    ) {

        if (session == null) {
            throw new OrderServiceException(
                    "WebSocket session is missing.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID
            );
        }

        Long restaurantId = getRequiredLongAttribute(
                session, ATTRIBUTE_RESTAURANT_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long tableId = getRequiredLongAttribute(
                session, ATTRIBUTE_TABLE_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long tableNumber = getRequiredLongAttribute(
                session, ATTRIBUTE_TABLE_NUMBER,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long sessionId = getRequiredLongAttribute(
                session, ATTRIBUTE_SESSION_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        String sessionToken = getRequiredStringAttribute(
                session, ATTRIBUTE_SESSION_TOKEN,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        validateSessionToken(sessionToken);

        return TableSessionResponse.builder()
                .restaurantId(restaurantId)
                .tableId(tableId)
                .tableNumber(tableNumber)
                .sessionId(sessionId)
                .sessionToken(sessionToken)
                .sessionStatus(SessionStatus.ACTIVE)
                .build();
    }

    private TableSessionResponse resolveTableSession(
            String sessionToken,
            String traceId
    ) {

        try {
            TableSessionResponse response =
                    tableSessionClient.getSession(sessionToken);

            if (response == null) {
                throw new SessionNotFoundException(
                        "Table session was not found.");
            }

            return response;

        } catch (OrderServiceException ex) {
            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Table session validation failed | traceId={}",
                    traceId, ex
            );

            throw new OrderServiceException(
                    "Unable to validate table session.",
                    ErrorCode.TABLE_SERVICE_UNAVAILABLE,
                    ex
            );
        }
    }

    private void validateTableSession(
            TableSessionResponse response,
            String requestedToken
    ) {

        if (response == null) {
            throw new SessionNotFoundException(
                    "Table session was not found.");
        }

        if (response.getSessionId() == null
                || response.getSessionId() <= 0) {
            throw new SessionNotFoundException(
                    "Table session ID is missing or invalid.");
        }

        if (response.getRestaurantId() == null
                || response.getRestaurantId() <= 0) {
            throw new TableMismatchException(
                    "Invalid restaurant information in table session.");
        }

        if (response.getTableId() == null
                || response.getTableId() <= 0) {
            throw new TableMismatchException(
                    "Invalid table information in table session.");
        }

        if (response.getTableNumber() == null
                || response.getTableNumber() <= 0) {
            throw new TableMismatchException(
                    "Invalid table number in table session.");
        }

        if (response.getSessionToken() == null
                || response.getSessionToken().isBlank()) {
            throw new SessionNotFoundException(
                    "Table session token is missing.");
        }

        if (!response.getSessionToken().equals(requestedToken)) {
            throw new TableMismatchException(
                    "Table session token mismatch.");
        }

        if (response.getSessionStatus() == null) {
            throw new SessionNotFoundException(
                    "Table session status is missing.");
        }

        if (response.getSessionStatus() == SessionStatus.EXPIRED) {
            throw new SessionExpiredException(
                    "Table session has expired.");
        }

        if (response.getSessionStatus() == SessionStatus.CLOSED) {
            throw new SessionClosedException(
                    "Table session is closed.");
        }

        if (response.getSessionStatus() != SessionStatus.ACTIVE) {
            throw new SessionNotFoundException(
                    "Table session is not active.");
        }
    }

    private void validateStoredContextMatches(
            WebSocketSession session,
            TableSessionResponse validated
    ) {

        Long storedRestaurantId = getRequiredLongAttribute(
                session, ATTRIBUTE_RESTAURANT_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long storedTableId = getRequiredLongAttribute(
                session, ATTRIBUTE_TABLE_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long storedTableNumber = getRequiredLongAttribute(
                session, ATTRIBUTE_TABLE_NUMBER,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        Long storedSessionId = getRequiredLongAttribute(
                session, ATTRIBUTE_SESSION_ID,
                ErrorCode.WEBSOCKET_SESSION_INVALID);

        if (!storedRestaurantId.equals(validated.getRestaurantId())
                || !storedTableId.equals(validated.getTableId())
                || !storedTableNumber.equals(validated.getTableNumber())
                || !storedSessionId.equals(validated.getSessionId())) {

            throw new TableMismatchException(
                    "WebSocket connection does not match the active table session.");
        }
    }

    private TableSessionResponse getValidatedStoredTableSession(
            WebSocketSession session,
            String traceId
    ) {

        TableSessionResponse storedSession =
                getStoredTableSession(session);

        String sessionToken = storedSession.getSessionToken();

        TableSessionResponse validatedSession =
                resolveTableSession(sessionToken, traceId);

        validateTableSession(validatedSession, sessionToken);

        validateStoredContextMatches(session, validatedSession);

        return validatedSession;
    }

    private void validateSessionToken(String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new SessionNotFoundException(
                    "Session token is required.");
        }

        if (sessionToken.length() > MAX_SESSION_TOKEN_LENGTH) {
            throw new SessionNotFoundException(
                    "Invalid session token.");
        }
    }

    // ================================================================
    // MESSAGE PARSING
    // ================================================================

    private void validateIncomingMessage(
            WebSocketSession session,
            TextMessage message
    ) {

        if (session == null || !session.isOpen()) {
            throw new OrderServiceException(
                    "WebSocket session is closed.",
                    ErrorCode.WEBSOCKET_SESSION_INVALID
            );
        }

        if (message == null
                || message.getPayload() == null
                || message.getPayload().isBlank()) {

            throw new OrderServiceException(
                    "WebSocket message cannot be empty.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        if (message.getPayload().length() > MAX_MESSAGE_LENGTH) {
            throw new OrderServiceException(
                    "WebSocket message exceeds the maximum allowed size.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }
    }

    private WsMessage<?> parseWebSocketMessage(
            String payload
    ) throws JsonProcessingException {

        JsonNode root = objectMapper.readTree(payload);

        if (root == null || !root.isObject()) {
            throw new OrderServiceException(
                    "WebSocket message must be a JSON object.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        JsonNode typeNode = root.get("type");

        if (typeNode == null
                || !typeNode.isTextual()
                || typeNode.asText().isBlank()) {

            throw new OrderServiceException(
                    "A valid message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        JsonNode payloadNode = root.get("payload");

        if (payloadNode == null || payloadNode.isNull()) {
            throw new OrderServiceException(
                    "WebSocket message payload is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        if (!payloadNode.isObject()) {
            throw new OrderServiceException(
                    "WebSocket message payload must be a JSON object.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        return objectMapper.treeToValue(root, WsMessage.class);
    }

    private <T> T convertPayload(
            Object payload,
            Class<T> targetClass
    ) {

        if (payload == null) {
            throw new OrderServiceException(
                    "WebSocket payload cannot be null.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        try {
            T result = objectMapper.convertValue(payload, targetClass);

            if (result == null) {
                throw new OrderServiceException(
                        "Invalid WebSocket payload.",
                        ErrorCode.WEBSOCKET_MESSAGE_INVALID
                );
            }

            return result;

        } catch (IllegalArgumentException ex) {

            throw new OrderServiceException(
                    "Invalid WebSocket payload.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID,
                    ex
            );
        }
    }

    private MessageType parseMessageType(String type) {

        if (type == null || type.isBlank()) {
            throw new OrderServiceException(
                    "Message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        try {
            return MessageType.valueOf(
                    type.trim().toUpperCase(Locale.ROOT));

        } catch (IllegalArgumentException ex) {
            throw new OrderServiceException(
                    "Unsupported WebSocket message type: " + type,
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }
    }

    private void validateMessageAllowedForRole(
            Role role,
            MessageType messageType
    ) {

        if (role == null) {
            throw new OrderServiceException(
                    "WebSocket role is required.",
                    ErrorCode.INVALID_ROLE
            );
        }

        if (messageType == null) {
            throw new OrderServiceException(
                    "Message type is required.",
                    ErrorCode.WEBSOCKET_MESSAGE_INVALID
            );
        }

        switch (role) {

            case CUSTOMER -> {
                if (messageType == MessageType.UPDATE_STATUS
                        || messageType == MessageType.STAFF_ORDER_SNAPSHOT) {

                    throw new OrderServiceException(
                            "This message type is not allowed for customers.",
                            ErrorCode.FORBIDDEN
                    );
                }
            }

            case STAFF, OWNER -> {
                if (messageType == MessageType.ORDER_SNAPSHOT) {
                    throw new OrderServiceException(
                            "This message type is not allowed for Staff/Owner.",
                            ErrorCode.FORBIDDEN
                    );
                }
            }
        }
    }

    private void validateStaffOwnerRole(Role role) {

        if (role != Role.STAFF && role != Role.OWNER) {
            throw new OrderServiceException(
                    "Only STAFF or OWNER can use restaurant-level WebSocket.",
                    ErrorCode.INVALID_ROLE
            );
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
            return null;
        }
    }


    private void validateAddItemRequest(AddItemRequest request, boolean restaurantRequest) {

        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderServiceException("At least one order item is required.",
                    ErrorCode.INVALID_ORDER_ITEM);
        }

        if (request.getItems().size() > MAX_ITEMS_PER_REQUEST) {
            throw new OrderServiceException(
                    "A maximum of " + MAX_ITEMS_PER_REQUEST
                            + " items can be submitted in one request.",
                    ErrorCode.INVALID_ORDER_ITEM
            );
        }

        if (restaurantRequest && (request.getOrderId() == null )) {

            throw new OrderServiceException(
                    "Order ID must be greater than zero.",
                    ErrorCode.ORDER_NOT_FOUND
            );
        }

        request.getItems().forEach(item -> {

            if (item == null) {
                throw new OrderServiceException(
                        "Order item cannot be null.",
                        ErrorCode.INVALID_ORDER_ITEM
                );
            }

            if (item.getItemId() == null || item.getItemId() <= 0) {
                throw new OrderServiceException(
                        "Menu item ID must be greater than zero.",
                        ErrorCode.INVALID_ORDER_ITEM
                );
            }

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderServiceException(
                        "Order item quantity must be greater than zero.",
                        ErrorCode.INVALID_ORDER_ITEM
                );
            }

            if (item.getQuantity() > MAX_ITEM_QUANTITY) {
                throw new OrderServiceException(
                        "Order item quantity cannot exceed "
                                + MAX_ITEM_QUANTITY + ".",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getPrice() == null || item.getPrice().signum() < 0) {
                throw new OrderServiceException(
                        "Order item price cannot be negative.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getName() == null || item.getName().isBlank()
                    || item.getName().trim().length() > MAX_ITEM_NAME_LENGTH) {
                throw new OrderServiceException(
                        "Order item name is required and must not exceed "
                                + MAX_ITEM_NAME_LENGTH + " characters.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }

            if (item.getCurrency() == null || !item.getCurrency().trim()
                    .toUpperCase(Locale.ROOT)
                    .matches("[A-Z]{3}")) {
                throw new OrderServiceException(
                        "Currency must be a valid three-letter currency code.",
                        ErrorCode.INVALID_ORDER_ITEM);
            }
        });
    }


    private void validateRemoveItemRequest(RemoveItemRequest request) {

        if (request == null) {
            throw new OrderServiceException(
                    "Remove item request cannot be null.",
                    ErrorCode.INVALID_ORDER_ITEM
            );
        }

        if (request.getOrderId() == null) {

            throw new OrderServiceException(
                    "Order ID must be greater than zero.",
                    ErrorCode.ORDER_NOT_FOUND
            );
        }

        if (request.getItemId() == null
                || request.getItemId() <= 0) {

            throw new OrderServiceException(
                    "Order item ID must be greater than zero.",
                    ErrorCode.ORDER_ITEM_NOT_FOUND
            );
        }
    }

    private void validateUpdateOrderStatusRequest(
            UpdateOrderStatusRequest request
    ) {

        if (request == null) {
            throw new OrderServiceException(
                    "Update status request cannot be null.",
                    ErrorCode.INVALID_ORDER_STATUS
            );
        }

        if (request.getOrderId() == null) {

            throw new OrderServiceException(
                    "Order ID must be greater than zero.",
                    ErrorCode.ORDER_NOT_FOUND
            );
        }

        if (request.getStatus() == null) {
            throw new OrderServiceException(
                    "Order status is required.",
                    ErrorCode.INVALID_ORDER_STATUS
            );
        }
    }

    // ================================================================
    // ATTRIBUTE HELPERS
    // ================================================================

    private String getRequiredStringAttribute(
            WebSocketSession session,
            String attributeName,
            ErrorCode errorCode
    ) {

        if (session == null) {
            throw new OrderServiceException(
                    "WebSocket session is missing.", errorCode);
        }

        Object value = session.getAttributes().get(attributeName);

        if (value == null || value.toString().isBlank()) {
            throw new OrderServiceException(
                    "Required WebSocket attribute '"
                            + attributeName + "' is missing.",
                    errorCode
            );
        }

        return value.toString().trim();
    }

    private Long getRequiredLongAttribute(
            WebSocketSession session,
            String attributeName,
            ErrorCode errorCode
    ) {

        if (session == null) {
            throw new OrderServiceException(
                    "WebSocket session is missing.", errorCode);
        }

        Object value = session.getAttributes().get(attributeName);

        if (value == null) {
            throw new OrderServiceException(
                    "Required WebSocket attribute '"
                            + attributeName + "' is missing.",
                    errorCode
            );
        }

        try {
            Long result = Long.valueOf(value.toString());

            if (result <= 0) {
                throw new NumberFormatException();
            }

            return result;

        } catch (NumberFormatException ex) {
            throw new OrderServiceException(
                    "Invalid value for WebSocket attribute '"
                            + attributeName + "'.",
                    errorCode,
                    ex
            );
        }
    }

    private Long getOptionalLongAttribute(
            WebSocketSession session,
            String attributeName
    ) {

        if (session == null) {
            return null;
        }

        Object value = session.getAttributes().get(attributeName);

        if (value == null) {
            return null;
        }

        try {
            Long result = Long.valueOf(value.toString());
            return result > 0 ? result : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ================================================================
    // OUTGOING MESSAGES
    // ================================================================

    private void sendCustomerSnapshot(
            WebSocketSession session,
            TableSessionResponse tableSession,
            String traceId
    ) throws IOException {

        OrderSnapshotResponse snapshot =
                orderService.checkRunningOrder(
                        tableSession.getRestaurantId(),
                        tableSession.getTableNumber()
                );

        if (snapshot != null) {
            send(
                    session,
                    new WsMessage<>(
                            MessageType.ORDER_SNAPSHOT.name(),
                            snapshot
                    )
            );
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

        send(
                session,
                new WsMessage<>(
                        MessageType.ORDER_SNAPSHOT.name(),
                        connection
                )
        );

        log.debug(
                "No running order for customer connection | restaurantId={} "
                        + "| tableNumber={} | traceId={}",
                tableSession.getRestaurantId(),
                tableSession.getTableNumber(),
                traceId
        );
    }

    private void send(
            WebSocketSession session,
            Object message
    ) throws IOException {

        if (session == null || !session.isOpen()) {
            return;
        }

        String json = objectMapper.writeValueAsString(message);

        synchronized (session) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }

    private void sendError(
            WebSocketSession session,
            String message,
            ErrorCode errorCode,
            String traceId
    ) {

        try {
            if (session == null || !session.isOpen()) {
                return;
            }

            WebSocketErrorResponse response =
                    WebSocketErrorResponse.builder()
                            .success(false)
                            .message(
                                    message != null
                                            ? message
                                            : "WebSocket operation failed."
                            )
                            .errorCode(
                                    errorCode != null
                                            ? errorCode.getCode()
                                            : ErrorCode.INTERNAL_SERVER_ERROR.getCode()
                            )
                            .traceId(traceId)
                            .timestamp(LocalDateTime.now())
                            .build();

            send(session, response);

        } catch (Exception ex) {
            log.error(
                    "Unable to send WebSocket error | sessionId={} | traceId={}",
                    safeSessionId(session), traceId, ex
            );
        }
    }

    private void sendErrorAndClose(
            WebSocketSession session,
            String message,
            ErrorCode errorCode,
            String traceId
    ) {

        sendError(session, message, errorCode, traceId);
        closeSafely(session, CloseStatus.POLICY_VIOLATION);
    }

    private void closeSafely(
            WebSocketSession session,
            CloseStatus status
    ) {

        if (session == null || !session.isOpen()) {
            return;
        }

        try {
            session.close(status);
        } catch (IOException ex) {
            log.warn(
                    "Unable to close WebSocket session | sessionId={}",
                    safeSessionId(session), ex
            );
        }
    }

    // ================================================================
    // GENERAL HELPERS
    // ================================================================

    private String safeSessionId(WebSocketSession session) {
        return session != null ? session.getId() : "unknown";
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }

    private String safeMessage(
            RuntimeException ex,
            String fallback
    ) {
        String message = ex.getMessage();
        return message != null && !message.isBlank()
                ? message
                : fallback;
    }
}
