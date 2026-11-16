package com.zapiee.paymentservice.repository;

import com.zapiee.paymentservice.enums.OrderPaymentStatus;
import com.zapiee.paymentservice.model.OrderPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, String> {

    Optional<OrderPayment> findByOrderIdAndRestaurantId(Long orderId, Long restaurantId);

    Page<OrderPayment> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);
    Page<OrderPayment> findByRestaurantIdAndOrderPaymentStatusOrderByCreatedAtDesc(
            Long restaurantId, OrderPaymentStatus status, Pageable pageable);

    Optional<OrderPayment> findByOrderPaymentStatus(OrderPaymentStatus orderPaymentStatus);
}

