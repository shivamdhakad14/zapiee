package com.zapiee.paymentservice.enums;

public enum OrderPaymentStatus {
    PENDING,      // Order created, no Razorpay order yet
    INITIATED,    // Razorpay order created, deep link / QR sent to customer
    PROCESSING,   // Redirect received from UPI app — awaiting webhook confirmation
    PAID,         // Confirmed by webhook payment.captured (source of truth)
    FAILED,       // Payment failed or timed out
    REFUNDED,     // Fully refunded
    PARTIALLY_REFUNDED  // Partial refund issued
}
