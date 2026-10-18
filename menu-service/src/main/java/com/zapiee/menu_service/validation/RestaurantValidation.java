package com.zapiee.menu_service.validation;

import com.zapiee.menu_service.client.RestaurantClient;
import com.zapiee.menu_service.dto.response.RestaurantStatusInfo;
import com.zapiee.menu_service.exception.BusinessRuleViolationException;
import com.zapiee.menu_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RestaurantValidation {

    private final RestaurantClient restaurantClient;

    public void validateRestaurantExist(Long restaurantId){
        RestaurantStatusInfo statusInfo = restaurantClient.getStatus(restaurantId);
        if (statusInfo == null) {
            throw new ResourceNotFoundException("Restaurant not found");
        }

        if (statusInfo.getStatus() != RestaurantStatusInfo.Status.ACTIVE) {
            throw new BusinessRuleViolationException(
                    "Restaurant is currently " + statusInfo.getStatus() + " and cannot be accessed", "ACCESS_DENIED"
            );
        }
    }
}
