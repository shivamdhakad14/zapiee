package com.zapiee.authservice.controller;

import com.zapiee.authservice.dto.response.ApiResponse;
import com.zapiee.authservice.dto.response.UserInfo;
import com.zapiee.authservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserInfo>> getUserById(@RequestHeader(name = "X-USER-ID") Long userId){
        log.info("request received for get user by id : {} ", userId);
        UserInfo user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
