
package com.Zapiee.notification_service.service;

import com.Zapiee.notification_service.event.NotificationEvent;
import com.Zapiee.notification_service.entity.Notification;
import com.Zapiee.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

//    @Transactional
//    public void process(NotificationEvent event) {
//        if (event.getEventId() != null
//                && repository.existsByEventId(event.getEventId())) {
//            return;
//        }
//
//        Notification notification = Notification.builder()
//                .eventId(event.getEventId())
//                .type(event.getType())
//                .channel(event.getChannel())
//                .title(event.getTitle())
//                .message(event.getMessage())
//                .orderId(event.getOrderId())
//                .tableId(event.getTableId())
//                .restaurantId(event.getRestaurantId()
//                        .
//                .build();
//
//        Notification saved = repository.save(notification);
//
//        if (event.channel()
//                == com.Zapiee.notification_service.enums.NotificationChannel.PUSH) {
//
//            if (event.orderId() != null) {
//                messagingTemplate.convertAndSend(
//                        "/topic/order/" + event.orderId(), saved);
//            }
//
//            if (event.restaurantId() != null) {
//                messagingTemplate.convertAndSend(
//                        "/topic/restaurant/" + event.restaurantId(), saved);
//            }
//        }
//    }


}
