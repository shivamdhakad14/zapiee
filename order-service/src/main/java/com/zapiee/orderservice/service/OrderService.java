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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Order Service",
        description = "Business operations for restaurant orders, table sessions and real-time order updates."
)
public class OrderService {

    private static final String DEFAULT_CURRENCY = "INR";

    private static final List<OrderStatus> RUNNING_STATUSES =
            List.of(
                    OrderStatus.PENDING,
                    OrderStatus.PREPARING,
                    OrderStatus.SERVED
            );

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderMapper mapper;
    private final TableSessionClient tableSessionClient;
    private final WebSocketPushService push;

    /**
     * Default tax percentage.
     *
     * This should normally come from application.properties:
     *
     * zapiee.order.tax-percentage=5
     */
    @Value("${zapiee.order.tax-percentage:0}")
    private BigDecimal defaultTaxPercentage;


    @Transactional
    @Operation(
            summary = "Create first order",
            description = "Creates the first running order for an active Table Service session."
    )
    public OrderResponse placeFirstOrder(String sessionToken, AddItemRequest request) {

        TableSessionResponse session = validateAndGetActiveSession(sessionToken);
        validateAddItemRequest(request);

        Optional<Order> existingOrder =
                orderRepository.findBySessionId(session.getSessionId());

        if (existingOrder.isPresent()) {
            throw new IllegalStateException(
                    "A running order already exists for this table session. Use NEW_ITEM.");
        }

        Order order = Order.builder()
                .restaurantId(session.getRestaurantId())
                .tableId(session.getTableId())
                .tableNumber(session.getTableNumber())
                .sessionId(session.getSessionId())
                .sessionToken(session.getSessionToken())
                .status(OrderStatus.PENDING)
                .payment(false)
                .subtotal(BigDecimal.ZERO)
                .taxPercentage(resolveTaxPercentage())
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        addItemsToOrder(order, request);

        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(savedOrder, "NEW_ITEM");

        log.info(
                "First order created | orderId={} | restaurantId={} | tableId={} | sessionId={}",
                savedOrder.getOrderId(),
                savedOrder.getRestaurantId(),
                savedOrder.getTableId(),
                savedOrder.getSessionId()
        );

        return mapper.toOrderResponse(savedOrder);
    }


    // ============================================================
    // CUSTOMER - ADD NEW ITEMS
    // ============================================================

    /**
     * Adds additional items to an existing running order.
     */
    @Transactional
    @Operation(
            summary = "Add new items",
            description = "Adds items to the existing running order of an active table session."
    )
    public OrderResponse addNewItems(
            String sessionToken,
            AddItemRequest request
    ) {

        TableSessionResponse session =
                validateAndGetActiveSession(sessionToken);

        validateAddItemRequest(request);

        Order order =
                orderRepository.findBySessionId(session.getSessionId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No running order exists for this table session."
                                )
                        );

        validateOrderBelongsToSession(order, session);

        validateOrderModifiable(order);

        OrderStatus previousStatus = order.getStatus();

        addItemsToOrder(order, request);

        /*
         * If an order was already SERVED and customer adds new items,
         * the new items require preparation.
         */
        if (previousStatus == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PREPARING);
        }

        /*
         * If order is PENDING, keep it PENDING.
         *
         * If order is PREPARING, keep it PREPARING.
         */
        recalculateTotals(order);

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "NEW_ITEM"
        );

        log.info(
                "Items added | orderId={} | sessionId={} | previousStatus={} | newStatus={}",
                savedOrder.getOrderId(),
                session.getSessionId(),
                previousStatus,
                savedOrder.getStatus()
        );

        return mapper.toOrderResponse(savedOrder);
    }


    // ============================================================
    // CUSTOMER - REMOVE ITEM
    // ============================================================

    /**
     * Removes an item from an order.
     *
     * Customer can remove items only while the order is PENDING.
     */
    @Transactional
    @Operation(
            summary = "Remove order item",
            description = "Removes an item from a pending order belonging to the active table session."
    )
    public OrderResponse removeItem(
            String sessionToken,
            RemoveItemRequest request
    ) {

        TableSessionResponse session =
                validateAndGetActiveSession(sessionToken);

        validateRemoveItemRequest(request);

        Order order =
                orderRepository.findById(request.getId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: "
                                                + request.getId()
                                )
                        );

        validateOrderBelongsToSession(order, session);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException(
                    "Items can only be removed while the order is PENDING."
            );
        }

        OrderItem item =
                orderItemRepository.findById(request.getItemId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order item not found with id: "
                                                + request.getItemId()
                                )
                        );

        if (item.getOrder() == null ||
                !Objects.equals(
                        item.getOrder().getId(),
                        order.getId()
                )) {

            throw new IllegalStateException(
                    "Order item does not belong to the requested order."
            );
        }

        order.getItems().remove(item);

        orderItemRepository.delete(item);

        /*
         * If no items remain, do not keep an empty running order.
         */
        if (order.getItems().isEmpty()) {

            order.setSubtotal(BigDecimal.ZERO);
            order.setTaxAmount(BigDecimal.ZERO);
            order.setTotalAmount(BigDecimal.ZERO);

            /*
             * We don't close the Table Service session here.
             * Table Service owns the session lifecycle.
             */
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());

        } else {
            recalculateTotals(order);
        }

        Order savedOrder = orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "ITEM_REMOVED"
        );

        return mapper.toOrderResponse(savedOrder);
    }


    // ============================================================
    // STAFF / OWNER - UPDATE STATUS
    // ============================================================

    /**
     * Updates order status.
     *
     * This method is intended for STAFF / OWNER WebSocket operations.
     *
     * Restaurant authorization must be performed before calling this
     * method or by using the restaurantId overload.
     */
    @Transactional
    @Operation(
            summary = "Update order status",
            description = "Updates the status of an order and broadcasts the updated order."
    )
    public OrderResponse updateStatusByOrderId(
            Long restaurantId,
            Long orderId,
            OrderStatus newStatus
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validatePositiveId(
                orderId,
                "Order ID"
        );

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Order status cannot be null."
            );
        }

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: " + orderId
                                )
                        );

        /*
         * Never allow STAFF/OWNER to update another restaurant's order.
         */
        if (!Objects.equals(
                order.getRestaurantId(),
                restaurantId
        )) {

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

        Order savedOrder =
                orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "UPDATE_STATUS"
        );

        log.info(
                "Order status updated | restaurantId={} | orderId={} | {} -> {}",
                restaurantId,
                orderId,
                previousStatus,
                newStatus
        );

        return mapper.toOrderResponse(savedOrder);
    }


    /**
     * Compatibility overload for existing code.
     *
     * This should eventually be replaced by the restaurant-aware method.
     */
    @Transactional
    public OrderResponse updateStatusByOrderId(
            Long orderId,
            OrderStatus newStatus
    ) {

        validatePositiveId(
                orderId,
                "Order ID"
        );

        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Order status cannot be null."
            );
        }

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: " + orderId
                                )
                        );

        validateStatusTransition(
                order.getStatus(),
                newStatus,
                order.isPayment()
        );

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(LocalDateTime.now());
        }

        Order savedOrder =
                orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "UPDATE_STATUS"
        );

        return mapper.toOrderResponse(savedOrder);
    }


    @Transactional
    @Operation(summary = "Add items by restaurant",
            description = "Allows Staff/Owner to add items to an existing restaurant order."
    )
    public OrderResponse addItemsByRestaurant(
            Long restaurantId,
            AddItemRequest request,
            MessageType messageType
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validateAddItemRequest(request);

        if (messageType == null) {
            throw new IllegalArgumentException(
                    "Message type is required."
            );
        }

        if (messageType != MessageType.ADD_ITEM &&
                messageType != MessageType.NEW_ITEM) {

            throw new IllegalArgumentException(
                    "Invalid message type for adding items: "
                            + messageType
            );
        }

        Long orderId = request.getId();

        validatePositiveId(orderId, "Order ID");

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: " + orderId
                                )
                        );

        /*
         * IMPORTANT:
         * Never allow Staff/Owner to modify another restaurant's order.
         */
        if (!Objects.equals(
                order.getRestaurantId(),
                restaurantId
        )) {

            throw new IllegalStateException(
                    "Order does not belong to the requested restaurant."
            );
        }

        validateOrderModifiable(order);

        OrderStatus previousStatus =
                order.getStatus();

        addItemsToOrder(
                order,
                request
        );

        /*
         * If the order was already SERVED and Staff/Owner
         * adds another item, it needs preparation again.
         */
        if (previousStatus == OrderStatus.SERVED) {
            order.setStatus(OrderStatus.PREPARING);
        }

        recalculateTotals(order);

        Order savedOrder =
                orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                messageType.name()
        );

        log.info(
                "Staff/Owner added items | restaurantId={} | orderId={} | previousStatus={} | newStatus={}",
                restaurantId,
                savedOrder.getOrderId(),
                previousStatus,
                savedOrder.getStatus()
        );

        return mapper.toOrderResponse(savedOrder);
    }


    /**
     * Staff/Owner:
     * Removes an item from an order within the restaurant.
     *
     * Restaurant scope is mandatory.
     */
    @Transactional
    @Operation(
            summary = "Remove item by restaurant",
            description = "Allows Staff/Owner to remove an item from an order belonging to their restaurant."
    )
    public OrderResponse removeItemByRestaurant(
            Long restaurantId,
            RemoveItemRequest request
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validateRemoveItemRequest(request);

        Order order =
                orderRepository.findById(
                                request.getId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: "
                                                + request.getId()
                                )
                        );

        /*
         * Restaurant authorization.
         */
        if (!Objects.equals(
                order.getRestaurantId(),
                restaurantId
        )) {

            throw new IllegalStateException(
                    "Order does not belong to the requested restaurant."
            );
        }

        validateOrderModifiable(order);

        OrderItem item =
                orderItemRepository.findById(
                                request.getItemId()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order item not found with id: "
                                                + request.getItemId()
                                )
                        );

        /*
         * Prevent deleting an item belonging to another order.
         */
        if (item.getOrder() == null ||
                !Objects.equals(
                        item.getOrder().getId(),
                        order.getId()
                )) {

            throw new IllegalStateException(
                    "Order item does not belong to the requested order."
            );
        }

        /*
         * Staff/Owner should not remove items from a completed/paid order.
         */
        if (order.isPayment()) {
            throw new IllegalStateException(
                    "Paid order cannot be modified."
            );
        }

        order.getItems().remove(item);

        orderItemRepository.delete(item);

        /*
         * Recalculate the complete financial state.
         */
        if (order.getItems().isEmpty()) {

            order.setSubtotal(
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );

            order.setTaxAmount(
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );

            order.setTotalAmount(
                    BigDecimal.ZERO.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );

            /*
             * Do NOT mark the order COMPLETED merely because
             * all items were removed.
             *
             * Completion should be controlled by the proper
             * order/payment lifecycle.
             */
            order.setStatus(OrderStatus.PENDING);

        } else {

            recalculateTotals(order);
        }

        Order savedOrder =
                orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "ITEM_REMOVED"
        );

        log.info(
                "Staff/Owner removed item | restaurantId={} | orderId={} | orderItemId={}",
                restaurantId,
                savedOrder.getOrderId(),
                request.getItemId()
        );

        return mapper.toOrderResponse(savedOrder);
    }


    // ============================================================
    // PAYMENT
    // ============================================================

    /**
     * Marks payment as successfully completed.
     *
     * Important:
     * Payment Service should call this endpoint after successful payment.
     */
    @Transactional
    @Operation(
            summary = "Mark payment successful",
            description = "Marks an order as paid and completes the order."
    )
    public OrderResponse markPaymentSuccess(
            Long orderId
    ) {

        validatePositiveId(
                orderId,
                "Order ID"
        );

        Order order =
                orderRepository.findById(orderId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Order not found with id: " + orderId
                                )
                        );

        if (order.isPayment()) {
            return mapper.toOrderResponse(order);
        }

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Order is already completed but payment is not marked."
            );
        }

        order.setPayment(true);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order savedOrder =
                orderRepository.save(order);

        broadcastOrderUpdate(
                savedOrder,
                "ORDER_COMPLETED"
        );

        log.info(
                "Payment completed | orderId={} | restaurantId={}",
                savedOrder.getOrderId(),
                savedOrder.getRestaurantId()
        );

        return mapper.toOrderResponse(savedOrder);
    }


    /**
     * Backward-compatible method.
     */
    @Transactional
    public void paymentSuccess(Long orderId) {
        markPaymentSuccess(orderId);
    }


    // ============================================================
    // CUSTOMER - RUNNING ORDER
    // ============================================================

    /**
     * Finds the running order for a restaurant/table.
     */
    @Transactional
    @Operation(
            summary = "Check running order",
            description = "Returns the running order for a restaurant table."
    )
    public OrderSnapshotResponse checkRunningOrder(
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

        Optional<Order> runningOrder =
                orderRepository
                        .findByRestaurantIdAndTableNumberAndStatusIn(
                                restaurantId,
                                tableNumber,
                                RUNNING_STATUSES
                        );

        return runningOrder
                .map(mapper::toSnapshot)
                .orElse(null);
    }


    /**
     * Checks whether a table already has a running order.
     */
    @Operation(
            summary = "Check running order status",
            description = "Checks whether a restaurant table already has a running order."
    )
    public MessageResponse checkRunningOrderStatus(
            Long restaurantId,
            Long tableNumber
    ) {

        OrderSnapshotResponse snapshot =
                checkRunningOrder(
                        restaurantId,
                        tableNumber
                );

        if (snapshot != null) {
            return MessageResponse.success(
                    "Order already running."
            );
        }

        return MessageResponse.error(
                "No running order."
        );
    }


    /**
     * Backward-compatible name used by older controller code.
     */
    public MessageResponse checkRunningOrderByToken(
            String sessionToken
    ) {

        TableSessionResponse session =
                validateAndGetActiveSession(sessionToken);

        return checkRunningOrderStatus(
                session.getRestaurantId(),
                session.getTableNumber()
        );
    }


    @Transactional
    @Operation(summary = "Get running orders",
            description = "Returns PENDING, PREPARING and SERVED orders for a restaurant."
    )
    public List<OrderListResponse> getRunningOrders(
            Long restaurantId
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        List<Order> orders =
                orderRepository
                        .findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
                                restaurantId,
                                RUNNING_STATUSES
                        );

        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .filter(Objects::nonNull)
                .map(mapper::toListResponse)
                .toList();
    }


    // ============================================================
    // ORDER HISTORY
    // ============================================================

    /**
     * Returns completed orders for restaurant history.
     */
    @Transactional
    @Operation(
            summary = "Get completed orders",
            description = "Returns paginated completed order history."
    )
    public Page<OrderListResponse> getOrders(
            Long restaurantId,
            Pageable pageable
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        Pageable safePageable =
                createSafePageable(pageable);

        Page<Order> orders =
                orderRepository
                        .findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                                restaurantId,
                                OrderStatus.COMPLETED,
                                safePageable
                        );

        return orders.map(mapper::toListResponse);
    }


    /**
     * Default pagination method.
     *
     * 9 orders per page as requested by the dashboard requirement.
     */
    public Page<OrderListResponse> getOrders(
            Long restaurantId
    ) {

        return getOrders(
                restaurantId,
                PageRequest.of(
                        0,
                        9,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                )
        );
    }


    // ============================================================
    // GET SINGLE ORDER
    // ============================================================

    /**
     * Returns detailed order information.
     */
    @Transactional
    @Operation(
            summary = "Get order",
            description = "Returns detailed information about an order."
    )
    public OrderResponse getOrder(
            Long restaurantId,
            String orderId
    ) {

        validatePositiveId(
                restaurantId,
                "Restaurant ID"
        );

        validateOrderId(orderId);

        Order order =
                orderRepository
                        .findByRestaurantIdAndOrderId(
                                restaurantId,
                                orderId
                        );

        if (order == null) {
            throw new IllegalStateException(
                    "Order not found with id: "
                            + orderId
                            + " for restaurant: "
                            + restaurantId
            );
        }

        return mapper.toOrderResponse(order);
    }


    // ============================================================
    // SESSION VALIDATION
    // ============================================================

    /**
     * Gets and validates the Table Service session.
     *
     * Order Service NEVER creates the session.
     */
    @Transactional
    public TableSessionResponse validateAndGetActiveSession(
            String sessionToken
    ) {

        if (sessionToken == null ||
                sessionToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Session token is required."
            );
        }

        final String token =
                sessionToken.trim();

        TableSessionResponse session;

        try {

            session =
                    tableSessionClient.getSession(token);

        } catch (Exception ex) {

            log.error(
                    "Failed to retrieve table session from Table Service | token={}",
                    maskToken(token),
                    ex
            );

            throw new IllegalStateException(
                    "Unable to validate table session."
            );
        }

        if (session == null) {
            throw new IllegalStateException(
                    "Table session not found."
            );
        }

        validateSessionData(
                session,
                token
        );

        return session;
    }


    /**
     * Validates complete Table Service response.
     */
    private void validateSessionData(
            TableSessionResponse session,
            String requestedToken
    ) {

        if (session.getSessionId() == null ||
                session.getSessionId() <= 0) {

            throw new IllegalStateException(
                    "Invalid table session ID."
            );
        }

        if (session.getRestaurantId() == null ||
                session.getRestaurantId() <= 0) {

            throw new IllegalStateException(
                    "Invalid restaurant ID returned by Table Service."
            );
        }

        if (session.getTableId() == null ||
                session.getTableId() <= 0) {

            throw new IllegalStateException(
                    "Invalid table ID returned by Table Service."
            );
        }

        if (session.getTableNumber() == null ||
                session.getTableNumber() <= 0) {

            throw new IllegalStateException(
                    "Invalid table number returned by Table Service."
            );
        }

        if (session.getSessionToken() == null ||
                session.getSessionToken().isBlank()) {

            throw new IllegalStateException(
                    "Table Service returned an empty session token."
            );
        }

        if (!requestedToken.equals(
                session.getSessionToken()
        )) {

            throw new IllegalStateException(
                    "Table session token mismatch."
            );
        }

        if (session.getSessionStatus() == null) {
            throw new IllegalStateException(
                    "Table session status is missing."
            );
        }

        if (session.getSessionStatus() !=
                SessionStatus.ACTIVE) {

            throw new IllegalStateException(
                    "Table session is not active."
            );
        }

//        if (session.getExpiresAt() == null) {
//            throw new IllegalStateException("Table session expiry time is missing.");
//        }
//
//        if (!session.getExpiresAt().isAfter(LocalDateTime.now())) {
//            throw new IllegalStateException("Table session has expired.");
//        }

    }


    // ============================================================
    // ITEM OPERATIONS
    // ============================================================

    /**
     * Adds request items into an order.
     *
     * Client supplied totals are NEVER trusted.
     */
    private void addItemsToOrder(
            Order order,
            AddItemRequest request
    ) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null."
            );
        }

        validateAddItemRequest(request);

        for (OrderItemDTO dto :
                request.getItems()) {

            validateItem(dto);

            Optional<OrderItem> existingItem =
                    order.getItems()
                            .stream()
                            .filter(Objects::nonNull)
                            .filter(item ->
                                    Objects.equals(
                                            item.getId(),
                                            dto.getItemId()
                                    )
                            )
                            .findFirst();

            if (existingItem.isPresent()) {

                OrderItem item =
                        existingItem.get();

                int currentQuantity =
                        item.getQuantity() == null
                                ? 0
                                : item.getQuantity();

                long newQuantity =
                        (long) currentQuantity
                                + dto.getQuantity();

                if (newQuantity > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException(
                            "Item quantity is too large."
                    );
                }

                item.setQuantity(
                        (int) newQuantity
                );

            } else {

                OrderItem newItem =
                        OrderItem.builder()
                                .order(order)
                                .id(dto.getItemId())
                                .name(dto.getName().trim())
                                .price(dto.getPrice())
                                .quantity(dto.getQuantity())
                                .imageUrl(dto.getImageUrl())
                                .currency(
                                        dto.getCurrency() == null ||
                                                dto.getCurrency().isBlank()
                                                ? DEFAULT_CURRENCY
                                                : dto.getCurrency()
                                                  .trim()
                                                  .toUpperCase()
                                )
                                .build();

                order.getItems()
                        .add(newItem);
            }
        }
    }


    // ============================================================
    // TOTAL CALCULATION
    // ============================================================

    /**
     * Recalculates subtotal, tax and total.
     *
     * All monetary values are calculated inside Order Service.
     */
    private void recalculateTotals(
            Order order
    ) {

        if (order == null) {
            throw new IllegalArgumentException(
                    "Order cannot be null."
            );
        }

        BigDecimal subtotal =
                BigDecimal.ZERO;

        if (order.getItems() != null) {

            for (OrderItem item :
                    order.getItems()) {

                if (item == null) {
                    continue;
                }

                validatePersistedItem(item);

                BigDecimal lineTotal =
                        item.getPrice()
                                .multiply(
                                        BigDecimal.valueOf(
                                                item.getQuantity()
                                        )
                                );

                subtotal =
                        subtotal.add(lineTotal);
            }
        }

        subtotal =
                subtotal.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

        BigDecimal taxPercentage =
                resolveTaxPercentage();

        BigDecimal taxAmount =
                subtotal
                        .multiply(taxPercentage)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

        BigDecimal totalAmount =
                subtotal
                        .add(taxAmount)
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        order.setSubtotal(subtotal);
        order.setTaxPercentage(taxPercentage);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(totalAmount);
    }


    // ============================================================
    // ORDER VALIDATION
    // ============================================================

    private void validateOrderBelongsToSession(
            Order order,
            TableSessionResponse session
    ) {

        if (order == null ||
                session == null) {

            throw new IllegalArgumentException(
                    "Order and table session are required."
            );
        }

        if (!Objects.equals(
                order.getRestaurantId(),
                session.getRestaurantId()
        )) {

            throw new IllegalStateException(
                    "Order does not belong to this restaurant."
            );
        }

        if (!Objects.equals(
                order.getTableId(),
                session.getTableId()
        )) {

            throw new IllegalStateException(
                    "Order does not belong to this table."
            );
        }

        if (!Objects.equals(
                order.getSessionId(),
                session.getSessionId()
        )) {

            throw new IllegalStateException(
                    "Order does not belong to this table session."
            );
        }

        if (!Objects.equals(
                order.getSessionToken(),
                session.getSessionToken()
        )) {

            throw new IllegalStateException(
                    "Order session token mismatch."
            );
        }
    }


    private void validateOrderModifiable(
            Order order
    ) {

        if (order.getStatus() == null) {
            throw new IllegalStateException(
                    "Order status is missing."
            );
        }

        if (order.isPayment()) {
            throw new IllegalStateException(
                    "Paid order cannot be modified."
            );
        }

        if (order.getStatus() ==
                OrderStatus.COMPLETED) {

            throw new IllegalStateException(
                    "Completed order cannot be modified."
            );
        }
    }


    /**
     * Valid status transitions.
     */
    private void validateStatusTransition(
            OrderStatus current,
            OrderStatus requested,
            boolean paymentCompleted
    ) {

        if (current == null) {
            throw new IllegalStateException(
                    "Current order status is missing."
            );
        }

        if (requested == null) {
            throw new IllegalArgumentException(
                    "New order status is required."
            );
        }

        if (current == requested) {
            throw new IllegalStateException(
                    "Order is already in status " + requested
            );
        }

        if (current == OrderStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Completed order cannot be modified."
            );
        }

        if (requested == OrderStatus.COMPLETED &&
                !paymentCompleted) {

            throw new IllegalStateException(
                    "Order cannot be completed before payment."
            );
        }

        boolean valid;

        valid = switch (current) {

            case PENDING ->
                    requested == OrderStatus.PREPARING;

            case PREPARING ->
                    requested == OrderStatus.SERVED;

            case SERVED ->
                    requested == OrderStatus.PREPARING ||
                            requested == OrderStatus.COMPLETED;

            case COMPLETED ->
                    false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid order status transition: "
                            + current
                            + " -> "
                            + requested
            );
        }
    }


    // ============================================================
    // REQUEST VALIDATION
    // ============================================================

    private void validateAddItemRequest(
            AddItemRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Add item request cannot be null."
            );
        }

        if (request.getItems() == null ||
                request.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one order item is required."
            );
        }
    }


    private void validateItem(
            OrderItemDTO item
    ) {

        if (item == null) {
            throw new IllegalArgumentException(
                    "Order item cannot be null."
            );
        }

        validatePositiveId(
                item.getItemId(),
                "Item ID"
        );

        if (item.getName() == null ||
                item.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Item name is required."
            );
        }

        if (item.getPrice() == null ||
                item.getPrice()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Item price cannot be negative."
            );
        }

        if (item.getQuantity() == null ||
                item.getQuantity() <= 0) {

            throw new IllegalArgumentException(
                    "Item quantity must be greater than zero."
            );
        }

        if (item.getQuantity() > 1000) {
            throw new IllegalArgumentException(
                    "Item quantity cannot exceed 1000."
            );
        }

        if (item.getCurrency() != null &&
                !item.getCurrency().isBlank() &&
                item.getCurrency().trim().length() != 3) {

            throw new IllegalArgumentException(
                    "Currency must be a valid 3-letter currency code."
            );
        }
    }


    private void validatePersistedItem(
            OrderItem item
    ) {

        if (item.getPrice() == null ||
                item.getPrice()
                        .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalStateException(
                    "Order contains an invalid item price."
            );
        }

        if (item.getQuantity() == null ||
                item.getQuantity() <= 0) {

            throw new IllegalStateException(
                    "Order contains an invalid item quantity."
            );
        }
    }


    private void validateRemoveItemRequest(
            RemoveItemRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Remove item request cannot be null."
            );
        }

        validatePositiveId(
                request.getId(),
                "Order ID"
        );

        validatePositiveId(
                request.getItemId(),
                "Order item ID"
        );
    }


    private void validateOrderId(
            String orderId
    ) {

        if (orderId == null ||
                orderId.isBlank()) {

            throw new IllegalArgumentException(
                    "Order ID is required."
            );
        }
    }


    private void validatePositiveId(
            Long id,
            String fieldName
    ) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than zero."
            );
        }
    }


    // ============================================================
    // WEBSOCKET
    // ============================================================

    /**
     * Broadcasts the latest order snapshot to:
     *
     * CUSTOMER -> table
     * STAFF/OWNER -> restaurant
     */
    private void broadcastOrderUpdate(
            Order order,
            String messageType
    ) {

        if (order == null) {
            return;
        }

        OrderSnapshotResponse snapshot =
                mapper.toSnapshot(order);

        WsMessage<OrderSnapshotResponse> message =
                new WsMessage<>(
                        messageType,
                        snapshot
                );

        push.broadcastToTable(
                order.getRestaurantId(),
                order.getTableNumber(),
                message
        );

        push.broadcastToStaff(
                order.getRestaurantId(),
                message
        );

        log.debug(
                "Order update broadcast | type={} | orderId={} | restaurantId={} | tableNumber={}",
                messageType,
                order.getOrderId(),
                order.getRestaurantId(),
                order.getTableNumber()
        );
    }


    // ============================================================
    // PAGINATION
    // ============================================================

    private Pageable createSafePageable(
            Pageable pageable
    ) {

        if (pageable == null) {
            return PageRequest.of(
                    0,
                    9,
                    Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                    )
            );
        }

        int page =
                Math.max(
                        pageable.getPageNumber(),
                        0
                );

        int size =
                Math.min(
                        Math.max(
                                pageable.getPageSize(),
                                1
                        ),
                        100
                );

        return PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );
    }


    private BigDecimal resolveTaxPercentage() {

        if (defaultTaxPercentage == null ||
                defaultTaxPercentage
                        .compareTo(BigDecimal.ZERO) < 0) {

            return BigDecimal.ZERO;
        }

        return defaultTaxPercentage
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }


    /**
     * Never write the complete session token to logs.
     */
    private String maskToken(
            String token
    ) {

        if (token == null ||
                token.length() <= 8) {

            return "***";
        }

        return token.substring(0, 4)
                + "***"
                + token.substring(
                token.length() - 4
        );
    }
}

