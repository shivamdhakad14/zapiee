
package com.zapiee.orderservice.repository;

import com.zapiee.orderservice.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    Optional<OrderItem> findByIdAndOrder_Id(
            Long orderItemId,
            Long orderId
    );

    boolean existsByIdAndOrder_Id(
            Long orderItemId,
            Long orderId
    );

    void deleteByIdAndOrder_Id(
            Long orderItemId,
            Long orderId
    );
}
