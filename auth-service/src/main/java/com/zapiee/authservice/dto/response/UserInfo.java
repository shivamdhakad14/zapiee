package com.zapiee.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.zapiee.authservice.enums.AccountStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfo {
    private Long id;
    private String email;
    private String name;
    private Set<String> roles;
    private String phoneNumber;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private AccountStatus accountStatus;
}
