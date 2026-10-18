package com.zapiee.orderservice.repository;

import com.zapiee.orderservice.enums.OrderStatus;
import com.zapiee.orderservice.model.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Schema(
        name = "Order Repository",
        description = "Persistence repository for managing orders."
)
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find an order using restaurant ID and its public order UUID.
     *
     * Restaurant ID is included to prevent cross-restaurant access.
     */
    Order findByRestaurantIdAndOrderId(
            Long restaurantId,
            String orderId
    );

    /**
     * Find the currently associated order for a table session.
     *
     * The sessionToken is generated and owned by Table Service.
     * Order Service only stores/uses the token for association.
     */
    Optional<Order> findByRestaurantIdAndSessionToken(
            Long restaurantId,
            String sessionToken
    );

    Optional<Order> findBySessionId(Long sessionId);

    /**
     * Find an order for a restaurant and session token,
     * fetching items in the same query.
     *
     * This is useful when returning the customer's current order.
     */
    @Query("""
            SELECT DISTINCT o
            FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.restaurantId = :restaurantId
              AND o.sessionToken = :sessionToken
            """)
    Optional<Order> findByRestaurantIdAndSessionTokenWithItems(
            @Param("restaurantId") Long restaurantId,
            @Param("sessionToken") String sessionToken
    );

    /**
     * Find running orders for a restaurant.
     *
     * Running statuses are supplied by the service layer:
     * PENDING, PREPARING, SERVED.
     */
    Page<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
            Long restaurantId,
            Collection<OrderStatus> statuses,
            Pageable pageable
    );

    /**
     * Find orders of a specific status for a restaurant.
     */
    Page<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(
            Long restaurantId,
            OrderStatus status,
            Pageable pageable
    );

    /**
     * Find all orders with selected statuses for a restaurant.
     *
     * Useful for WebSocket staff/owner snapshots.
     */
    List<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
            Long restaurantId,
            Collection<OrderStatus> statuses
    );

    /**
     * Find a running order for a particular restaurant table.
     *
     * The service layer must supply only running statuses.
     */
    Optional<Order> findByRestaurantIdAndTableNumberAndStatusIn(
            Long restaurantId,
            Long tableNumber,
            Collection<OrderStatus> statuses
    );

    /**
     * Check whether an order exists for a restaurant.
     *
     * Useful before performing an operation on an order.
     */
    boolean existsByRestaurantIdAndOrderId(
            Long restaurantId,
            String orderId
    );

    /**
     * Count orders created from a specific time.
     *
     * Used for restaurant-level order statistics.
     */
    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.restaurantId = :restaurantId
              AND o.createdAt >= :startDateTime
            """)
    long countOrdersSince(
            @Param("restaurantId") Long restaurantId,
            @Param("startDateTime") LocalDateTime startDateTime
    );

    /**
     * Count orders for a restaurant within a date/time range.
     */
    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.restaurantId = :restaurantId
              AND o.createdAt >= :startDateTime
              AND o.createdAt < :endDateTime
            """)
    long countOrdersBetween(
            @Param("restaurantId") Long restaurantId,
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime
    );

    /**
     * Count orders by status for a restaurant.
     */
    long countByRestaurantIdAndStatus(
            Long restaurantId,
            OrderStatus status
    );

    /**
     * Count orders for a restaurant using multiple statuses.
     */
    long countByRestaurantIdAndStatusIn(
            Long restaurantId,
            Collection<OrderStatus> statuses
    );

    /**
     * Find orders created within a date/time range.
     *
     * Useful for order history and analytics.
     */
    Page<Order> findByRestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long restaurantId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    );
}

