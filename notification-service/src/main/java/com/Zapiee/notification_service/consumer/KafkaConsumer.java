package com.Zapiee.notification_service.consumer;

import com.Zapiee.notification_service.event.NotificationEvent;
import com.Zapiee.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.notification-topic}")
    public void consume(String message) throws Exception {
        NotificationEvent event =
                objectMapper.readValue(message, NotificationEvent.class);

        log.info("Received notification event: {}", event.getEventId());

        notificationService.process(event);
    }

}
