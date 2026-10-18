package com.zapiee.menu_service.client;


import com.zapiee.menu_service.dto.response.PlanFeaturesResponse;
import com.zapiee.menu_service.exception.BillingServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionClientWrapper {

    private SubscriptionServiceClient billingServiceClient;

    public SubscriptionClientWrapper(SubscriptionServiceClient billingServiceClient) {
        this.billingServiceClient = billingServiceClient;
    }

    public SubscriptionClientWrapper(){}

    @Retry(name = "billingService")
    @CircuitBreaker(name = "billingService", fallbackMethod = "fallbackFeatures")

    public PlanFeaturesResponse getFeatures(Long restaurantId) {
        return billingServiceClient.getFeatures(restaurantId);
    }

    private PlanFeaturesResponse fallbackFeatures(Long restaurantId, Throwable t) {
        throw new BillingServiceUnavailableException(
                "Unable to verify subscription limits right now. Please try again shortly."
        );
    }
}
