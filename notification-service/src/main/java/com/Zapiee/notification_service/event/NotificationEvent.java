package com.Zapiee.notification_service.event;

import com.Zapiee.notification_service.enums.NotificationChannel;
import com.Zapiee.notification_service.enums.NotificationStatus;
import com.Zapiee.notification_service.enums.NotificationType;
import lombok.*;

import java.util.Map;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    String eventId ;
    NotificationType type;
    NotificationChannel channel;
    NotificationStatus status;
    String title;
    String message;
    Long orderId;
    Long tableId;
    Long restaurantId;
    private Map<String,Object> payload;

}
