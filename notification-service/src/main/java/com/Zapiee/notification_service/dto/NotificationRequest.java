package com.Zapiee.notification_service.dto;

import lombok.Data;

import java.util.Map;

@Data
public class NotificationRequest {

    private String token; //the unique FCM browser token belonging to the customer/kitchen
    private String title; //e.g., "order confirmed"
    private String message; //e.g., "Chef is preparing your order"
    private String icon;
    private Map<String,String> data; // Custom data payload (e.g., {"orderID" : "1042" , "table": "5"})
}
