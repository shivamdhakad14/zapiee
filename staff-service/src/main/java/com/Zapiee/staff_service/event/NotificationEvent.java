package com.Zapiee.staff_service.event;


import com.Zapiee.staff_service.enums.NotificationChannel;
import com.Zapiee.staff_service.enums.NotificationStatus;
import com.Zapiee.staff_service.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long userId;
    private String referenceId = UUID.randomUUID().toString();
    private String email;         // staff email
    private String title;        // notification title
    private String message;       // notification body
    private NotificationType type;  // STAFF_CREATED / STAFF_DELETED
    private NotificationStatus status;
    private NotificationChannel channel;// EMAIL
    private Map<String , Object> payload;
}