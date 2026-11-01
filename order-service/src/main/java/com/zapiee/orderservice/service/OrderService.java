
package com.zapiee.orderservice.service;

import com.zapiee.orderservice.client.TableSessionClient;
import com.zapiee.orderservice.dto.request.AddItemRequest;
import com.zapiee.orderservice.dto.request.OrderItemDTO;
import com.zapiee.orderservice.dto.request.RemoveItemRequest;
import com.zapiee.orderservice.dto.request.WsMessage;
import com.zapiee.orderservice.dto.response.MessageResponse;
import com.zapiee.orderservice.dto.response.OrderListResponse;
import com.zapiee.orderservice.dto.response.OrderResponse;
import com.zapiee.orderservice.dto.response.OrderSnapshotResponse;
import com.zapiee.orderservice.dto.response.TableSessionResponse;
import com.zapiee.orderservice.enums.MessageType;
import com.zapiee.orderservice.enums.OrderStatus;
import com.zapiee.orderservice.enums.SessionStatus;
import com.zapiee.orderservice.mapper.OrderMapper;
import com.zapiee.orderservice.model.Order;
import com.zapiee.orderservice.model.OrderItem;
import com.zapiee.orderservice.repository.OrderItemRepository;
import com.zapiee.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String DEFAULT_CURRENCY = "INR";
    private static final int MAX_ITEM_QUANTITY = 1000;

    private static final List<OrderStatus> RUNNING_STATUSES =
            List.of(OrderStatus.PENDING, OrderStatus.PREPARING, OrderStatus.SERVED);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper mapper;
    private final TableSessionClient tableSessionClient;
    private final WebSocketPushService push;

    @Value("${zapiee.order.tax-percentage:0}")
    private BigDecimal defaultTaxPercentage;

    @Transactional
    public OrderResponse placeFirstOrder(String sessionToken, AddItemRequest request) {

        TableSessionResponse session = validateAndGetActiveSession(sessionToken);

        validateAddItemRequest(request);

        Optional<Order> runningOrder =
                orderRepository.findByRestaurantIdAndTableNumberAndStatusIn(
                        session.getRestaurantId(), session.getTableNumber(),
                        RUNNING_STATUSES);

        if (runningOrder.isPresent()) {
            throw new IllegalStateException(
                    "A running order already exists for this table. Use NEW_ITEM.");
        }

        Order order = Order.builder()
                .restaurantId(session.getRestaurantId())
                .tableId(session.getTableId())
                .tableNumber(session.getTableNumber())
                .sessionId(session.getSessionId())
                .sessionToken(session.getSessionToken())
                .status(OrderStatus.PENDING)
                .payment(false)
                .subtotal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .taxPercentage(resolveTaxPercentage())
                .taxAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .totalAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .items(new ArrayList<>())
                .build();

        addItemsToOrder(order, request);
        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.NEW_ITEM);

        log.info("First order created | orderId={} | restaurantId={} | tableId={} | sessionId={}",
                savedOrder.getOrderId(), savedOrder.getRestaurantId(),
                savedOrder.getTableId(), savedOrder.getSessionId());

        return mapper.toOrderResponse(savedOrder);
    }


    @Transactional
    public OrderResponse addNewItems(String sessionToken, AddItemRequest request) {

        TableSessionResponse session = validateAndGetActiveSession(sessionToken);

        validateAddItemRequest(request);

        Order order = orderRepository.findByRestaurantIdAndTableNumberAndStatusIn(
                        session.getRestaurantId(), session.getTableNumber(), RUNNING_STATUSES)
                       .orElseThrow(() -> new IllegalStateException(
                                "No running order exists for this table."));

        validateOrderBelongsToSession(order, session);
        validateOrderModifiable(order);

        OrderStatus previousStatus = order.getStatus();

        addItemsToOrder(order, request);

        if (previousStatus == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PREPARING);
        }

        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.NEW_ITEM);

        log.info("Items added | orderId={} | sessionId={} | previousStatus={} | newStatus={}",
                savedOrder.getOrderId(), session.getSessionId(),
                previousStatus, savedOrder.getStatus());

        return mapper.toOrderResponse(savedOrder);
    }


    @Transactional
    public OrderResponse removeItem(String sessionToken, RemoveItemRequest request) {

        TableSessionResponse session = validateAndGetActiveSession(sessionToken);

        validateRemoveItemRequest(request);

        OrderItem item = getOrderItem(request.getItemId());
        Order order = item.getOrder();

        validateOrderBelongsToSession(order, session);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Items can only be removed while the order is PENDING."
            );
        }

        removeItemFromOrder(order, item);

        if (order.getItems().isEmpty()) {
            orderRepository.delete(order);

            OrderSnapshotResponse snapshot = mapper.toSnapshot(order);

            broadcastSnapshot(snapshot, order.getRestaurantId(), order.getTableNumber(),
                    MessageType.REMOVE_ITEM,
                    order.getOrderId()
            );

            log.info(
                    "Customer removed final item; empty order deleted | orderId={} | orderItemId={}",
                    order.getOrderId(),
                    request.getItemId()
            );

            return mapper.toOrderResponse(order);
        }

        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.REMOVE_ITEM);

        log.info(
                "Customer removed item | orderId={} | orderItemId={}",
                savedOrder.getOrderId(),
                request.getItemId()
        );

        return mapper.toOrderResponse(savedOrder);
    }

    // ---------------------------------------------------------
    // UPDATE ORDER STATUS - RESTAURANT
    // ---------------------------------------------------------

    @Transactional
    public OrderResponse updateStatusByOrderId(
            Long restaurantId,
            String orderId,
            OrderStatus newStatus) {



        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Order status cannot be null."
            );
        }

        Order order = getOrderByDatabaseId(orderId);

        if (!Objects.equals(order.getRestaurantId(), restaurantId)) {
            throw new IllegalStateException(
                    "Order does not belong to the requested restaurant."
            );
        }

        validateStatusTransition(
                order.getStatus(),
                newStatus,
                order.isPayment()
        );

        OrderStatus previousStatus = order.getStatus();

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.UPDATE_STATUS);

        log.info(
                "Order status updated | restaurantId={} | orderId={} | {} -> {}",
                restaurantId,
                orderId,
                previousStatus,
                newStatus
        );

        return mapper.toOrderResponse(savedOrder);
    }


    @Transactional
    public OrderResponse updateStatusByOrderId(String orderId, OrderStatus newStatus) {


        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Order status cannot be null."
            );
        }

        Order order = getOrderByDatabaseId(orderId);

        validateStatusTransition(
                order.getStatus(),
                newStatus,
                order.isPayment()
        );

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.UPDATE_STATUS);

        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse addItemsByRestaurant(
            Long restaurantId,
            String orderId,
            AddItemRequest request,
            MessageType messageType) {

        validateAddItemRequest(request);

        if (messageType != MessageType.ADD_ITEM
                && messageType != MessageType.NEW_ITEM) {
            throw new IllegalArgumentException(
                    "Invalid message type for adding items. Allowed types: ADD_ITEM, NEW_ITEM."
            );
        }

        Order order = getOrderByDatabaseId(orderId);

        if (!Objects.equals(order.getRestaurantId(), restaurantId)) {
            throw new IllegalStateException(
                    "Order does not belong to the requested restaurant."
            );
        }

        validateOrderModifiable(order);

        OrderStatus previousStatus = order.getStatus();

        addItemsToOrder(order, request);

        if (previousStatus == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PREPARING);
        }

        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, messageType);

        log.info("Staff/Owner added items | restaurantId={} | orderId={} | previousStatus={} | newStatus={}",
                restaurantId, savedOrder.getOrderId(), previousStatus, savedOrder.getStatus());

        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse removeItemByRestaurant(
            Long restaurantId,
            RemoveItemRequest request) {

        validateRemoveItemRequest(request);

        OrderItem item = getOrderItem(request.getItemId());
        Order order = item.getOrder();

        if (!Objects.equals(order.getRestaurantId(), restaurantId)) {
            throw new IllegalStateException(
                    "Order does not belong to the requested restaurant.");
        }

        validateOrderModifiable(order);

        removeItemFromOrder(order, item);

        if (order.getItems().isEmpty()) {
            orderRepository.delete(order);

            OrderSnapshotResponse snapshot = mapper.toSnapshot(order);

            broadcastSnapshot(snapshot,
                    order.getRestaurantId(), order.getTableNumber(),
                    MessageType.REMOVE_ITEM, order.getOrderId());

            log.info("Staff/Owner removed final item; empty order deleted | orderId={}",
                    order.getOrderId());

            return mapper.toOrderResponse(order);
        }

        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.REMOVE_ITEM);

        log.info("Staff/Owner removed item | restaurantId={} | orderId={} | orderItemId={}",
                restaurantId, savedOrder.getOrderId(), request.getItemId());

        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markPaymentSuccess(String orderId) {

        Order order = getOrderByDatabaseId(orderId);

        if (order.isPayment()) {
            return mapper.toOrderResponse(order);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Order is already completed but payment is not marked.");
        }

        order.setPayment(true);
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, MessageType.UPDATE_STATUS);

        log.info(
                "Payment marked successful | orderId={} | restaurantId={}",
                savedOrder.getOrderId(),
                savedOrder.getRestaurantId()
        );

        return mapper.toOrderResponse(savedOrder);
    }

    @Transactional
    public void paymentSuccess(String orderId) {
        markPaymentSuccess(orderId);
    }

    @Transactional(readOnly = true)
    public OrderSnapshotResponse checkRunningOrder(Long restaurantId, Long tableNumber) {

        return orderRepository.findByRestaurantIdAndTableNumberAndStatusIn(
                        restaurantId, tableNumber, RUNNING_STATUSES)
                .map(mapper::toSnapshot)
                .orElse(null);
    }

    public MessageResponse checkRunningOrderStatus(Long restaurantId, Long tableNumber) {

        OrderSnapshotResponse snapshot = checkRunningOrder(restaurantId, tableNumber);

        if (snapshot != null) {
            return MessageResponse.success("Order already running.");
        }

        return MessageResponse.error("No running order.");
    }

    public MessageResponse checkRunningOrderByToken(String sessionToken) {
        TableSessionResponse session = validateAndGetActiveSession(sessionToken);
        return checkRunningOrderStatus(session.getRestaurantId(), session.getTableNumber());
    }

    @Transactional(readOnly = true)
    public List<OrderListResponse> getRunningOrders(Long restaurantId) {

        List<Order> orders = orderRepository.findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
                        restaurantId, RUNNING_STATUSES);

        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .map(mapper::toListResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public Page<OrderListResponse> getOrders(Long restaurantId, Pageable pageable) {

        Pageable safePageable = createSafePageable(pageable);

        Page<Order> orders = orderRepository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                        restaurantId, OrderStatus.COMPLETED, safePageable);

        return orders.map(mapper::toListResponse);
    }

    public Page<OrderListResponse> getOrders(Long restaurantId) {

        return getOrders(restaurantId,
                PageRequest.of(0, 9,
                        Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long restaurantId, String orderId) {
        validateOrderId(orderId);

        Order order = orderRepository
                .findByRestaurantIdAndOrderId(restaurantId, orderId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Order not found for restaurant: " + restaurantId));

        return mapper.toOrderResponse(order);
    }

    // ---------------------------------------------------------
    // TABLE SESSION VALIDATION
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public TableSessionResponse validateAndGetActiveSession(
            String sessionToken) {

        if (sessionToken == null || sessionToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Session token is required."
            );
        }

        String token = sessionToken.trim();

        TableSessionResponse session;

        try {
            session = tableSessionClient.getSession(token);
        } catch (Exception ex) {
            log.error("Failed to retrieve table session from Table Service | token={}",
                    maskToken(token), ex);

            throw new IllegalStateException("Unable to validate table session.");
        }

        if (session == null) {
            throw new IllegalStateException("Table session not found.");
        }

        validateSessionData(session, token);

        return session;
    }

    private void validateSessionData(TableSessionResponse session, String requestedToken) {


        if (session.getSessionToken() == null || session.getSessionToken().isBlank()) {
            throw new IllegalStateException("Table Service returned an empty session token.");
        }

        if (!requestedToken.equals(session.getSessionToken())) {
            throw new IllegalStateException("Table session token mismatch.");
        }

        if (session.getSessionStatus() == null) {
            throw new IllegalStateException("Table session status is missing.");
        }

        if (session.getSessionStatus() != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Table session is not active.");
        }
    }


    private void addItemsToOrder(Order order, AddItemRequest request) {

        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }

        validateAddItemRequest(request);

        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        for (OrderItemDTO dto : request.getItems()) {

            validateItem(dto);

            String currency = normalizeCurrency(dto.getCurrency());

            OrderItem newItem = OrderItem.builder()
                    .order(order)
                    .name(dto.getName().trim())
                    .price(dto.getPrice())
                    .quantity(dto.getQuantity())
                    .imageUrl(dto.getImageUrl())
                    .currency(currency)
                    .build();

            order.getItems().add(newItem);
        }
    }


    private void removeItemFromOrder(Order order, OrderItem item) {

        if (order == null || item == null) {
            throw new IllegalArgumentException(
                    "Order and order item are required.");
        }

        if (order.getItems() != null) {
            order.getItems().removeIf(existing -> existing != null
                            && Objects.equals(existing.getId(), item.getId()));
        }

    }

    private OrderItem getOrderItem(Long orderItemId) {
        return orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new IllegalStateException(
                                "Order item not found with id: " + orderItemId));
    }

    private Order getOrderByDatabaseId(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException(
                                "Order not found with id: " + orderId));
    }


    private void validateOrderBelongsToSession(Order order, TableSessionResponse session) {

        if (order == null || session == null) {
            throw new IllegalArgumentException("Order and table session are required.");
        }

        if (!Objects.equals(order.getRestaurantId(), session.getRestaurantId())) {
            throw new IllegalStateException("Order does not belong to this restaurant.");
        }

        if (!Objects.equals(order.getTableId(), session.getTableId())) {
            throw new IllegalStateException("Order does not belong to this table.");
        }

        if (!Objects.equals(order.getSessionId(), session.getSessionId())) {
            throw new IllegalStateException("Order does not belong to this table session.");
        }

        if (!Objects.equals(order.getSessionToken(), session.getSessionToken())) {
            throw new IllegalStateException("Order session token mismatch.");
        }
    }

    private void validateOrderModifiable(Order order) {

        if (order == null || order.getStatus() == null) {
            throw new IllegalStateException(
                    "Order or order status is missing.");
        }

        if (order.isPayment()) {
            throw new IllegalStateException(
                    "Paid order cannot be modified.");
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed order cannot be modified.");
        }
    }


    private void validateStatusTransition(OrderStatus current, OrderStatus requested, boolean paymentCompleted) {

        if (current == null) {
            throw new IllegalStateException(
                    "Current order status is missing.");
        }

        if (requested == null) {
            throw new IllegalArgumentException(
                    "New order status is required.");
        }

        if (current == requested) {
            throw new IllegalStateException(
                    "Order is already in status " + requested);
        }

        if (current == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed order cannot be modified.");
        }

        if (requested == OrderStatus.COMPLETED && !paymentCompleted) {
            throw new IllegalStateException(
                    "Order cannot be completed before payment.");
        }

        boolean valid = switch (current) {
            case PENDING ->
                    requested == OrderStatus.PREPARING;

            case PREPARING ->
                    requested == OrderStatus.SERVED;

            case SERVED ->
                    requested == OrderStatus.PREPARING
                            || requested == OrderStatus.COMPLETED;

            case COMPLETED -> false;
        };

        if (!valid) {
            throw new IllegalStateException("Invalid order status transition: "
                            + current + " -> " + requested);
        }
    }

    private void recalculateTotals(Order order) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null."
            );
        }

        BigDecimal subtotal = BigDecimal.ZERO;

        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {

                if (item == null) {
                    continue;
                }

                validatePersistedItem(item);

                BigDecimal lineTotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

                subtotal = subtotal.add(lineTotal);
            }
        }

        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal taxPercentage = resolveTaxPercentage();

        BigDecimal taxAmount = subtotal.multiply(taxPercentage).divide(
                        BigDecimal.valueOf(100),
                        2,
                        RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        order.setSubtotal(subtotal);
        order.setTaxPercentage(taxPercentage);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
    }

    private void validatePersistedItem(OrderItem item) {

        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Order contains an invalid item price.");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0 || item.getQuantity() > MAX_ITEM_QUANTITY) {
            throw new IllegalStateException("Order contains an invalid item quantity.");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalStateException("Order contains an item without a name.");
        }
    }

    private BigDecimal resolveTaxPercentage() {

        if (defaultTaxPercentage == null || defaultTaxPercentage.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Configured tax percentage cannot be null or negative.");
        }

        return defaultTaxPercentage.setScale(2, RoundingMode.HALF_UP);
    }


    private void validateAddItemRequest(AddItemRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Add item request cannot be null.");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one order item is required.");
        }

        if (request.getItems().size() > 100) {
            throw new IllegalArgumentException("Cannot submit more than 100 items at once.");
        }

        if (request.getItems().stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Order item cannot be null.");
        }
    }

    private void validateItem(OrderItemDTO item) {

        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null.");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new IllegalArgumentException("Item name is required.");
        }

        if (item.getName().trim().length() > 255) {
            throw new IllegalArgumentException("Item name cannot exceed 255 characters.");
        }

        if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Item price cannot be null or negative.");
        }

        if (item.getQuantity() == null || item.getQuantity() <= 0
                || item.getQuantity() > MAX_ITEM_QUANTITY) {
            throw new IllegalArgumentException("Item quantity must be between 1 and "
                            + MAX_ITEM_QUANTITY + ".");
        }

        if (item.getCurrency() != null && !item.getCurrency().isBlank()
                && item.getCurrency().trim().length() != 3) {
            throw new IllegalArgumentException(
                    "Currency must be a 3-letter code.");
        }

        if (item.getImageUrl() != null && item.getImageUrl().length() > 2048) {
            throw new IllegalArgumentException("Image URL cannot exceed 2048 characters.");
        }
    }

    private void validateRemoveItemRequest(RemoveItemRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Remove item request cannot be null.");
        }

        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("Order ID is required");
        }

        if (request.getItemId() == null || request.getItemId() <= 0) {
            throw new IllegalArgumentException("Item ID must be positive");
        }

    }

    private void validateOrderId(String orderId) {

        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required.");
        }

        if (orderId.length() > 36) {
            throw new IllegalArgumentException("Order ID cannot exceed 36 characters.");
        }
    }


    private String normalizeCurrency(String currency) {

        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }

        String normalized = currency.trim().toUpperCase(Locale.ROOT);

        if (normalized.length() != 3) {
            throw new IllegalArgumentException(
                    "Currency must be a 3-letter code.");
        }

        return normalized;
    }


    private void broadcastOrderUpdate(Order order, MessageType messageType) {

        OrderSnapshotResponse snapshot = mapper.toSnapshot(order);

        broadcastSnapshot(
                snapshot,
                order.getRestaurantId(),
                order.getTableNumber(),
                messageType,
                order.getOrderId()
        );
    }

    private void broadcastSnapshot(OrderSnapshotResponse snapshot,
            Long restaurantId, Long tableNumber, MessageType messageType, String publicOrderId) {

        Runnable pushTask = () -> {
            try {
                WsMessage<OrderSnapshotResponse> message =
                        new WsMessage<>(messageType.name(), snapshot);

                push.broadcastToTable(restaurantId, tableNumber, message);

                push.broadcastToStaff(restaurantId, message);

                log.debug("Order update broadcast | type={} | orderId={} | restaurantId={} | tableNumber={}",
                        messageType, publicOrderId, restaurantId, tableNumber);

            } catch (Exception ex) {
                log.error("Failed to broadcast order update | type={} | orderId={}",
                        messageType, publicOrderId, ex);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            pushTask.run();
                        }
                    }
            );
        } else {
            pushTask.run();
        }
    }


    private Pageable createSafePageable(Pageable pageable) {

        if (pageable == null) {
            return PageRequest.of(
                    0,
                    9,
                    Sort.by(Sort.Direction.DESC, "createdAt")
            );
        }

        int page = Math.max(pageable.getPageNumber(), 0);
        int size = Math.min(Math.max(pageable.getPageSize(), 1), 100);

        return PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private String maskToken(String token) {

        if (token == null || token.length() <= 8) {
            return "***";
        }

        return token.substring(0, 4)
                + "***"
                + token.substring(token.length() - 4);
    }
}
