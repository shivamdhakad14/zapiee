package com.zapiee.authservice.dto.response;

import com.zapiee.authservice.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthTokens {
    private User user;
    private String accessToken;
    private String refreshToken;
}