package com.Zapiee.notification_service.service;

import com.Zapiee.notification_service.dto.NotificationRequest;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushNotification;
import org.springframework.stereotype.Service;

@Service
public class FcmNotificationService {

    public String sendPushNotification(NotificationRequest request) {

        // 1. Build web-specific notification layouts
        WebpushNotification.Builder webpushNotificationBuilder = WebpushNotification.builder()
                .setTitle(request.getTitle())
                .setBody(request.getMessage());

        if (request.getIcon() != null) {
            webpushNotificationBuilder.setIcon(request.getIcon());
        } else {
            webpushNotificationBuilder.setIcon("/icons/restaurant-logo.png"); // Default icon fallback
        }

        WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(webpushNotificationBuilder.build())
                .build();

        // 2. Start constructing the core Message payload
        Message.Builder messageBuilder = Message.builder()
                .setToken(request.getToken())
                .setWebpushConfig(webpushConfig);

        // Map custom key-value metadata to the React client if it exists
        if (request.getData() != null) {
            messageBuilder.putAllData(request.getData());
        }

        Message message = messageBuilder.build();

        try {
            // 3. Synchronously dispatch to Firebase Cloud Messaging API via SDK
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent message: " + response);
            return response;
        } catch (FirebaseMessagingException e) {
            System.err.println("Firebase execution error: " + e.getMessage());
            // getMessagingErrorCode() returns a clean enum representing the error (e.g. INVALID_ARGUMENT, UNREGISTERED)
            return "Failed to dispatch: " + e.getMessagingErrorCode();
        }
    }
}
