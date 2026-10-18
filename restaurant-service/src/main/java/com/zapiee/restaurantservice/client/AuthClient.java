package com.zapiee.restaurantservice.client;

import com.zapiee.restaurantservice.dto.Response.AuthUserInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service")
public interface AuthClient {

    @GetMapping("/api/auth/user")
    AuthUserInfo getUserById(@RequestParam Long userId);

}