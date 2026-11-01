
package com.zapiee.orderservice.repository;

import com.zapiee.orderservice.enums.OrderStatus;
import com.zapiee.orderservice.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByRestaurantIdAndOrderId(
            Long restaurantId,
            String orderId
    );

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByRestaurantIdAndSessionToken(
            Long restaurantId,
            String sessionToken
    );

    Optional<Order> findBySessionId(Long sessionId);

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

    Page<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
            Long restaurantId,
            Collection<OrderStatus> statuses,
            Pageable pageable
    );

    Page<Order> findByRestaurantIdAndStatusOrderByCreatedAtDesc(
            Long restaurantId,
            OrderStatus status,
            Pageable pageable
    );

    List<Order> findByRestaurantIdAndStatusInOrderByCreatedAtDesc(
            Long restaurantId,
            Collection<OrderStatus> statuses
    );

    Optional<Order> findByRestaurantIdAndTableNumberAndStatusIn(
            Long restaurantId,
            Long tableNumber,
            Collection<OrderStatus> statuses
    );

    boolean existsByRestaurantIdAndOrderId(
            Long restaurantId,
            String orderId
    );

    long countByRestaurantIdAndStatus(
            Long restaurantId,
            OrderStatus status
    );

    long countByRestaurantIdAndStatusIn(
            Long restaurantId,
            Collection<OrderStatus> statuses
    );

    Page<Order> findByRestaurantIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long restaurantId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            Pageable pageable
    );
}
