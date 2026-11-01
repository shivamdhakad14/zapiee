package com.Zapiee.notification_service.controller;


import com.Zapiee.notification_service.dto.NotificationRequest;
import com.Zapiee.notification_service.service.FcmNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // Secure this to your specific API Gateway / domain in staging
public class NotificationController {

    private final FcmNotificationService fcmService;

    public NotificationController(FcmNotificationService fcmService) {
        this.fcmService = fcmService;
    }

    @PostMapping("/send")
    public ResponseEntity<String> triggerNotification(@RequestBody NotificationRequest request) {
        String response = fcmService.sendPushNotification(request);
        if (response.startsWith("Failed")) {
            return ResponseEntity.internalServerError().body(response);
        }
        return ResponseEntity.ok("Notification pushed successfully reference string: " + response);
    }
}
