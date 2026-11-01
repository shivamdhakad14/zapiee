package com.zapiee.restaurantservice.client;

import com.zapiee.restaurantservice.dto.ApiResponse;
import com.zapiee.restaurantservice.dto.Response.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/v1/users")
    ApiResponse<UserInfo> getUserById(@RequestHeader(name = "X-USER-ID") Long userId);

    @PutMapping("/assing-role")
    void assignRoleOwnerToUser(@RequestHeader(name = "X-USER-ID") Long userId);

}