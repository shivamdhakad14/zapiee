package com.Zapiee.notification_service.repository;

import com.Zapiee.notification_service.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification , Long> {

    List<Notification> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Notification> findByRestaurantIdOrderByCreatedAtDesc(
            Long restaurantId);

    List<Notification> findByRestaurantIdAndReadFalseOrderByCreatedAtDesc(
            Long restaurantId);

    boolean existsByEventId(String eventId);
}
