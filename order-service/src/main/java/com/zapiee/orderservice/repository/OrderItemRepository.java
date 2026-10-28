package com.zapiee.orderservice.repository;

import com.zapiee.orderservice.model.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Schema(
        name = "Order Item Repository",
        description = "Persistence repository for managing order items."
)
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all items belonging to a particular order.
     */
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    /**
     * Find a particular item belonging to a particular order.
     *
     * This is important for security/business validation because
     * an order item must not be removed from another order.
     */
    Optional<OrderItem> findByIdAndOrderId(
            Long orderItemId,
            Long orderId
    );

    /**
     * Check whether an item belongs to an order.
     */
    boolean existsByIdAndOrderId(
            Long orderItemId,
            Long orderId
    );

    /**
     * Find an existing menu item inside an order.
     *
     * Useful when the same menu item is added again.
     */
    Optional<OrderItem> findByOrderIdAndId(
            Long orderId,
            Long itemId
    );

    /**
     * Check whether a menu item already exists in an order.
     */
    boolean existsByOrderIdAndId(
            Long orderId,
            Long itemId
    );

    /**
     * Delete a specific item from a specific order.
     */
    void deleteByIdAndOrderId(
            Long orderItemId,
            Long orderId
    );

    /**
     * Top-selling menu items for a restaurant.
     *
     * Returns:
     * [0] itemId
     * [1] itemName
     * [2] totalQuantitySold
     */
    @Query("""
            SELECT
                oi.id,
                oi.name,
                SUM(oi.quantity)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.restaurantId = :restaurantId
            GROUP BY oi.id, oi.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingItems(
            @Param("restaurantId") Long restaurantId
    );

    /**
     * Top-selling items with a configurable maximum result count
     * should preferably be handled using Pageable.
     */
    @Query("""
            SELECT
                oi.id,
                oi.name,
                SUM(oi.quantity)
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.restaurantId = :restaurantId
            GROUP BY oi.id, oi.name
            ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> findTopSellingItems(
            @Param("restaurantId") Long restaurantId,
            org.springframework.data.domain.Pageable pageable
    );
}
