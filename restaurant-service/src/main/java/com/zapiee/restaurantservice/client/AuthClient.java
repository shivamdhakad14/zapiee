package com.zapiee.restaurantservice.client;

import com.zapiee.restaurantservice.dto.Response.UserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/v1/users")
    UserInfo getUserById(@RequestHeader Long userId);

}