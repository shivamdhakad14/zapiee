package com.zapiee.subscriptionservice.util;

import com.zapiee.subscriptionservice.config.RazorpayConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignatureUtil {

    private final RazorpayConfig config;

    /**
     * Verify payment: HMAC_SHA256(orderId + "|" + paymentId, key_secret)
     */
    public boolean verifyPaymentSignature(String razorpayOrderId,
                                          String razorpayPaymentId,
                                          String signature) {
        try {
            String payload = razorpayOrderId + "|" + razorpayPaymentId;
            String expected = hmacSha256(payload, config.getKeySecret());
            return timingSafeEquals(expected, signature);
        } catch (Exception e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify webhook: HMAC_SHA256(raw_body, webhook_secret)
     */
    public boolean verifyWebhookSignature(String rawBody, String signature) {
        try {
            String expected = hmacSha256(rawBody, config.getWebhookSecret());
            return timingSafeEquals(expected, signature);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    /** Constant-time comparison to prevent timing attacks */
    private boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
