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


    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);

    Optional<OrderItem> findByIdAndOrderId(Long orderItemId, Long orderId);

    boolean existsByIdAndOrderId(Long orderItemId, Long orderId);

    void deleteByIdAndOrderId(Long orderItemId, Long orderId);

}
