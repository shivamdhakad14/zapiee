//package com.Zapiee.staff_service.service;
//
//import com.Zapiee.staff_service.event.NotificationEvent;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class KafkaProducerService {
//
//    private final KafkaTemplate<String , NotificationEvent> kafkaTemplate;
//
//
//    private static final String TOPIC = "notification-topic";
//
//    public void sendNotification(NotificationEvent event) {
//        log.info("Sending notification event {}", event);
//        kafkaTemplate.send(TOPIC , event);
//    }
//}
